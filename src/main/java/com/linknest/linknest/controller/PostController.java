package com.linknest.linknest.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.util.StringUtils;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.IOException;

import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.security.core.GrantedAuthority;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

import com.linknest.linknest.service.NotificationWebSocketService;
import com.linknest.linknest.service.EmailService;
import com.linknest.linknest.entity.UserBlock;
import com.linknest.linknest.entity.MutedUser;
import com.linknest.linknest.entity.MutedHashtag;
import com.linknest.linknest.repository.UserBlockRepository;
import com.linknest.linknest.repository.MutedUserRepository;
import com.linknest.linknest.repository.MutedHashtagRepository;

import com.linknest.linknest.entity.User;
import com.linknest.linknest.entity.Post;
import com.linknest.linknest.entity.Follow;
import com.linknest.linknest.entity.Hashtag;
import com.linknest.linknest.repository.UserRepository;
import com.linknest.linknest.repository.PostRepository;
import com.linknest.linknest.repository.FollowRepository;
import com.linknest.linknest.repository.HashtagRepository;
import com.linknest.linknest.dto.PostRequest;
import com.linknest.linknest.dto.PostResponse;
import com.linknest.linknest.dto.PostEditRequest;
import com.linknest.linknest.entity.Comment;
import com.linknest.linknest.entity.Like;
import com.linknest.linknest.entity.Notification;
import com.linknest.linknest.repository.CommentRepository;
import com.linknest.linknest.repository.LikeRepository;
import com.linknest.linknest.repository.NotificationRepository;
import com.linknest.linknest.repository.BookmarkRepository;
import com.linknest.linknest.entity.Bookmark;
import com.linknest.linknest.dto.CommentEditRequest;


@RestController
@RequestMapping("/api/posts")
public class PostController {

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private LikeRepository likeRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private FollowRepository followRepository;

    @Autowired
    private BookmarkRepository bookmarkRepository;

    @Autowired
    private NotificationWebSocketService notificationWebSocketService;

    @Autowired
    private HashtagRepository hashtagRepository;

    @Autowired
    private UserBlockRepository userBlockRepository;

    @Autowired
    private MutedUserRepository mutedUserRepository;
    @Autowired
    private MutedHashtagRepository mutedHashtagRepository;

    @Autowired
    private EmailService emailService;

