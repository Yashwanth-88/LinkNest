package com.linknest.linknest.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.linknest.linknest.entity.Notification;
import com.linknest.linknest.entity.User;
import com.linknest.linknest.repository.NotificationRepository;
import com.linknest.linknest.repository.UserRepository;

import java.util.List;
import java.util.Set;
import java.util.HashSet;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    @Autowired
    private NotificationRepository notificationRepository;
    @Autowired
    private UserRepository userRepository;

    // In-memory muted types for demonstration (should be persisted in User entity in production)
    private static final java.util.Map<Long, java.util.Set<String>> userMutedTypes = new java.util.concurrent.ConcurrentHashMap<>();

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found"));
    }

    // List notifications with pagination and optional read/unread filter
    @GetMapping
    public Page<Notification> getNotifications(@RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "10") int size,
                                               @RequestParam(required = false) Boolean read) {
        User user = getCurrentUser();
        Pageable pageable = PageRequest.of(page, size);
        if (read == null) {
            return notificationRepository.findByUserOrderByCreatedAtDesc(user, pageable);
        } else {
            return notificationRepository.findByUserAndIsRead(user, read, pageable);
        }
    }

    // Mark notifications as read (bulk)
    @PostMapping("/mark-read")
    public ResponseEntity<?> markAsRead(@RequestBody Set<Long> notificationIds) {
        User user = getCurrentUser();
        List<Notification> notifications = notificationRepository.findByIdInAndUser(notificationIds.stream().toList(), user);
        notifications.forEach(n -> n.setRead(true));
        notificationRepository.saveAll(notifications);
        return ResponseEntity.ok("Marked as read");
    }

    // Mark notifications as unread (bulk)
    @PostMapping("/mark-unread")
    public ResponseEntity<?> markAsUnread(@RequestBody Set<Long> notificationIds) {
        User user = getCurrentUser();
        List<Notification> notifications = notificationRepository.findByIdInAndUser(notificationIds.stream().toList(), user);
        notifications.forEach(n -> n.setRead(false));
        notificationRepository.saveAll(notifications);
        return ResponseEntity.ok("Marked as unread");
    }

    // Mark all as read
    @PostMapping("/mark-all-read")
    public ResponseEntity<?> markAllAsRead() {
        User user = getCurrentUser();
        List<Notification> notifications = notificationRepository.findByUserOrderByCreatedAtDesc(user);
        notifications.forEach(n -> n.setRead(true));
        notificationRepository.saveAll(notifications);
        return ResponseEntity.ok("All notifications marked as read");
    }

    // Clear all notifications
    @DeleteMapping("/clear")
    public ResponseEntity<?> clearAllNotifications() {
        User user = getCurrentUser();
        List<Notification> notifications = notificationRepository.findByUserOrderByCreatedAtDesc(user);
        notificationRepository.deleteAll(notifications);
        return ResponseEntity.ok("All notifications cleared");
    }

    // Delete a notification
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteNotification(@PathVariable Long id) {
        User user = getCurrentUser();
        return notificationRepository.findById(id)
                .filter(n -> n.getUser().getId().equals(user.getId()))
                .map(n -> {
                    notificationRepository.delete(n);
                    return ResponseEntity.ok("Deleted");
                })
                .orElse(ResponseEntity.status(404).body("Notification not found"));
    }

    // Get unread notification count
    @GetMapping("/unread-count")
    public ResponseEntity<Integer> getUnreadCount() {
        User user = getCurrentUser();
        int count = (int) notificationRepository.findByUserOrderByCreatedAtDesc(user).stream().filter(n -> !n.isRead()).count();
        return ResponseEntity.ok(count);
    }

    // Stream real-time unread notification count via SSE
    @GetMapping("/unread-count/stream")
    public SseEmitter streamUnreadCount() {
        SseEmitter emitter = new SseEmitter(0L); // No timeout
        User user = getCurrentUser();
        new Thread(() -> {
            int lastCount = -1;
            try {
                while (true) {
                    int count = (int) notificationRepository.findByUserOrderByCreatedAtDesc(user).stream().filter(n -> !n.isRead()).count();
                    if (count != lastCount) {
                        emitter.send(count);
                        lastCount = count;
                    }
                    Thread.sleep(3000); // Poll every 3 seconds
                }
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        }).start();
        return emitter;
    }

    // Get notifications by type
    @GetMapping("/type")
    public Page<Notification> getNotificationsByType(@RequestParam String type,
                                                    @RequestParam(defaultValue = "0") int page,
                                                    @RequestParam(defaultValue = "10") int size) {
        User user = getCurrentUser();
        Pageable pageable = PageRequest.of(page, size);
        return notificationRepository.findByUserAndTypeOrderByCreatedAtDesc(user, type, pageable);
    }

    // Get unread notification count by type
    @GetMapping("/unread-count/type")
    public ResponseEntity<Integer> getUnreadCountByType(@RequestParam String type) {
        User user = getCurrentUser();
        int count = (int) notificationRepository.findByUserAndTypeOrderByCreatedAtDesc(user, type, org.springframework.data.domain.PageRequest.of(0, 1000))
            .stream().filter(n -> !n.isRead()).count();
        return ResponseEntity.ok(count);
    }

    // List muted notification types
    @GetMapping("/muted-types")
    public ResponseEntity<java.util.Set<String>> getMutedTypes() {
        User user = getCurrentUser();
        return ResponseEntity.ok(userMutedTypes.getOrDefault(user.getId(), new HashSet<>()));
    }

    // Mute a notification type
    @PostMapping("/mute-type")
    public ResponseEntity<?> muteType(@RequestParam String type) {
        User user = getCurrentUser();
        userMutedTypes.computeIfAbsent(user.getId(), k -> new HashSet<>()).add(type);
        return ResponseEntity.ok("Muted type: " + type);
    }

    // Unmute a notification type
    @DeleteMapping("/mute-type")
    public ResponseEntity<?> unmuteType(@RequestParam String type) {
        User user = getCurrentUser();
        userMutedTypes.computeIfAbsent(user.getId(), k -> new HashSet<>()).remove(type);
        return ResponseEntity.ok("Unmuted type: " + type);
    }

    // Trigger notification sound (frontend should poll this endpoint or use SSE for real-time)
    @PostMapping("/sound-trigger")
    public ResponseEntity<?> triggerSound() {
        // In a real app, this would push a flag to the frontend via WebSocket or SSE
        // Here, just return a flag for demonstration
        return ResponseEntity.ok(java.util.Map.of("playSound", true));
    }

    // Export all notifications for the current user as JSON
    @GetMapping("/export")
    public ResponseEntity<java.util.List<Notification>> exportNotifications() {
        User user = getCurrentUser();
        var notifications = notificationRepository.findByUserOrderByCreatedAtDesc(user);
        return ResponseEntity.ok(notifications);
    }
} 