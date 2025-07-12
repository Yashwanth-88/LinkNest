package com.linknest.linknest.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import org.springframework.security.core.GrantedAuthority;
import java.util.List;
import java.util.stream.Collectors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Map;
import java.util.HashMap;
import java.time.LocalDate;
import java.time.LocalDateTime;
import com.linknest.linknest.entity.User;
import com.linknest.linknest.entity.Follow;
import com.linknest.linknest.entity.Bookmark;
import com.linknest.linknest.entity.Post;
import com.linknest.linknest.entity.Report;
import com.linknest.linknest.entity.Like;
import com.linknest.linknest.entity.Comment;
import com.linknest.linknest.entity.UserBlock;
import com.linknest.linknest.entity.Notification;
import com.linknest.linknest.repository.UserRepository;
import com.linknest.linknest.repository.FollowRepository;
import com.linknest.linknest.repository.BookmarkRepository;
import com.linknest.linknest.repository.PostRepository;
import com.linknest.linknest.repository.ReportRepository;
import com.linknest.linknest.repository.LikeRepository;
import com.linknest.linknest.repository.CommentRepository;
import com.linknest.linknest.repository.UserBlockRepository;
import com.linknest.linknest.repository.NotificationRepository;
import com.linknest.linknest.repository.UserBadgeRepository;
import com.linknest.linknest.service.NotificationWebSocketService;
import com.linknest.linknest.service.EmailService;
import com.linknest.linknest.util.JwtUtil;
import com.linknest.linknest.dto.UserResponse;
import com.linknest.linknest.dto.UserProfileRequest;
import com.linknest.linknest.dto.UserAnalyticsResponse;
import com.linknest.linknest.dto.AdminAnalyticsResponse;
import com.linknest.linknest.dto.PostResponse;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private FollowRepository followRepository;

    @Autowired
    private BookmarkRepository bookmarkRepository;

    @Autowired
    private NotificationWebSocketService notificationWebSocketService;

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private LikeRepository likeRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private UserBlockRepository userBlockRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private UserBadgeRepository userBadgeRepository;

    @Value("${upload.dir:uploads/profile-pictures/}")
    private String uploadDir;

    @PostMapping
    public ResponseEntity<User> createUser(@Valid @RequestBody User user) {
        System.out.println("Received user: " + user.getUsername() + ", plain password: " + user.getPassword());
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        // Email is now part of the User entity and should be set by the client
        User savedUser = userRepository.save(user);
        return ResponseEntity.ok(savedUser);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        boolean isAdmin = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role -> role.equals("ROLE_ADMIN"));
        boolean isPrivate = user.isPrivateProfile();

        if (!user.getId().equals(id) && !isAdmin && isPrivate) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot view private profile");
        }

        return ResponseEntity.ok(new UserResponse(
                        user.getId(),
                        user.getUsername(),
                        user.getBio(),
                        user.getLocation(),
                        user.getWebsite(),
                        user.getProfilePictureUrl(),
                        user.getInterests()
                ));
    }

    @PostMapping("/authenticate")
    public ResponseEntity<?> authenticateUser(@RequestBody AuthenticationRequest authRequest) throws JsonProcessingException {
        System.out.println("Received raw authentication request body: " + new ObjectMapper().writeValueAsString(authRequest));
        System.out.println("Received authentication request: username=" + authRequest.username() + ", password=" + authRequest.password());
        if (authRequest.username() == null || authRequest.password() == null) {
            System.err.println("Invalid authentication request: username or password is null");
            return ResponseEntity.badRequest().body("Username and password are required");
        }
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(authRequest.username(), authRequest.password()));
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String jwt = jwtUtil.generateToken(userDetails);
        System.out.println("Generated JWT for user: " + authRequest.username());
        return ResponseEntity.ok(new AuthenticationResponse(jwt));
    }

    @PutMapping("/{id}/profile")
    public ResponseEntity<UserResponse> updateUserProfile(@PathVariable Long id, @Valid @RequestBody UserProfileRequest request) {
        // Security check: Only the user or admin can update
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Unauthorized access"));
        boolean isAdmin = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role -> role.equals("ROLE_ADMIN"));
        if (!currentUser.getId().equals(id) && !isAdmin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot update another user's profile");
        }
        return userRepository.findById(id)
                .map(user -> {
                    user.setBio(request.getBio());
                    user.setLocation(request.getLocation());
                    user.setWebsite(request.getWebsite());
                    user.setProfilePictureUrl(request.getProfilePictureUrl());
                    user.setEmail(request.getEmail()); // Allow updating email
                    User updated = userRepository.save(user);
                    return ResponseEntity.ok(new UserResponse(
                        updated.getId(),
                        updated.getUsername(),
                        updated.getBio(),
                        updated.getLocation(),
                        updated.getWebsite(),
                        updated.getProfilePictureUrl(),
                        updated.getInterests()
                    ));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/profile-picture")
    public ResponseEntity<User> updateProfilePicture(@PathVariable Long id, @Valid @RequestBody UpdateProfilePictureRequest request) {
        return userRepository.findById(id)
                .map(user -> {
                    user.setProfilePictureUrl(request.profilePictureUrl());
                    User updated = userRepository.save(user);
                    return ResponseEntity.ok(updated);
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/profile-picture/upload")
    public ResponseEntity<?> uploadProfilePicture(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        // Security check: Ensure the authenticated user matches the id
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Unauthorized access"));
        if (!user.getId().equals(id)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot update another user's profile picture");
        }

        // Validate file
        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is empty");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.matches("image/(jpeg|png|gif)")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only JPEG, PNG, and GIF images are allowed");
        }
        long maxSize = 2 * 1024 * 1024; // 2MB
        if (file.getSize() > maxSize) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File size exceeds 2MB limit");
        }

        // Prepare upload directory
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            try {
                Files.createDirectories(uploadPath);
            } catch (IOException e) {
                System.err.println("Failed to create directory " + uploadPath + ": " + e.getMessage());
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to initialize upload directory");
            }
        }

        // Generate unique filename
        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
        String fileExtension = originalFilename.substring(originalFilename.lastIndexOf('.'));
        String newFilename = "user-" + id + "-" + System.currentTimeMillis() + fileExtension;
        Path destPath = uploadPath.resolve(newFilename);

        // Save file
        try {
            Files.copy(file.getInputStream(), destPath);
        } catch (IOException e) {
            System.err.println("Failed to save file " + newFilename + ": " + e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to save file: " + e.getMessage());
        }

        // Update user
        user.setProfilePictureUrl("/" + uploadPath.relativize(destPath).toString());
        userRepository.save(user);

        // Return only the updated field for security
        return ResponseEntity.ok(new ProfilePictureResponse(user.getProfilePictureUrl()));
    }

    @PutMapping("/{id}/privacy")
    public ResponseEntity<?> updatePrivacy(@PathVariable Long id, @RequestParam boolean privateProfile) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        boolean isAdmin = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .map(a -> a.getAuthority()).anyMatch(role -> role.equals("ROLE_ADMIN"));
        if (!currentUser.getId().equals(id) && !isAdmin) {
            return ResponseEntity.status(403).body("Forbidden");
        }
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setPrivateProfile(privateProfile);
        userRepository.save(user);
        return ResponseEntity.ok().body("Privacy setting updated");
    }

    @PostMapping("/{id}/follow")
    public ResponseEntity<?> followUser(@PathVariable Long id) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User follower = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        User following = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        // Block check
        if (userBlockRepository.existsByBlockerAndBlocked(follower, following) ||
            userBlockRepository.existsByBlockerAndBlocked(following, follower)) {
            return ResponseEntity.status(403).body("Cannot follow: one of the users has blocked the other.");
        }
        if (followRepository.existsByFollowerAndFollowing(follower, following)) {
            return ResponseEntity.badRequest().body("Already following");
        }
        Follow follow = new Follow();
        follow.setFollower(follower);
        follow.setFollowing(following);
        followRepository.save(follow);
        // Send real-time notification to the followed user
        String message = follower.getUsername() + " started following you.";
        Long targetId = follower.getId();
        String targetType = "user";
        String data = null;
        Notification notification = new Notification(
            following, "follow", null, null, message, targetId, targetType, data);
        notificationRepository.save(notification);
        notificationWebSocketService.sendNotification(following.getId(), message);
        // Send email notification if preferences allow
        if (following.isNotifyFollows() && following.getEmail() != null && !following.getEmail().isBlank()) {
            String subject = "You have a new follower on LinkNest!";
            String text = "Hi " + following.getUsername() + ",\n\n" +
                    follower.getUsername() + " just started following you on LinkNest!";
            emailService.sendEmail(following.getEmail(), subject, text);
        }
        return ResponseEntity.ok("Followed successfully");
    }

    @DeleteMapping("/{id}/follow")
    public ResponseEntity<?> unfollowUser(@PathVariable Long id) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User follower = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        User following = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User to unfollow not found"));
        return followRepository.findByFollowerAndFollowing(follower, following)
                .map(follow -> {
                    followRepository.delete(follow);
                    return ResponseEntity.ok().body("Unfollowed user with ID " + id);
                })
                .orElseGet(() -> ResponseEntity.badRequest().body("Not following this user"));
    }

    @GetMapping("/{id}/followers")
    public ResponseEntity<?> getFollowers(@PathVariable Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        List<Follow> followers = followRepository.findByFollowing(user);
        List<UserResponse> followerResponses = followers.stream()
                .map(f -> new UserResponse(
                        f.getFollower().getId(),
                        f.getFollower().getUsername(),
                        f.getFollower().getBio(),
                        f.getFollower().getLocation(),
                        f.getFollower().getWebsite(),
                        f.getFollower().getProfilePictureUrl(),
                        f.getFollower().getInterests()
                ))
                .toList();
        return ResponseEntity.ok(followerResponses);
    }

    @GetMapping("/{id}/following")
    public ResponseEntity<?> getFollowing(@PathVariable Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        List<Follow> following = followRepository.findByFollower(user);
        List<UserResponse> followingResponses = following.stream()
                .map(f -> new UserResponse(
                        f.getFollowing().getId(),
                        f.getFollowing().getUsername(),
                        f.getFollowing().getBio(),
                        f.getFollowing().getLocation(),
                        f.getFollowing().getWebsite(),
                        f.getFollowing().getProfilePictureUrl(),
                        f.getFollowing().getInterests()
                ))
                .toList();
        return ResponseEntity.ok(followingResponses);
    }

    @GetMapping("/{id}/bookmarks")
    public ResponseEntity<?> getUserBookmarks(@PathVariable Long id) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        boolean isAdmin = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .map(a -> a.getAuthority()).anyMatch(role -> role.equals("ROLE_ADMIN"));
        if (!currentUser.getId().equals(id) && !isAdmin) {
            return ResponseEntity.status(403).body("Forbidden");
        }
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        var bookmarks = bookmarkRepository.findByUser(user);
        var responses = bookmarks.stream().map(b -> {
            Post post = b.getPost();
            return new PostResponse(
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
            );
        }).toList();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/suggestions")
    public ResponseEntity<?> getUserSuggestions() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        if (username == null || username.equals("anonymousUser")) {
            return ResponseEntity.status(401).body("Unauthorized");
        }
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        // Get IDs of users already followed (and self)
        List<Follow> following = followRepository.findByFollower(currentUser);
        var excludedIds = following.stream().map(f -> f.getFollowing().getId()).collect(Collectors.toSet());
        excludedIds.add(currentUser.getId());
        // Find users not followed
        List<User> allUsers = userRepository.findAll();
        List<User> suggestions = allUsers.stream()
                .filter(u -> !excludedIds.contains(u.getId()))
                .sorted((a, b) -> Integer.compare(
                        followRepository.findByFollowing(b).size(),
                        followRepository.findByFollowing(a).size()
                ))
                .limit(10)
                .collect(Collectors.toList());
        List<UserResponse> suggestionResponses = suggestions.stream()
                .map(u -> new UserResponse(
                        u.getId(),
                        u.getUsername(),
                        u.getBio(),
                        u.getLocation(),
                        u.getWebsite(),
                        u.getProfilePictureUrl(),
                        u.getInterests()
                ))
                .collect(Collectors.toList());
        return ResponseEntity.ok(suggestionResponses);
    }

    @GetMapping("/{id}/notification-preferences")
    public ResponseEntity<?> getNotificationPreferences(@PathVariable Long id) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        boolean isAdmin = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .map(a -> a.getAuthority()).anyMatch(role -> role.equals("ROLE_ADMIN"));
        if (!currentUser.getId().equals(id) && !isAdmin) {
            return ResponseEntity.status(403).body("Forbidden");
        }
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        var prefs = java.util.Map.of(
            "notifyLikes", user.isNotifyLikes(),
            "notifyComments", user.isNotifyComments(),
            "notifyFollows", user.isNotifyFollows()
        );
        return ResponseEntity.ok(prefs);
    }

    @PutMapping("/{id}/notification-preferences")
    public ResponseEntity<?> updateNotificationPreferences(@PathVariable Long id,
                                                          @RequestParam(required = false) Boolean notifyLikes,
                                                          @RequestParam(required = false) Boolean notifyComments,
                                                          @RequestParam(required = false) Boolean notifyFollows) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        boolean isAdmin = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .map(a -> a.getAuthority()).anyMatch(role -> role.equals("ROLE_ADMIN"));
        if (!currentUser.getId().equals(id) && !isAdmin) {
            return ResponseEntity.status(403).body("Forbidden");
        }
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (notifyLikes != null) user.setNotifyLikes(notifyLikes);
        if (notifyComments != null) user.setNotifyComments(notifyComments);
        if (notifyFollows != null) user.setNotifyFollows(notifyFollows);
        userRepository.save(user);
        return ResponseEntity.ok().body("Notification preferences updated");
    }

    @PutMapping("/{id}/change-password")
    public ResponseEntity<?> changePassword(@PathVariable Long id,
                                            @RequestParam String currentPassword,
                                            @RequestParam String newPassword) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        boolean isAdmin = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .map(a -> a.getAuthority()).anyMatch(role -> role.equals("ROLE_ADMIN"));
        if (!currentUser.getId().equals(id) && !isAdmin) {
            return ResponseEntity.status(403).body("Forbidden");
        }
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (!isAdmin && !passwordEncoder.matches(currentPassword, user.getPassword())) {
            return ResponseEntity.status(400).body("Current password is incorrect");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        return ResponseEntity.ok().body("Password changed successfully");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteAccount(@PathVariable Long id) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        boolean isAdmin = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .map(a -> a.getAuthority()).anyMatch(role -> role.equals("ROLE_ADMIN"));
        if (!currentUser.getId().equals(id) && !isAdmin) {
            return ResponseEntity.status(403).body("Forbidden");
        }
        userRepository.deleteById(id);
        return ResponseEntity.ok().body("Account deleted successfully");
    }

    @PostMapping("/{id}/report")
    public ResponseEntity<?> reportUser(@PathVariable Long id, @RequestParam String reason) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User reporter = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        User reportedUser = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Reported user not found"));
        Report report = new Report(reporter, reportedUser, null, reason);
        reportRepository.save(report);
        return ResponseEntity.ok().body("User reported successfully");
    }

    @PostMapping("/posts/{id}/report")
    public ResponseEntity<?> reportPost(@PathVariable Long id, @RequestParam String reason) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User reporter = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Post reportedPost = postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Reported post not found"));
        Report report = new Report(reporter, null, reportedPost, reason);
        reportRepository.save(report);
        return ResponseEntity.ok().body("Post reported successfully");
    }

    @PostMapping("/comments/{id}/report")
    public ResponseEntity<?> reportComment(@PathVariable Long id, @RequestParam String reason) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User reporter = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Comment reportedComment = commentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Reported comment not found"));
        Report report = new Report(reporter, null, null, reason);
        report.setReportedComment(reportedComment);
        reportRepository.save(report);
        return ResponseEntity.ok().body("Comment reported successfully");
    }

    @GetMapping("/reports")
    public ResponseEntity<?> getAllReports() {
        boolean isAdmin = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .map(a -> a.getAuthority()).anyMatch(role -> role.equals("ROLE_ADMIN"));
        if (!isAdmin) {
            return ResponseEntity.status(403).body("Forbidden");
        }
        return ResponseEntity.ok(reportRepository.findAll());
    }

    @PutMapping("/reports/{reportId}/status")
    public ResponseEntity<?> updateReportStatus(@PathVariable Long reportId, @RequestParam String status) {
        boolean isAdmin = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .map(a -> a.getAuthority()).anyMatch(role -> role.equals("ROLE_ADMIN"));
        if (!isAdmin) {
            return ResponseEntity.status(403).body("Forbidden");
        }
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Report not found"));
        report.setStatus(status);
        reportRepository.save(report);
        return ResponseEntity.ok().body("Report status updated");
    }

    @PutMapping("/reports/{reportId}/resolve")
    public ResponseEntity<?> resolveReport(@PathVariable Long reportId) {
        boolean isAdmin = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .map(a -> a.getAuthority()).anyMatch(role -> role.equals("ROLE_ADMIN"));
        if (!isAdmin) {
            return ResponseEntity.status(403).body("Forbidden");
        }
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Report not found"));
        report.setStatus("resolved");
        reportRepository.save(report);
        // Optionally, notify reporter
        // notification/email logic here
        return ResponseEntity.ok().body("Report resolved");
    }

    @PutMapping("/reports/{reportId}/escalate")
    public ResponseEntity<?> escalateReport(@PathVariable Long reportId) {
        boolean isAdmin = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .map(a -> a.getAuthority()).anyMatch(role -> role.equals("ROLE_ADMIN"));
        if (!isAdmin) {
            return ResponseEntity.status(403).body("Forbidden");
        }
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Report not found"));
        report.setStatus("escalated");
        reportRepository.save(report);
        // Optionally, notify reporter
        // notification/email logic here
        return ResponseEntity.ok().body("Report escalated");
    }

    @GetMapping("/reports/filter")
    public ResponseEntity<?> filterReports(@RequestParam(required = false) String status,
                                           @RequestParam(required = false) String type) {
        boolean isAdmin = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .map(a -> a.getAuthority()).anyMatch(role -> role.equals("ROLE_ADMIN"));
        if (!isAdmin) {
            return ResponseEntity.status(403).body("Forbidden");
        }
        if (status != null && type != null) {
            // Custom query logic for both status and type
            return ResponseEntity.ok(reportRepository.findByStatusAndType(status, type));
        } else if (status != null) {
            return ResponseEntity.ok(reportRepository.findByStatus(status));
        } else if (type != null) {
            return ResponseEntity.ok(reportRepository.findByType(type));
        } else {
            return ResponseEntity.ok(reportRepository.findAll());
        }
    }

    @PutMapping("/{id}/ban")
    public ResponseEntity<?> banUser(@PathVariable Long id) {
        boolean isAdmin = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .map(a -> a.getAuthority()).anyMatch(role -> role.equals("ROLE_ADMIN"));
        if (!isAdmin) {
            return ResponseEntity.status(403).body("Forbidden");
        }
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setStatus("banned");
        userRepository.save(user);
        return ResponseEntity.ok().body("User banned");
    }

    @PutMapping("/{id}/unban")
    public ResponseEntity<?> unbanUser(@PathVariable Long id) {
        boolean isAdmin = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .map(a -> a.getAuthority()).anyMatch(role -> role.equals("ROLE_ADMIN"));
        if (!isAdmin) {
            return ResponseEntity.status(403).body("Forbidden");
        }
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setStatus("active");
        userRepository.save(user);
        return ResponseEntity.ok().body("User unbanned");
    }

    @PostMapping("/{id}/warn")
    public ResponseEntity<?> warnUser(@PathVariable Long id, @RequestParam String message) {
        boolean isAdmin = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .map(a -> a.getAuthority()).anyMatch(role -> role.equals("ROLE_ADMIN"));
        if (!isAdmin) {
            return ResponseEntity.status(403).body("Forbidden");
        }
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setStatus("warned");
        userRepository.save(user);
        // Optionally, store or send the warning message (not persisted here)
        return ResponseEntity.ok().body("User warned: " + message);
    }

    @DeleteMapping("/posts/{id}/admin")
    public ResponseEntity<?> adminDeletePost(@PathVariable Long id) {
        boolean isAdmin = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .map(a -> a.getAuthority()).anyMatch(role -> role.equals("ROLE_ADMIN"));
        if (!isAdmin) {
            return ResponseEntity.status(403).body("Forbidden");
        }
        postRepository.deleteById(id);
        return ResponseEntity.ok().body("Post deleted by admin");
    }

    @DeleteMapping("/comments/{id}/admin")
    public ResponseEntity<?> adminDeleteComment(@PathVariable Long id) {
        boolean isAdmin = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .map(a -> a.getAuthority()).anyMatch(role -> role.equals("ROLE_ADMIN"));
        if (!isAdmin) {
            return ResponseEntity.status(403).body("Forbidden");
        }
        commentRepository.deleteById(id);
        return ResponseEntity.ok().body("Comment deleted by admin");
    }

    @GetMapping("/analytics/user/{userId}")
    public ResponseEntity<UserAnalyticsResponse> getUserAnalytics(@PathVariable Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        int postCount = postRepository.findByUserId(userId).size();
        // Count comments made by user (not available directly, so filter all comments)
        int commentCount = (int) commentRepository.findAll().stream().filter(c -> c.getUser().getId().equals(userId)).count();
        // Count likes received on user's posts
        int likeCount = postRepository.findByUserId(userId).stream().mapToInt(p -> (int) likeRepository.countByPost(p)).sum();
        int followerCount = followRepository.findByFollowing(user).size();
        int badgeCount = userBadgeRepository.findByUser(user).size();
        UserAnalyticsResponse response = new UserAnalyticsResponse(postCount, commentCount, likeCount, followerCount, badgeCount);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/admin/analytics")
    public ResponseEntity<?> getPlatformAnalytics() {
        boolean isAdmin = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .map(a -> a.getAuthority()).anyMatch(role -> role.equals("ROLE_ADMIN"));
        if (!isAdmin) {
            return ResponseEntity.status(403).body("Forbidden");
        }
        int totalUsers = (int) userRepository.count();
        int totalPosts = (int) postRepository.count();
        int totalComments = (int) commentRepository.count();
        int totalLikes = (int) likeRepository.count();
        // Most active users (by post count)
        var users = userRepository.findAll();
        var mostActiveUsers = users.stream()
            .sorted((a, b) -> Integer.compare(
                postRepository.findByUserId(b.getId()).size(),
                postRepository.findByUserId(a.getId()).size()
            ))
            .limit(5)
            .map(u -> new AdminAnalyticsResponse.UserActivity(u.getId(), u.getUsername(), postRepository.findByUserId(u.getId()).size()))
            .toList();
        // Most popular posts (by like count)
        var posts = postRepository.findAll();
        var mostPopularPosts = posts.stream()
            .sorted((a, b) -> Long.compare(
                likeRepository.countByPostId(b.getId()),
                likeRepository.countByPostId(a.getId())
            ))
            .limit(5)
            .map(p -> new AdminAnalyticsResponse.PostPopularity(p.getId(), p.getTitle(), (int) likeRepository.countByPostId(p.getId())))
            .toList();
        // Trending hashtags from latest 100 posts
        var recentPosts = postRepository.findAllByOrderByCreatedAtDesc(org.springframework.data.domain.PageRequest.of(0, 100));
        java.util.regex.Pattern hashtagPattern = java.util.regex.Pattern.compile("#\\w+");
        java.util.Map<String, Integer> hashtagCounts = new java.util.HashMap<>();
        for (var post : recentPosts) {
            java.util.regex.Matcher matcher = hashtagPattern.matcher(post.getContent());
            while (matcher.find()) {
                String hashtag = matcher.group().toLowerCase();
                hashtagCounts.put(hashtag, hashtagCounts.getOrDefault(hashtag, 0) + 1);
            }
        }
        var trendingHashtags = hashtagCounts.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .limit(10)
                .map(java.util.Map.Entry::getKey)
                .toList();
        // User and post growth over last 30 days
        java.time.LocalDate today = java.time.LocalDate.now();
        java.util.Map<String, Integer> userGrowth = new java.util.HashMap<>();
        java.util.Map<String, Integer> postGrowth = new java.util.HashMap<>();
        for (int i = 29; i >= 0; i--) {
            java.time.LocalDate day = today.minusDays(i);
            String key = day.toString();
            int usersOnDay = (int) users.stream().filter(u -> {
                try {
                    var field = u.getClass().getDeclaredField("createdAt");
                    field.setAccessible(true);
                    java.time.LocalDateTime createdAt = (java.time.LocalDateTime) field.get(u);
                    return createdAt != null && createdAt.toLocalDate().equals(day);
                } catch (Exception e) { return false; }
            }).count();
            int postsOnDay = (int) posts.stream().filter(p -> p.getCreatedAt() != null && p.getCreatedAt().toLocalDate().equals(day)).count();
            userGrowth.put(key, usersOnDay);
            postGrowth.put(key, postsOnDay);
        }
        // Engagement rate: (total likes + total comments) / total posts
        double engagementRate = totalPosts > 0 ? ((double) (totalLikes + totalComments)) / totalPosts : 0.0;
        // Daily engagement rate for last 30 days
        java.util.Map<String, Double> dailyEngagementRate = new java.util.HashMap<>();
        for (int i = 29; i >= 0; i--) {
            java.time.LocalDate day = today.minusDays(i);
            String key = day.toString();
            int postsOnDay = (int) posts.stream().filter(p -> p.getCreatedAt() != null && p.getCreatedAt().toLocalDate().equals(day)).count();
            int likesOnDay = (int) likeRepository.findAll().stream().filter(l -> l.getCreatedAt() != null && l.getCreatedAt().toLocalDate().equals(day)).count();
            int commentsOnDay = (int) commentRepository.findAll().stream().filter(c -> c.getCreatedAt() != null && c.getCreatedAt().toLocalDate().equals(day)).count();
            double rate = postsOnDay > 0 ? ((double) (likesOnDay + commentsOnDay)) / postsOnDay : 0.0;
            dailyEngagementRate.put(key, rate);
        }
        AdminAnalyticsResponse analytics = new AdminAnalyticsResponse();
        analytics.setTotalUsers(totalUsers);
        analytics.setTotalPosts(totalPosts);
        analytics.setTotalComments(totalComments);
        analytics.setTotalLikes(totalLikes);
        analytics.setMostActiveUsers(mostActiveUsers);
        analytics.setMostPopularPosts(mostPopularPosts);
        analytics.setTrendingHashtags(trendingHashtags);
        analytics.setUserGrowthLast30Days(userGrowth);
        analytics.setPostGrowthLast30Days(postGrowth);
        analytics.setEngagementRate(engagementRate);
        analytics.setDailyEngagementRateLast30Days(dailyEngagementRate);
        return ResponseEntity.ok(analytics);
    }

    @GetMapping("/admin/analytics/activity")
    public ResponseEntity<?> getActivityAnalytics(@RequestParam(defaultValue = "daily") String period) {
        // Example: count users, posts, comments, likes in the last day/week/month
        java.time.LocalDateTime since = switch (period) {
            case "weekly" -> java.time.LocalDateTime.now().minusWeeks(1);
            case "monthly" -> java.time.LocalDateTime.now().minusMonths(1);
            default -> java.time.LocalDateTime.now().minusDays(1);
        };
        int users = (int) userRepository.findAll().stream().filter(u -> u.getCreatedAt() != null && u.getCreatedAt().isAfter(since)).count();
        int posts = (int) postRepository.findAll().stream().filter(p -> p.getCreatedAt() != null && p.getCreatedAt().isAfter(since)).count();
        int comments = (int) commentRepository.findAll().stream().filter(c -> c.getCreatedAt() != null && c.getCreatedAt().isAfter(since)).count();
        int likes = (int) likeRepository.findAll().stream().filter(l -> l.getCreatedAt() != null && l.getCreatedAt().isAfter(since)).count();
        return ResponseEntity.ok(java.util.Map.of("users", users, "posts", posts, "comments", comments, "likes", likes));
    }

    @GetMapping("/admin/analytics/growth")
    public ResponseEntity<?> getGrowthAnalytics(@RequestParam String type, @RequestParam(defaultValue = "daily") String period) {
        // Example: return growth data for posts/comments/likes over time
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        java.util.List<java.util.Map<String, Object>> data = new java.util.ArrayList<>();
        int intervals = switch (period) { case "monthly" -> 12; case "weekly" -> 12; default -> 30; };
        for (int i = intervals - 1; i >= 0; i--) {
            java.time.LocalDateTime start, end;
            if (period.equals("monthly")) {
                start = now.minusMonths(i + 1).withDayOfMonth(1);
                end = now.minusMonths(i).withDayOfMonth(1);
            } else if (period.equals("weekly")) {
                start = now.minusWeeks(i + 1).with(java.time.DayOfWeek.MONDAY);
                end = now.minusWeeks(i).with(java.time.DayOfWeek.MONDAY);
            } else {
                start = now.minusDays(i + 1).withHour(0).withMinute(0).withSecond(0).withNano(0);
                end = now.minusDays(i).withHour(0).withMinute(0).withSecond(0).withNano(0);
            }
            int count = switch (type) {
                case "post" -> (int) postRepository.findAll().stream().filter(p -> p.getCreatedAt() != null && !p.getCreatedAt().isBefore(start) && p.getCreatedAt().isBefore(end)).count();
                case "comment" -> (int) commentRepository.findAll().stream().filter(c -> c.getCreatedAt() != null && !c.getCreatedAt().isBefore(start) && c.getCreatedAt().isBefore(end)).count();
                case "like" -> (int) likeRepository.findAll().stream().filter(l -> l.getCreatedAt() != null && !l.getCreatedAt().isBefore(start) && l.getCreatedAt().isBefore(end)).count();
                default -> 0;
            };
            data.add(java.util.Map.of("start", start.toString(), "end", end.toString(), "count", count));
        }
        return ResponseEntity.ok(data);
    }

    @GetMapping("/admin/analytics/top")
    public ResponseEntity<?> getTopAnalytics(@RequestParam String type, @RequestParam(defaultValue = "daily") String period) {
        java.time.LocalDateTime since = switch (period) {
            case "weekly" -> java.time.LocalDateTime.now().minusWeeks(1);
            case "monthly" -> java.time.LocalDateTime.now().minusMonths(1);
            default -> java.time.LocalDateTime.now().minusDays(1);
        };
        if (type.equals("user")) {
            var users = userRepository.findAll().stream()
                .sorted((a, b) -> Integer.compare(
                    postRepository.findByUserId(b.getId()).stream().filter(p -> p.getCreatedAt() != null && p.getCreatedAt().isAfter(since)).toList().size(),
                    postRepository.findByUserId(a.getId()).stream().filter(p -> p.getCreatedAt() != null && p.getCreatedAt().isAfter(since)).toList().size()
                ))
                .limit(10)
                .map(u -> java.util.Map.of("id", u.getId(), "username", u.getUsername(), "postCount", postRepository.findByUserId(u.getId()).stream().filter(p -> p.getCreatedAt() != null && p.getCreatedAt().isAfter(since)).toList().size()))
                .toList();
            return ResponseEntity.ok(users);
        } else if (type.equals("post")) {
            var posts = postRepository.findAll().stream()
                .filter(p -> p.getCreatedAt() != null && p.getCreatedAt().isAfter(since))
                .sorted((a, b) -> Long.compare(likeRepository.countByPostId(b.getId()), likeRepository.countByPostId(a.getId())))
                .limit(10)
                .map(p -> java.util.Map.of("id", p.getId(), "title", p.getTitle(), "likeCount", likeRepository.countByPostId(p.getId())))
                .toList();
            return ResponseEntity.ok(posts);
        } else if (type.equals("hashtag")) {
            var posts = postRepository.findAll().stream().filter(p -> p.getCreatedAt() != null && p.getCreatedAt().isAfter(since)).toList();
            java.util.Map<String, Integer> hashtagCounts = new java.util.HashMap<>();
            java.util.regex.Pattern hashtagPattern = java.util.regex.Pattern.compile("#\\w+");
            for (var post : posts) {
                java.util.regex.Matcher matcher = hashtagPattern.matcher(post.getContent());
                while (matcher.find()) {
                    String hashtag = matcher.group().toLowerCase();
                    hashtagCounts.put(hashtag, hashtagCounts.getOrDefault(hashtag, 0) + 1);
                }
            }
            var trending = hashtagCounts.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .limit(10)
                .map(e -> java.util.Map.of("name", e.getKey(), "count", e.getValue()))
                .toList();
            return ResponseEntity.ok(trending);
        } else {
            return ResponseEntity.badRequest().body("Invalid type");
        }
    }

    @GetMapping("/admin/analytics/export")
    public ResponseEntity<?> exportAnalytics() {
        // Export all analytics as JSON (could be extended to CSV)
        var analytics = java.util.Map.of(
            "users", userRepository.count(),
            "posts", postRepository.count(),
            "comments", commentRepository.count(),
            "likes", likeRepository.count()
        );
        return ResponseEntity.ok(analytics);
    }
}

record AuthenticationRequest(String username, String password) {}
record AuthenticationResponse(String jwtToken) {}
record UpdateProfilePictureRequest(
    @NotBlank(message = "Profile picture URL cannot be blank") String profilePictureUrl
) {}
record ProfilePictureResponse(String profilePictureUrl) {}