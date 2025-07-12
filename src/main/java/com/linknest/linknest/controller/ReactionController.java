package com.linknest.linknest.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import com.linknest.linknest.entity.User;
import com.linknest.linknest.entity.Post;
import com.linknest.linknest.entity.Comment;
import com.linknest.linknest.entity.Reaction;
import com.linknest.linknest.repository.UserRepository;
import com.linknest.linknest.repository.PostRepository;
import com.linknest.linknest.repository.CommentRepository;
import com.linknest.linknest.repository.ReactionRepository;
import com.linknest.linknest.repository.UserBlockRepository;
import com.linknest.linknest.dto.ReactionRequest;
import com.linknest.linknest.dto.ReactionResponse;

import java.util.List;
import java.util.stream.Collectors;
import java.util.Optional;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/reactions")
public class ReactionController {
    @Autowired
    private ReactionRepository reactionRepository;
    @Autowired
    private PostRepository postRepository;
    @Autowired
    private CommentRepository commentRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserBlockRepository userBlockRepository;

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found"));
    }

    @PostMapping
    public ResponseEntity<?> addOrUpdateReaction(@RequestBody ReactionRequest request) {
        User user = getCurrentUser();
        Reaction reaction = null;
        if (request.getPostId() != null) {
            Post post = postRepository.findById(request.getPostId()).orElseThrow(() -> new RuntimeException("Post not found"));
            // Block check
            if (userBlockRepository.existsByBlockerIdAndBlockedId(user.getId(), post.getUser().getId()) ||
                userBlockRepository.existsByBlockerIdAndBlockedId(post.getUser().getId(), user.getId())) {
                return ResponseEntity.status(403).body("Cannot react: one of the users has blocked the other.");
            }
            Optional<Reaction> existing = reactionRepository.findFirstByUserIdAndPostId(user.getId(), post.getId());
            if (existing.isPresent()) {
                reaction = existing.get();
                reaction.setType(request.getType());
            } else {
                reaction = new Reaction(request.getType(), user, post, null);
            }
        } else if (request.getCommentId() != null) {
            Comment comment = commentRepository.findById(request.getCommentId()).orElseThrow(() -> new RuntimeException("Comment not found"));
            // Block check
            if (userBlockRepository.existsByBlockerIdAndBlockedId(user.getId(), comment.getUser().getId()) ||
                userBlockRepository.existsByBlockerIdAndBlockedId(comment.getUser().getId(), user.getId())) {
                return ResponseEntity.status(403).body("Cannot react: one of the users has blocked the other.");
            }
            Optional<Reaction> existing = reactionRepository.findFirstByUserIdAndCommentId(user.getId(), comment.getId());
            if (existing.isPresent()) {
                reaction = existing.get();
                reaction.setType(request.getType());
            } else {
                reaction = new Reaction(request.getType(), user, null, comment);
            }
        } else {
            return ResponseEntity.badRequest().body("Must provide postId or commentId");
        }
        reactionRepository.save(reaction);
        return ResponseEntity.ok("Reaction saved");
    }

    @DeleteMapping
    public ResponseEntity<?> removeReaction(@RequestParam(required = false) Long postId, @RequestParam(required = false) Long commentId) {
        User user = getCurrentUser();
        if (postId != null) {
            Optional<Reaction> existing = reactionRepository.findFirstByUserIdAndPostId(user.getId(), postId);
            existing.ifPresent(reactionRepository::delete);
        } else if (commentId != null) {
            Optional<Reaction> existing = reactionRepository.findFirstByUserIdAndCommentId(user.getId(), commentId);
            existing.ifPresent(reactionRepository::delete);
        } else {
            return ResponseEntity.badRequest().body("Must provide postId or commentId");
        }
        return ResponseEntity.ok("Reaction removed");
    }

    @GetMapping
    public ResponseEntity<ReactionResponse> getReactions(@RequestParam(required = false) Long postId, @RequestParam(required = false) Long commentId) {
        User user = getCurrentUser();
        List<Reaction> reactions;
        String userReaction = null;
        if (postId != null) {
            reactions = reactionRepository.findByPostId(postId);
            userReaction = reactionRepository.findFirstByUserIdAndPostId(user.getId(), postId).map(Reaction::getType).orElse(null);
        } else if (commentId != null) {
            reactions = reactionRepository.findByCommentId(commentId);
            userReaction = reactionRepository.findFirstByUserIdAndCommentId(user.getId(), commentId).map(Reaction::getType).orElse(null);
        } else {
            return ResponseEntity.badRequest().build();
        }
        Map<String, Integer> counts = new HashMap<>();
        for (Reaction r : reactions) {
            counts.put(r.getType(), counts.getOrDefault(r.getType(), 0) + 1);
        }
        return ResponseEntity.ok(new ReactionResponse(counts, userReaction));
    }
} 