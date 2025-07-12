package com.linknest.linknest.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;

import com.linknest.linknest.repository.PostRepository;
import com.linknest.linknest.repository.HashtagRepository;
import com.linknest.linknest.repository.UserRepository;
import com.linknest.linknest.entity.Post;
import com.linknest.linknest.entity.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/explore")
public class ExploreController {
    @Autowired
    private PostRepository postRepository;
    @Autowired
    private HashtagRepository hashtagRepository;
    @Autowired
    private UserRepository userRepository;

    @GetMapping("/trending-posts")
    public List<Post> getTrendingPosts(@RequestParam(defaultValue = "10") int size) {
        LocalDateTime since = LocalDateTime.now().minusDays(7);
        return postRepository.findTrendingPosts(since, PageRequest.of(0, size));
    }

    @GetMapping("/trending-hashtags")
    public List<Map<String, Object>> getTrendingHashtags(@RequestParam(defaultValue = "10") int size) {
        LocalDateTime since = LocalDateTime.now().minusDays(7);
        List<Object[]> results = hashtagRepository.findTrendingHashtags(since, PageRequest.of(0, size));
        return results.stream().map(obj -> Map.of("name", obj[0], "count", obj[1])).collect(Collectors.toList());
    }

    @GetMapping("/trending-users")
    public List<User> getTrendingUsers(@RequestParam(defaultValue = "10") int size) {
        return userRepository.findTrendingUsers(PageRequest.of(0, size));
    }
} 