package com.linknest.linknest;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/posts")
public class PostController {

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private UserRepository userRepository;

    @PostMapping
    public ResponseEntity<?> createPost(@Valid @RequestBody Post post, @RequestParam Long userId) {
        Optional<User> user = userRepository.findById(userId);
        if (user.isEmpty()) {
            return ResponseEntity.badRequest().body("User with ID " + userId + " not found");
        }
        post.setUser(user.get());
        Post savedPost = postRepository.save(post);
        return ResponseEntity.ok(savedPost);
    }

    // Other methods remain unchanged
}