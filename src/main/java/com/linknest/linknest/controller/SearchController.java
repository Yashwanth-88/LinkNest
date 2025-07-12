package com.linknest.linknest.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.linknest.linknest.entity.User;
import com.linknest.linknest.entity.Post;
import com.linknest.linknest.entity.Hashtag;
import com.linknest.linknest.repository.UserRepository;
import com.linknest.linknest.repository.PostRepository;
import com.linknest.linknest.repository.HashtagRepository;

import java.util.List;
import java.util.stream.Collectors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/search")
public class SearchController {
    @Autowired
    private PostRepository postRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private HashtagRepository hashtagRepository;

    // Hashtag autocomplete
    @GetMapping("/hashtags")
    public List<String> autocompleteHashtags(@RequestParam String query) {
        // Use repository method for prefix search, add typo-tolerance
        var hashtags = hashtagRepository.findTop10ByNameStartingWithIgnoreCase("#" + query);
        // Add typo-tolerance: get all hashtags, rank by Levenshtein distance, then by popularity
        var allHashtags = hashtagRepository.findAll();
        org.apache.commons.text.similarity.LevenshteinDistance ld = new org.apache.commons.text.similarity.LevenshteinDistance();
        var sorted = allHashtags.stream()
            .sorted((a, b) -> {
                int distA = ld.apply(query.toLowerCase(), a.getName().replace("#", "").toLowerCase());
                int distB = ld.apply(query.toLowerCase(), b.getName().replace("#", "").toLowerCase());
                if (distA != distB) return Integer.compare(distA, distB);
                return Integer.compare(b.getPosts().size(), a.getPosts().size());
            })
            .limit(10)
            .map(Hashtag::getName)
            .toList();
        return sorted;
    }

    // Username autocomplete
    @GetMapping("/usernames")
    public List<String> autocompleteUsernames(@RequestParam String query) {
        // Use repository method for prefix search
        return userRepository.findTop10ByUsernameStartingWithIgnoreCase(query).stream()
            .map(User::getUsername)
            .sorted()
            .toList();
    }

    @GetMapping("/trending-hashtags")
    public List<String> getTrendingHashtags() {
        return hashtagRepository.findAll().stream()
            .sorted((a, b) -> Integer.compare(b.getPosts().size(), a.getPosts().size()))
            .limit(10)
            .map(Hashtag::getName)
            .collect(java.util.stream.Collectors.toList());
    }

    @GetMapping("/posts")
    public ResponseEntity<?> searchPosts(@RequestParam String query, @RequestParam(defaultValue = "keyword") String type,
                                         @RequestParam(defaultValue = "0") int page,
                                         @RequestParam(defaultValue = "10") int size) {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);
        org.springframework.data.domain.Page<Post> results;
        switch (type.toLowerCase()) {
            case "hashtag" -> results = postRepository.findByHashtagNameIgnoreCase(query.startsWith("#") ? query : ("#" + query), pageable);
            case "username" -> results = postRepository.findByUsernameIgnoreCase(query, pageable);
            default -> results = postRepository.fuzzySearchByKeywordRanked(query, pageable);
        }
        return ResponseEntity.ok(results.getContent());
    }

    @PostMapping("/migrate-hashtags")
    public String migrateHashtags() {
        int updated = 0;
        for (Post post : postRepository.findAll()) {
            java.util.Set<Hashtag> hashtags = new java.util.HashSet<>();
            String content = post.getContent();
            if (content != null) {
                java.util.regex.Pattern hashtagPattern = java.util.regex.Pattern.compile("#\\w+");
                java.util.regex.Matcher matcher = hashtagPattern.matcher(content);
                while (matcher.find()) {
                    String tag = matcher.group().toLowerCase();
                    Hashtag hashtag = hashtagRepository.findByNameIgnoreCase(tag)
                            .orElseGet(() -> hashtagRepository.save(new Hashtag(tag)));
                    hashtags.add(hashtag);
                }
            }
            if (!hashtags.equals(post.getHashtags())) {
                post.setHashtags(hashtags);
                postRepository.save(post);
                updated++;
            }
        }
        return "Migrated hashtags for " + updated + " posts.";
    }
} 