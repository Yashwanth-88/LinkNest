package com.linknest.linknest.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import java.util.stream.Collectors;

import com.linknest.linknest.entity.User;
import com.linknest.linknest.entity.Message;
import com.linknest.linknest.repository.UserRepository;
import com.linknest.linknest.repository.MessageRepository;
import com.linknest.linknest.repository.UserBlockRepository;
import com.linknest.linknest.service.EmailService;
import com.linknest.linknest.service.NotificationWebSocketService;
import com.linknest.linknest.dto.MessageRequest;
import com.linknest.linknest.dto.MessageResponse;
import com.linknest.linknest.entity.Notification;
import com.linknest.linknest.repository.NotificationRepository;


@RestController
@RequestMapping("/api/messages")
public class MessageController {
    @Autowired
    private MessageRepository messageRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserBlockRepository userBlockRepository;
    @Autowired
    private EmailService emailService;
    @Autowired
    private NotificationWebSocketService notificationWebSocketService;
    @Autowired
    private NotificationRepository notificationRepository;

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found"));
    }

    @PostMapping
    public ResponseEntity<?> sendMessage(@RequestBody MessageRequest request) {
        User sender = getCurrentUser();
        User recipient = userRepository.findById(request.getRecipientId()).orElseThrow(() -> new RuntimeException("Recipient not found"));
        // Block check
        if (userBlockRepository.existsByBlockerAndBlocked(sender, recipient) ||
            userBlockRepository.existsByBlockerAndBlocked(recipient, sender)) {
            return ResponseEntity.status(403).body("Cannot send message: one of the users has blocked the other.");
        }
        Message message = new Message(sender, recipient, request.getContent(), request.getMediaUrl());
        messageRepository.save(message);
        // Create notification for the recipient
        String notifMessage = sender.getUsername() + " sent you a direct message.";
        Long targetId = message.getId();
        String targetType = "message";
        String data = null;
        Notification notification = new Notification(
            recipient, "message", null, null, notifMessage, targetId, targetType, data);
        notificationRepository.save(notification);
        notificationWebSocketService.sendNotification(recipient.getId(), notifMessage);
        // Send email notification if recipient has an email set
        if (recipient.getEmail() != null && !recipient.getEmail().isBlank()) {
            String subject = "New direct message on LinkNest!";
            String text = "Hi " + recipient.getUsername() + ",\n\n" +
                    sender.getUsername() + " sent you a new message: \"" + request.getContent() + "\".";
            emailService.sendEmail(recipient.getEmail(), subject, text);
        }
        return ResponseEntity.ok("Message sent");
    }

    @GetMapping("/conversation/{userId}")
    public ResponseEntity<?> getConversation(@PathVariable Long userId,
                                             @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "20") int size) {
        User currentUser = getCurrentUser();
        // Block check
        User otherUser = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        if (userBlockRepository.existsByBlockerAndBlocked(currentUser, otherUser) ||
            userBlockRepository.existsByBlockerAndBlocked(otherUser, currentUser)) {
            return ResponseEntity.status(403).body("Cannot view conversation: one of the users has blocked the other.");
        }
        List<Message> messages1 = messageRepository.findBySenderAndRecipientOrderByCreatedAtDesc(currentUser, otherUser);
        List<Message> messages2 = messageRepository.findBySenderAndRecipientOrderByCreatedAtDesc(otherUser, currentUser);
        List<Message> allMessages = new ArrayList<>();
        allMessages.addAll(messages1);
        allMessages.addAll(messages2);
        allMessages.sort((m1, m2) -> m2.getCreatedAt().compareTo(m1.getCreatedAt()));
        
        // Apply pagination manually
        int start = page * size;
        int end = Math.min(start + size, allMessages.size());
        List<Message> paginatedMessages = allMessages.subList(start, end);
        List<MessageResponse> response = paginatedMessages.stream().map(m ->
                new MessageResponse(m.getId(), m.getSender().getId(), m.getRecipient().getId(), m.getContent(), m.getCreatedAt(), m.isRead())
        ).collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/conversations")
    public ResponseEntity<?> getAllConversations() {
        User currentUser = getCurrentUser();
        List<Message> messages = messageRepository.findByRecipientOrderByCreatedAtDesc(currentUser);
        Set<Long> userIds = new HashSet<>();
        for (Message m : messages) {
            if (!m.getSender().getId().equals(currentUser.getId())) userIds.add(m.getSender().getId());
            if (!m.getRecipient().getId().equals(currentUser.getId())) userIds.add(m.getRecipient().getId());
        }
        return ResponseEntity.ok(userIds);
    }

    @PostMapping("/mark-read/{userId}")
    public ResponseEntity<?> markMessagesAsRead(@PathVariable Long userId) {
        User currentUser = getCurrentUser();
        User otherUser = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        List<Message> messages1 = messageRepository.findBySenderAndRecipientOrderByCreatedAtDesc(otherUser, currentUser);
        List<Message> messages2 = messageRepository.findBySenderAndRecipientOrderByCreatedAtDesc(currentUser, otherUser);
        List<Message> messages = new ArrayList<>();
        messages.addAll(messages1);
        messages.addAll(messages2);
        int updated = 0;
        for (Message m : messages) {
            if (m.getRecipient().getId().equals(currentUser.getId()) && !m.isRead()) {
                m.setRead(true);
                messageRepository.save(m);
                updated++;
            }
        }
        return ResponseEntity.ok("Marked " + updated + " messages as read.");
    }
} 