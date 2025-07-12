package com.linknest.linknest.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import com.linknest.linknest.entity.User;
import com.linknest.linknest.entity.Hashtag;
import com.linknest.linknest.entity.MutedUser;
import com.linknest.linknest.entity.MutedHashtag;
import com.linknest.linknest.repository.UserRepository;
import com.linknest.linknest.repository.HashtagRepository;
import com.linknest.linknest.repository.MutedUserRepository;
import com.linknest.linknest.repository.MutedHashtagRepository;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/personalization")
public class FeedPersonalizationController {
    @Autowired
    private MutedUserRepository mutedUserRepository;
    @Autowired
    private MutedHashtagRepository mutedHashtagRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private HashtagRepository hashtagRepository;

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found"));
    }

    // Mute a user
    @PostMapping("/mute/user/{userId}")
    public ResponseEntity<?> muteUser(@PathVariable Long userId) {
        User user = getCurrentUser();
        if (user.getId().equals(userId)) {
            return ResponseEntity.badRequest().body("Cannot mute yourself");
        }
        User muted = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        if (mutedUserRepository.existsByUserAndMutedUser(user, muted)) {
            return ResponseEntity.badRequest().body("User already muted");
        }
        mutedUserRepository.save(new MutedUser(user, muted));
        return ResponseEntity.ok("User muted");
    }

    // Unmute a user
    @DeleteMapping("/mute/user/{userId}")
    public ResponseEntity<?> unmuteUser(@PathVariable Long userId) {
        User user = getCurrentUser();
        User muted = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        List<MutedUser> mutes = mutedUserRepository.findByUser(user);
        for (MutedUser mu : mutes) {
            if (mu.getMutedUser().getId().equals(muted.getId())) {
                mutedUserRepository.delete(mu);
                return ResponseEntity.ok("User unmuted");
            }
        }
        return ResponseEntity.badRequest().body("User was not muted");
    }

    // List muted users
    @GetMapping("/mute/users")
    public List<Long> getMutedUsers() {
        User user = getCurrentUser();
        return mutedUserRepository.findByUser(user).stream()
                .map(mu -> mu.getMutedUser().getId())
                .collect(Collectors.toList());
    }

    // Mute a hashtag
    @PostMapping("/mute/hashtag/{hashtagId}")
    public ResponseEntity<?> muteHashtag(@PathVariable Long hashtagId) {
        User user = getCurrentUser();
        Hashtag hashtag = hashtagRepository.findById(hashtagId).orElseThrow(() -> new RuntimeException("Hashtag not found"));
        if (mutedHashtagRepository.existsByUserAndHashtag(user, hashtag)) {
            return ResponseEntity.badRequest().body("Hashtag already muted");
        }
        mutedHashtagRepository.save(new MutedHashtag(user, hashtag));
        return ResponseEntity.ok("Hashtag muted");
    }

    // Unmute a hashtag
    @DeleteMapping("/mute/hashtag/{hashtagId}")
    public ResponseEntity<?> unmuteHashtag(@PathVariable Long hashtagId) {
        User user = getCurrentUser();
        Hashtag hashtag = hashtagRepository.findById(hashtagId).orElseThrow(() -> new RuntimeException("Hashtag not found"));
        List<MutedHashtag> mutes = mutedHashtagRepository.findByUser(user);
        for (MutedHashtag mh : mutes) {
            if (mh.getHashtag().getId().equals(hashtag.getId())) {
                mutedHashtagRepository.delete(mh);
                return ResponseEntity.ok("Hashtag unmuted");
            }
        }
        return ResponseEntity.badRequest().body("Hashtag was not muted");
    }

    // List muted hashtags
    @GetMapping("/mute/hashtags")
    public List<Long> getMutedHashtags() {
        User user = getCurrentUser();
        return mutedHashtagRepository.findByUser(user).stream()
                .map(mh -> mh.getHashtag().getId())
                .collect(Collectors.toList());
    }
} 