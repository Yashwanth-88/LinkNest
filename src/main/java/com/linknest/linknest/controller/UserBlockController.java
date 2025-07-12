package com.linknest.linknest.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import com.linknest.linknest.entity.User;
import com.linknest.linknest.entity.UserBlock;
import com.linknest.linknest.repository.UserRepository;
import com.linknest.linknest.repository.UserBlockRepository;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/blocks")
public class UserBlockController {
    @Autowired
    private UserBlockRepository userBlockRepository;
    @Autowired
    private UserRepository userRepository;

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found"));
    }

    @PostMapping("/{userId}")
    public ResponseEntity<?> blockUser(@PathVariable Long userId) {
        User blocker = getCurrentUser();
        if (blocker.getId().equals(userId)) {
            return ResponseEntity.badRequest().body("You cannot block yourself.");
        }
        User blocked = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        if (userBlockRepository.existsByBlockerAndBlocked(blocker, blocked)) {
            return ResponseEntity.badRequest().body("User already blocked.");
        }
        userBlockRepository.save(new UserBlock(blocker, blocked));
        return ResponseEntity.ok("User blocked.");
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<?> unblockUser(@PathVariable Long userId) {
        User blocker = getCurrentUser();
        User blocked = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        return userBlockRepository.findByBlockerAndBlocked(blocker, blocked)
                .map(block -> {
                    userBlockRepository.delete(block);
                    return ResponseEntity.ok("User unblocked.");
                })
                .orElse(ResponseEntity.badRequest().body("User was not blocked."));
    }

    @GetMapping
    public List<Long> getBlockedUsers() {
        User blocker = getCurrentUser();
        return userBlockRepository.findByBlocker(blocker).stream()
                .map(block -> block.getBlocked().getId())
                .collect(Collectors.toList());
    }
} 