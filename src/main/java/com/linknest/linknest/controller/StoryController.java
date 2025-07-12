package com.linknest.linknest.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import com.linknest.linknest.entity.User;
import com.linknest.linknest.entity.Story;
import com.linknest.linknest.repository.UserRepository;
import com.linknest.linknest.repository.StoryRepository;
import com.linknest.linknest.repository.FollowRepository;
import com.linknest.linknest.dto.StoryRequest;
import com.linknest.linknest.dto.StoryResponse;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/stories")
public class StoryController {
    @Autowired
    private StoryRepository storyRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private FollowRepository followRepository;

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found"));
    }

    @PostMapping
    public ResponseEntity<?> createStory(@RequestBody StoryRequest request) {
        User user = getCurrentUser();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusHours(24);
        Story story = new Story(user, request.getMediaUrl(), expiresAt);
        storyRepository.save(story);
        return ResponseEntity.ok("Story created");
    }

    @GetMapping("/me")
    public List<StoryResponse> getMyStories() {
        User user = getCurrentUser();
        LocalDateTime now = LocalDateTime.now();
        return storyRepository.findByUserOrderByCreatedAtDesc(user).stream()
                .filter(s -> s.getExpiresAt().isAfter(now))
                .map(s -> new StoryResponse(s.getId(), s.getUser().getId(), s.getMediaUrl(), s.getCreatedAt(), s.getExpiresAt()))
                .collect(Collectors.toList());
    }

    @GetMapping("/feed")
    public List<StoryResponse> getFeedStories() {
        User user = getCurrentUser();
        LocalDateTime now = LocalDateTime.now();
        List<Long> userIds = followRepository.findByFollower(user).stream()
                .map(f -> f.getFollowing().getId())
                .collect(Collectors.toList());
        userIds.add(user.getId());
        List<Story> allStories = new ArrayList<>();
        for (Long userId : userIds) {
            User storyUser = userRepository.findById(userId).orElse(null);
            if (storyUser != null) {
                allStories.addAll(storyRepository.findByUserOrderByCreatedAtDesc(storyUser));
            }
        }
        return allStories.stream()
                .filter(s -> s.getExpiresAt().isAfter(now))
                .map(s -> new StoryResponse(s.getId(), s.getUser().getId(), s.getMediaUrl(), s.getCreatedAt(), s.getExpiresAt()))
                .collect(Collectors.toList());
    }
} 