    private Long getAuthenticatedUserId() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        System.out.println("Authenticated username: " + username);
        return userRepository.findByUsername(username)
                .map(User::getId)
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"));
    }

    @PostMapping
    public ResponseEntity<PostResponse> createPost(@Validated @RequestBody PostRequest postRequest) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Post post = new Post();
        post.setTitle(postRequest.getTitle());
        // Sanitize content
        String sanitizedContent = Jsoup.clean(postRequest.getContent(), Safelist.basicWithImages());
        post.setContent(sanitizedContent);
        post.setMediaUrl(postRequest.getMediaUrl());
        post.setUser(user);
        post.setCreatedAt(LocalDateTime.now());
        post.setHashtags(extractAndPersistHashtags(sanitizedContent));
        Post savedPost = postRepository.save(post);
        PostResponse response = new PostResponse(
                savedPost.getId(),
                savedPost.getTitle(),
                savedPost.getContent(),
                savedPost.getMediaUrl(),
                savedPost.getCreatedAt(),
                new PostResponse.UserResponse(
                        user.getId(),
                        user.getUsername(),
                        user.getBio(),
                        user.getLocation(),
                        user.getWebsite(),
                        user.getProfilePictureUrl(),
                        user.getInterests()
                ),
                savedPost.getLikeCount()
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<?> getPosts(@RequestParam(required = false) Long userId,
                                      @RequestParam(defaultValue = "0") int page,
                                      @RequestParam(defaultValue = "10") int size,
                                      @RequestParam(defaultValue = "newest") String sort,
                                      @RequestParam(required = false) Boolean mediaOnly,
                                      @RequestParam(required = false) String search,
                                      @RequestParam(required = false) String hashtag) {
        if (page < 0) {
            return ResponseEntity.badRequest().body("Page number cannot be negative");
        }
        if (size <= 0) {
            return ResponseEntity.badRequest().body("Page size must be positive");
        }
        Pageable pageable = PageRequest.of(page, size);
        Page<Post> posts;
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        List<Long> userIds = null;
        final List<Long>[] mutedUserIdsHolder = new List[1];
        final List<String>[] mutedHashtagsHolder = new List[1];
        if (username != null && !username.equals("anonymousUser")) {
            User currentUser = userRepository.findByUsername(username).orElse(null);
            if (currentUser != null) {
                List<Follow> following = followRepository.findByFollower(currentUser);
                userIds = following.stream().map(f -> f.getFollowing().getId()).toList();
                userIds = new java.util.ArrayList<>(userIds);
                userIds.add(currentUser.getId());
                // Get muted users and hashtags
                mutedUserIdsHolder[0] = mutedUserRepository.findByUserId(currentUser.getId()).stream()
                    .map(mu -> mu.getMutedUser().getId()).toList();
                mutedHashtagsHolder[0] = mutedHashtagRepository.findByUserId(currentUser.getId()).stream()
                    .map(mh -> mh.getHashtag().getName().toLowerCase()).toList();
            }
        }
        if (userId != null) {
            userIds = java.util.List.of(userId);
            // Privacy check for profile feed
            User profileUser = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
            boolean isPrivate = profileUser.isPrivateProfile();
            User currentUser = userRepository.findByUsername(username).orElse(null);
            boolean isAdmin = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .map(a -> a.getAuthority()).anyMatch(role -> role.equals("ROLE_ADMIN"));
            boolean isSelf = currentUser != null && currentUser.getId().equals(userId);
            boolean isFollower = false;
            if (currentUser != null) {
                isFollower = followRepository.existsByFollowerAndFollowing(currentUser, profileUser);
            }
            if (isPrivate && !isSelf && !isAdmin && !isFollower) {
                return ResponseEntity.status(403).body("Cannot view posts from a private profile");
            }
            // Show pinned posts first for profile feed
            posts = postRepository.findByUserIdOrderByPinnedDescCreatedAtDesc(userId, pageable);
        } else {
            if (userIds == null) {
                userIds = userRepository.findAll().stream().map(User::getId).toList();
            }
            if (search != null && !search.isBlank()) {
                posts = postRepository.searchByUserIdInAndKeyword(userIds, search, pageable);
            } else if (hashtag != null && !hashtag.isBlank()) {
                posts = postRepository.searchByUserIdInAndHashtag(userIds, hashtag.startsWith("#") ? hashtag : ("#" + hashtag), pageable);
            } else if (Boolean.TRUE.equals(mediaOnly)) {
                posts = postRepository.findByUserIdInAndMediaUrlIsNotNull(userIds, pageable);
            } else if ("oldest".equalsIgnoreCase(sort)) {
                posts = postRepository.findByUserIdInOrderByCreatedAtAsc(userIds, pageable);
            } else if ("mostLiked".equalsIgnoreCase(sort)) {
                posts = postRepository.findByUserIdInOrderByLikeCountDesc(userIds, pageable);
            } else {
                posts = postRepository.findByUserIdInOrderByCreatedAtDesc(userIds, pageable);
            }
        }
        posts.getContent().forEach(post -> post.setLikeCount(likeRepository.countByPostId(post.getId())));
        // After posts are fetched, filter out muted users and hashtags
        List<Post> filteredPosts = posts.getContent().stream()
            .filter(post -> mutedUserIdsHolder[0] == null || !mutedUserIdsHolder[0].contains(post.getUser().getId()))
            .filter(post -> {
                if (mutedHashtagsHolder[0] == null || post.getHashtags() == null) return true;
                for (Hashtag tag : post.getHashtags()) {
                    if (mutedHashtagsHolder[0].contains(tag.getName().toLowerCase())) return false;
                }
                return true;
            })
            .collect(Collectors.toList());
        // Map to response page
        Page<PostResponse> responsePage = new org.springframework.data.domain.PageImpl<>(
            filteredPosts.stream().map(post -> new PostResponse(
                post.getId(),
                post.getTitle(),
                post.getContent(),
                post.getMediaUrl(),
                post.getCreatedAt(),
                new PostResponse.UserResponse(
                    post.getUser().getId(),
                    post.getUser().getUsername(),
                    post.getUser().getBio(),
                    post.getUser().getLocation(),
                    post.getUser().getWebsite(),
                    post.getUser().getProfilePictureUrl(),
                    post.getUser().getInterests()
                ),
                post.getLikeCount()
            )).collect(Collectors.toList()), pageable, posts.getTotalElements()
        );
        return ResponseEntity.ok(responsePage);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updatePost(@PathVariable Long id, @Validated @RequestBody PostEditRequest postEditRequest) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("User not found"));
        Optional<Post> post = postRepository.findById(id);
        if (post.isEmpty()) {
            return ResponseEntity.status(404).body("Post with ID " + id + " not found");
        }
        boolean isAdmin = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role -> role.equals("ROLE_ADMIN"));
        if (!post.get().getUser().getId().equals(user.getId()) && !isAdmin) {
            return ResponseEntity.status(403).body("You are not authorized to update this post");
        }
        post.get().setTitle(postEditRequest.getTitle());
        // Sanitize content
        String sanitizedContent = Jsoup.clean(postEditRequest.getContent(), Safelist.basicWithImages());
        post.get().setContent(sanitizedContent);
        post.get().setMediaUrl(postEditRequest.getMediaUrl());
        post.get().setHashtags(extractAndPersistHashtags(sanitizedContent));
        Post updatedPost = postRepository.save(post.get());
        updatedPost.setLikeCount(likeRepository.countByPostId(updatedPost.getId()));
        PostResponse response = new PostResponse(
                updatedPost.getId(),
                updatedPost.getTitle(),
                updatedPost.getContent(),
                updatedPost.getMediaUrl(),
                updatedPost.getCreatedAt(),
                new PostResponse.UserResponse(
                        updatedPost.getUser().getId(),
                        updatedPost.getUser().getUsername(),
                        updatedPost.getUser().getBio(),
                        updatedPost.getUser().getLocation(),
                        updatedPost.getUser().getWebsite(),
                        updatedPost.getUser().getProfilePictureUrl(),
                        updatedPost.getUser().getInterests()
                ),
                updatedPost.getLikeCount()
        );
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletePost(@PathVariable Long id) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("User not found"));
        Optional<Post> post = postRepository.findById(id);
        if (post.isEmpty()) {
            return ResponseEntity.status(404).body("Post with ID " + id + " not found");
        }
        boolean isAdmin = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role -> role.equals("ROLE_ADMIN"));
        if (!post.get().getUser().getId().equals(user.getId()) && !isAdmin) {
            return ResponseEntity.status(403).body("You are not authorized to delete this post");
        }
        postRepository.delete(post.get());
        return ResponseEntity.ok().body("Post with ID " + id + " deleted successfully");
    }

    @PostMapping("/{postId}/comments")
    public ResponseEntity<Comment> createComment(@PathVariable Long postId, @Valid @RequestBody Comment comment) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found"));
        Post post = postRepository.findById(postId).orElseThrow(() -> new RuntimeException("Post not found"));
        // Block check
        if (userBlockRepository.existsByBlockerIdAndBlockedId(user.getId(), post.getUser().getId()) ||
            userBlockRepository.existsByBlockerIdAndBlockedId(post.getUser().getId(), user.getId())) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "Cannot comment: one of the users has blocked the other.");
        }
        comment.setUser(user);
        comment.setPost(post);
        Comment savedComment = commentRepository.save(comment);
        if (!post.getUser().getId().equals(user.getId())) {
            String message = user.getUsername() + " commented on your post: '" + post.getTitle() + "'";
            Long targetId = post.getId();
            String targetType = "post";
            String data = "{\"commentId\": " + savedComment.getId() + ", \"commentText\": \"" + savedComment.getContent().replace("\"", "\\\"") + "\"}";
            Notification notification = new Notification(
                post.getUser(), "comment", post, savedComment, message, targetId, targetType, data);
            notificationRepository.save(notification);
            notificationWebSocketService.sendNotification(post.getUser().getId(), message);
            // Send email notification if preferences allow
            User postOwner = post.getUser();
            if (postOwner.isNotifyComments() && postOwner.getEmail() != null && !postOwner.getEmail().isBlank()) {
                String subject = "New comment on your post on LinkNest!";
                String text = "Hi " + postOwner.getUsername() + ",\n\n" +
                        user.getUsername() + " commented on your post: '" + post.getTitle() + "'.";
                emailService.sendEmail(postOwner.getEmail(), subject, text);
            }
        }
        // In createComment, after saving notification, check notifyComments preference
        // (This block is now redundant and uses the old constructor, so remove it)
        return ResponseEntity.ok(savedComment);
    }

    @GetMapping("/{postId}/comments")
    public ResponseEntity<?> getComments(@PathVariable Long postId,
                                         @RequestParam(defaultValue = "0") int page,
                                         @RequestParam(defaultValue = "5") int size) {
        Optional<Post> post = postRepository.findById(postId);
        if (post.isEmpty()) {
            return ResponseEntity.status(404).body("Post with ID " + postId + " not found");
        }

        if (page < 0) {
            return ResponseEntity.badRequest().body("Page number cannot be negative");
        }
        if (size <= 0) {
            return ResponseEntity.badRequest().body("Page size must be positive");
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<Comment> comments = commentRepository.findByPostId(postId, pageable);
        comments.getContent().forEach(comment -> comment.setLikeCount(likeRepository.countByCommentId(comment.getId())));
        return ResponseEntity.ok(comments);
    }

    @PutMapping("/{postId}/comments/{commentId}")
    public ResponseEntity<?> updateComment(@PathVariable Long postId, @PathVariable Long commentId, @Valid @RequestBody CommentEditRequest commentEditRequest) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Optional<Post> post = postRepository.findById(postId);
        if (post.isEmpty()) {
            return ResponseEntity.status(404).body("Post with ID " + postId + " not found");
        }
        Optional<Comment> comment = commentRepository.findById(commentId);
        if (comment.isEmpty()) {
            return ResponseEntity.status(404).body("Comment with ID " + commentId + " not found");
        }
        boolean isAdmin = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role -> role.equals("ROLE_ADMIN"));
        if (!comment.get().getUser().getId().equals(currentUser.getId()) && !isAdmin) {
            return ResponseEntity.status(403).body("You are not authorized to update this comment");
        }
        comment.get().setContent(commentEditRequest.getContent());
        Comment updatedComment = commentRepository.save(comment.get());
        updatedComment.setLikeCount(likeRepository.countByCommentId(updatedComment.getId()));
        return ResponseEntity.ok(updatedComment);
    }

    @DeleteMapping("/{postId}/comments/{commentId}")
    public ResponseEntity<?> deleteComment(@PathVariable Long postId, @PathVariable Long commentId) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Optional<Post> post = postRepository.findById(postId);
        if (post.isEmpty()) {
            return ResponseEntity.status(404).body("Post with ID " + postId + " not found");
        }
        Optional<Comment> comment = commentRepository.findById(commentId);
        if (comment.isEmpty()) {
            return ResponseEntity.status(404).body("Comment with ID " + commentId + " not found");
        }
        boolean isAdmin = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role -> role.equals("ROLE_ADMIN"));
        if (!comment.get().getUser().getId().equals(currentUser.getId()) && !isAdmin) {
            return ResponseEntity.status(403).body("You are not authorized to delete this comment");
        }
        commentRepository.delete(comment.get());
        return ResponseEntity.ok().body("Comment with ID " + commentId + " deleted successfully");
    }

    @PostMapping("/{itemId}/like")
    public ResponseEntity<?> likeItem(@PathVariable Long itemId, @RequestParam Long userId, @RequestParam(required = false) String type) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        Post post = postRepository.findById(itemId).orElseThrow(() -> new RuntimeException("Post not found"));
        // Block check
        if (userBlockRepository.existsByBlockerIdAndBlockedId(user.getId(), post.getUser().getId()) ||
            userBlockRepository.existsByBlockerIdAndBlockedId(post.getUser().getId(), user.getId())) {
            return ResponseEntity.status(403).body("Cannot like: one of the users has blocked the other.");
        }

        if (type == null || (!type.equals("post") && !type.equals("comment"))) {
            return ResponseEntity.badRequest().body("Type must be 'post' or 'comment'");
        }

        if (type.equals("post")) {
            if (likeRepository.existsByUserIdAndPostId(user.getId(), itemId)) {
                return ResponseEntity.badRequest().body("User has already liked this post");
            }
            Like like = new Like(user, post, null);
            likeRepository.save(like);

            // Create notification for the post owner
            if (!post.getUser().getId().equals(user.getId())) {
                User postOwner = post.getUser();
                if (postOwner.isNotifyLikes()) {
                    String message = user.getUsername() + " liked your post: '" + post.getTitle() + "'";
                    Long targetId = post.getId();
                    String targetType = "post";
                    String data = null;
                    Notification notification = new Notification(
                        postOwner, "like", post, null, message, targetId, targetType, data);
                    notificationRepository.save(notification);
                    notificationWebSocketService.sendNotification(postOwner.getId(), message);
                    // Send email notification if preferences allow
                    if (postOwner.getEmail() != null && !postOwner.getEmail().isBlank()) {
                        String subject = "Your post was liked on LinkNest!";
                        String text = "Hi " + postOwner.getUsername() + ",\n\n" +
                                user.getUsername() + " liked your post: '" + post.getTitle() + "'.";
                        emailService.sendEmail(postOwner.getEmail(), subject, text);
                    }
                }
            }
        } else { // type.equals("comment")
            Optional<Comment> comment = commentRepository.findById(itemId);
            if (comment.isEmpty()) {
                return ResponseEntity.status(404).body("Comment with ID " + itemId + " not found");
            }
            if (likeRepository.existsByUserIdAndCommentId(user.getId(), itemId)) {
                return ResponseEntity.badRequest().body("User has already liked this comment");
            }
            Like like = new Like(user, null, comment.get());
            likeRepository.save(like);

            // Create notification for the comment owner
            if (!comment.get().getUser().getId().equals(user.getId())) {
                User commentOwner = comment.get().getUser();
                if (commentOwner.isNotifyLikes()) {
                    String message = user.getUsername() + " liked your comment on post: '" + comment.get().getPost().getTitle() + "'";
                    Long targetId = comment.get().getId();
                    String targetType = "comment";
                    String data = null;
                    Notification notification = new Notification(
                        commentOwner, "like", null, comment.get(), message, targetId, targetType, data);
                    notificationRepository.save(notification);
                    notificationWebSocketService.sendNotification(commentOwner.getId(), message);
                }
            }
        }

        return ResponseEntity.ok().body("Item with ID " + itemId + " liked successfully");
    }

    @DeleteMapping("/{itemId}/like")
    public ResponseEntity<?> unlikeItem(@PathVariable Long itemId, @RequestParam Long userId, @RequestParam(required = false) String type) {
        Optional<User> user = userRepository.findById(userId);
        if (user.isEmpty()) {
            return ResponseEntity.badRequest().body("User with ID " + userId + " not found");
        }

        if (type == null || (!type.equals("post") && !type.equals("comment"))) {
            return ResponseEntity.badRequest().body("Type must be 'post' or 'comment'");
        }

        Optional<Like> likeToDelete = Optional.empty();
        if (type.equals("post")) {
            if (!likeRepository.existsByUserIdAndPostId(userId, itemId)) {
                return ResponseEntity.status(404).body("Like not found for user on post with ID " + itemId);
            }
            likeToDelete = likeRepository.findAll().stream()
                    .filter(l -> l.getUser().getId().equals(userId) && l.getPost() != null && l.getPost().getId().equals(itemId))
                    .findFirst();
        } else { // type.equals("comment")
            if (!likeRepository.existsByUserIdAndCommentId(userId, itemId)) {
                return ResponseEntity.status(404).body("Like not found for user on comment with ID " + itemId);
            }
            likeToDelete = likeRepository.findAll().stream()
                    .filter(l -> l.getUser().getId().equals(userId) && l.getComment() != null && l.getComment().getId().equals(itemId))
                    .findFirst();
        }

        if (likeToDelete.isEmpty()) {
            return ResponseEntity.status(404).body("Like not found for user on item with ID " + itemId);
        }

        likeRepository.delete(likeToDelete.get());
        return ResponseEntity.ok().body("Item with ID " + itemId + " unliked successfully");
    }

    @PostMapping("/{id}/bookmark")
    public ResponseEntity<?> bookmarkPost(@PathVariable Long id) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        if (username == null || username.equals("anonymousUser")) {
            return ResponseEntity.status(401).body("Unauthorized");
        }
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Post not found"));
        // Block check
        if (userBlockRepository.existsByBlockerIdAndBlockedId(user.getId(), post.getUser().getId()) ||
            userBlockRepository.existsByBlockerIdAndBlockedId(post.getUser().getId(), user.getId())) {
            return ResponseEntity.status(403).body("Cannot bookmark: one of the users has blocked the other.");
        }
        if (bookmarkRepository.existsByUserAndPost(user, post)) {
            return ResponseEntity.badRequest().body("Already bookmarked");
        }
        bookmarkRepository.save(new Bookmark(user, post));
        return ResponseEntity.ok().body("Bookmarked post with ID " + id);
    }

    @DeleteMapping("/{id}/bookmark")
    public ResponseEntity<?> removeBookmark(@PathVariable Long id) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        if (username == null || username.equals("anonymousUser")) {
            return ResponseEntity.status(401).body("Unauthorized");
        }
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Post not found"));
        return bookmarkRepository.findByUserAndPost(user, post)
                .map(b -> {
                    bookmarkRepository.delete(b);
                    return ResponseEntity.ok().body("Removed bookmark for post with ID " + id);
                })
                .orElseGet(() -> ResponseEntity.badRequest().body("Bookmark not found"));
    }

    @PostMapping("/{id}/pin")
    public ResponseEntity<?> pinPost(@PathVariable Long id) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        if (username == null || username.equals("anonymousUser")) {
            return ResponseEntity.status(401).body("Unauthorized");
        }
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Post not found"));
        // Block check
        if (userBlockRepository.existsByBlockerIdAndBlockedId(user.getId(), post.getUser().getId()) ||
            userBlockRepository.existsByBlockerIdAndBlockedId(post.getUser().getId(), user.getId())) {
            return ResponseEntity.status(403).body("Cannot pin: one of the users has blocked the other.");
        }
        boolean isAdmin = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .map(a -> a.getAuthority()).anyMatch(role -> role.equals("ROLE_ADMIN"));
        if (!post.getUser().getId().equals(user.getId()) && !isAdmin) {
            return ResponseEntity.status(403).body("Forbidden");
        }
        post.setPinned(true);
        postRepository.save(post);
        return ResponseEntity.ok().body("Pinned post with ID " + id);
    }

    @DeleteMapping("/{id}/pin")
    public ResponseEntity<?> unpinPost(@PathVariable Long id) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        if (username == null || username.equals("anonymousUser")) {
            return ResponseEntity.status(401).body("Unauthorized");
        }
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Post not found"));
        boolean isAdmin = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .map(a -> a.getAuthority()).anyMatch(role -> role.equals("ROLE_ADMIN"));
        if (!post.getUser().getId().equals(user.getId()) && !isAdmin) {
            return ResponseEntity.status(403).body("Forbidden");
        }
        post.setPinned(false);
        postRepository.save(post);
        return ResponseEntity.ok().body("Unpinned post with ID " + id);
    }

    @GetMapping("/notifications")
    public ResponseEntity<?> getNotifications(@RequestParam Long userId,
                                              @RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "10") int size) {
        Optional<User> user = userRepository.findById(userId);
        if (user.isEmpty()) {
            return ResponseEntity.badRequest().body("User with ID " + userId + " not found");
        }

        if (page < 0) {
            return ResponseEntity.badRequest().body("Page number cannot be negative");
        }
        if (size <= 0) {
            return ResponseEntity.badRequest().body("Page size must be positive");
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<Notification> notifications = notificationRepository.findByUserId(userId, pageable);
        return ResponseEntity.ok(notifications);
    }

    @PatchMapping("/notifications/{id}")
    public ResponseEntity<?> markNotificationAsRead(@PathVariable Long id, @RequestParam Long userId) {
        Optional<User> user = userRepository.findById(userId);
        if (user.isEmpty()) {
            return ResponseEntity.badRequest().body("User with ID " + userId + " not found");
        }

        Optional<Notification> notification = notificationRepository.findById(id);
        if (notification.isEmpty()) {
            return ResponseEntity.status(404).body("Notification with ID " + id + " not found");
        }

        // Check if the user is the owner or an admin (userId=2)
        if (!notification.get().getUser().getId().equals(userId) && !userId.equals(2L)) {
            return ResponseEntity.status(403).body("You are not authorized to mark this notification as read");
        }

        notification.get().setRead(true);
        notificationRepository.save(notification.get());
        return ResponseEntity.ok().body("Notification with ID " + id + " marked as read");
    }

    @PostMapping("/upload-media")
    public ResponseEntity<?> uploadMedia(@RequestParam("file") MultipartFile file) {
        // Security: Only authenticated users can upload
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        if (username == null || username.equals("anonymousUser")) {
            return ResponseEntity.status(401).body("Unauthorized");
        }
        // Validate file
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("File is empty");
        }
        String contentType = file.getContentType();
        if (contentType == null || !(contentType.matches("image/(jpeg|png|gif)") || contentType.matches("video/(mp4|webm|ogg)"))) {
            return ResponseEntity.badRequest().body("Only JPEG, PNG, GIF images and MP4, WebM, OGG videos are allowed");
        }
        long maxSize = 10 * 1024 * 1024; // 10MB
        if (file.getSize() > maxSize) {
            return ResponseEntity.badRequest().body("File size exceeds 10MB limit");
        }
        // Prepare upload directory
        String uploadDir = "uploads/post-media/";
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            try {
                Files.createDirectories(uploadPath);
            } catch (IOException e) {
                return ResponseEntity.status(500).body("Failed to create upload directory");
            }
        }
        // Generate unique filename
        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
        String fileExtension = originalFilename.substring(originalFilename.lastIndexOf('.'));
        String newFilename = "media-" + System.currentTimeMillis() + fileExtension;
        Path destPath = uploadPath.resolve(newFilename);
        // Save file
        try {
            Files.copy(file.getInputStream(), destPath);
        } catch (IOException e) {
            return ResponseEntity.status(500).body("Failed to save file: " + e.getMessage());
        }
        // Return media URL
        String mediaUrl = "/" + uploadDir + newFilename;
        return ResponseEntity.ok().body(mediaUrl);
    }

    @GetMapping("/trending-hashtags")
    public ResponseEntity<?> getTrendingHashtags() {
        Pageable pageable = PageRequest.of(0, 100);
        Page<Post> recentPosts = postRepository.findAllByOrderByCreatedAtDesc(pageable);
        Pattern hashtagPattern = Pattern.compile("#\\w+");
        Map<String, Integer> hashtagCounts = new HashMap<>();
        for (Post post : recentPosts) {
            Matcher matcher = hashtagPattern.matcher(post.getContent());
            while (matcher.find()) {
                String hashtag = matcher.group().toLowerCase();
                hashtagCounts.put(hashtag, hashtagCounts.getOrDefault(hashtag, 0) + 1);
            }
        }
        // Get top 10 hashtags
        var trending = hashtagCounts.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .limit(10)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        return ResponseEntity.ok(trending);
    }

    @GetMapping("/recommendations")
    public ResponseEntity<?> getPostRecommendations() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        if (username == null || username.equals("anonymousUser")) {
            return ResponseEntity.status(401).body("Unauthorized");
        }
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        var interests = currentUser.getInterests();
        if (interests == null || interests.isEmpty()) {
            return ResponseEntity.ok(java.util.Collections.emptyList());
        }
        // Find posts matching any interest, not by the user
        List<Post> allPosts = postRepository.findAll();
        List<Post> recommended = allPosts.stream()
                .filter(p -> !p.getUser().getId().equals(currentUser.getId()))
                .filter(p -> interests.stream().anyMatch(i ->
                        (p.getTitle() != null && p.getTitle().toLowerCase().contains(i.toLowerCase())) ||
                        (p.getContent() != null && p.getContent().toLowerCase().contains(i.toLowerCase()))
                ))
                .limit(10)
                .collect(java.util.stream.Collectors.toList());
        List<PostResponse> response = recommended.stream().map(post -> new PostResponse(
                post.getId(),
                post.getTitle(),
                post.getContent(),
                post.getMediaUrl(),
                post.getCreatedAt(),
                new PostResponse.UserResponse(
                        post.getUser().getId(),
                        post.getUser().getUsername(),
                        post.getUser().getBio(),
                        post.getUser().getLocation(),
                        post.getUser().getWebsite(),
                        post.getUser().getProfilePictureUrl(),
                        post.getUser().getInterests()
                ),
                post.getLikeCount()
        )).collect(java.util.stream.Collectors.toList());
        return ResponseEntity.ok(response);
    }

    // Helper method to extract and persist hashtags
    private java.util.Set<Hashtag> extractAndPersistHashtags(String content) {
        java.util.Set<Hashtag> hashtags = new java.util.HashSet<>();
        if (content == null) return hashtags;
        java.util.regex.Pattern hashtagPattern = java.util.regex.Pattern.compile("#\\w+");
        java.util.regex.Matcher matcher = hashtagPattern.matcher(content);
        while (matcher.find()) {
            String tag = matcher.group().toLowerCase();
            Hashtag hashtag = hashtagRepository.findByNameIgnoreCase(tag)
                    .orElseGet(() -> hashtagRepository.save(new Hashtag(tag)));
            hashtags.add(hashtag);
        }
        return hashtags;
    }
}