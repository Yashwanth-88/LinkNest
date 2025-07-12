package com.linknest.linknest.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import com.linknest.linknest.entity.User;
import com.linknest.linknest.entity.Badge;
import com.linknest.linknest.entity.UserBadge;
import com.linknest.linknest.entity.Notification;
import com.linknest.linknest.repository.BadgeRepository;
import com.linknest.linknest.repository.UserBadgeRepository;
import com.linknest.linknest.repository.UserRepository;
import com.linknest.linknest.repository.NotificationRepository;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/badges")
public class BadgeController {
    @Autowired
    private BadgeRepository badgeRepository;
    @Autowired
    private UserBadgeRepository userBadgeRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private NotificationRepository notificationRepository;

    @GetMapping
    public List<Badge> getAllBadges() {
        return badgeRepository.findAll();
    }

    @GetMapping("/user/{userId}")
    public List<Badge> getUserBadges(@PathVariable Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        return userBadgeRepository.findByUser(user).stream()
                .map(UserBadge::getBadge)
                .collect(Collectors.toList());
    }

    @PostMapping("/award")
    public ResponseEntity<?> awardBadge(@RequestParam Long userId, @RequestParam Long badgeId) {
        // Only admin can award badges
        boolean isAdmin = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!isAdmin) {
            return ResponseEntity.status(403).body("Only admin can award badges");
        }
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        Badge badge = badgeRepository.findById(badgeId).orElseThrow(() -> new RuntimeException("Badge not found"));
        if (userBadgeRepository.existsByUserAndBadge(user, badge)) {
            return ResponseEntity.badRequest().body("User already has this badge");
        }
        userBadgeRepository.save(new UserBadge(user, badge));
        // Create notification for the user
        String message = "You have been awarded a new badge: '" + badge.getName() + "'!";
        Long targetId = badge.getId();
        String targetType = "badge";
        String data = null;
        Notification notification = new Notification(
            user, "badge", null, null, message, targetId, targetType, data);
        notificationRepository.save(notification);
        // Optionally, send real-time notification (if using WebSocket)
        // notificationWebSocketService.sendNotification(user.getId(), message);
        return ResponseEntity.ok("Badge awarded");
    }
} 