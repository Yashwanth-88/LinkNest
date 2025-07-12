package com.linknest.linknest.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.apache.commons.text.similarity.LevenshteinDistance;

import com.linknest.linknest.entity.Post;
import com.linknest.linknest.entity.User;
import com.linknest.linknest.entity.Hashtag;
import com.linknest.linknest.repository.PostRepository;
import com.linknest.linknest.repository.HashtagRepository;
import com.linknest.linknest.repository.UserRepository;
import com.linknest.linknest.repository.FollowRepository;
import com.linknest.linknest.repository.LikeRepository;
import com.linknest.linknest.repository.CommentRepository;
import com.linknest.linknest.repository.MutedUserRepository;
import com.linknest.linknest.repository.MutedHashtagRepository;
import com.linknest.linknest.repository.UserBlockRepository;

@RestController
@RequestMapping("/api/feed")
public class FeedController {

    @Autowired
    private PostRepository postRepository;
    @Autowired
    private HashtagRepository hashtagRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private FollowRepository followRepository;
    @Autowired
    private LikeRepository likeRepository;
    @Autowired
    private CommentRepository commentRepository;
    @Autowired
    private MutedUserRepository mutedUserRepository;
    @Autowired
    private MutedHashtagRepository mutedHashtagRepository;
    @Autowired
    private UserBlockRepository userBlockRepository;

    // In-memory 'not interested' sets with timestamps for expiration (should be persisted in production)
    private static final java.util.Map<Long, java.util.Map<Long, Long>> notInterestedPosts = new java.util.concurrent.ConcurrentHashMap<>();
    private static final java.util.Map<Long, java.util.Map<Long, Long>> notInterestedUsers = new java.util.concurrent.ConcurrentHashMap<>();
    private static final java.util.Map<Long, java.util.Map<String, Long>> notInterestedHashtags = new java.util.concurrent.ConcurrentHashMap<>();
    private static final long NOT_INTERESTED_EXPIRY_MILLIS = 30L * 24 * 60 * 60 * 1000; // 30 days

    private void cleanupExpiredNotInterested() {
        long now = System.currentTimeMillis();
        notInterestedPosts.values().forEach(map -> map.entrySet().removeIf(e -> now - e.getValue() > NOT_INTERESTED_EXPIRY_MILLIS));
        notInterestedUsers.values().forEach(map -> map.entrySet().removeIf(e -> now - e.getValue() > NOT_INTERESTED_EXPIRY_MILLIS));
        notInterestedHashtags.values().forEach(map -> map.entrySet().removeIf(e -> now - e.getValue() > NOT_INTERESTED_EXPIRY_MILLIS));
    }

    @PostMapping("/not-interested/post/{postId}")
    public ResponseEntity<?> notInterestedPost(@PathVariable Long postId) {
        String username = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) return ResponseEntity.status(401).body("Unauthorized");
        notInterestedPosts.computeIfAbsent(user.getId(), k -> new java.util.HashMap<>()).put(postId, System.currentTimeMillis());
        return ResponseEntity.ok("Post marked as not interested");
    }

    @PostMapping("/not-interested/user/{userId}")
    public ResponseEntity<?> notInterestedUser(@PathVariable Long userId) {
        String username = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) return ResponseEntity.status(401).body("Unauthorized");
        notInterestedUsers.computeIfAbsent(user.getId(), k -> new java.util.HashMap<>()).put(userId, System.currentTimeMillis());
        return ResponseEntity.ok("User marked as not interested");
    }

    @PostMapping("/not-interested/hashtag")
    public ResponseEntity<?> notInterestedHashtag(@RequestParam String hashtag) {
        String username = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) return ResponseEntity.status(401).body("Unauthorized");
        notInterestedHashtags.computeIfAbsent(user.getId(), k -> new java.util.HashMap<>()).put(hashtag.toLowerCase(), System.currentTimeMillis());
        return ResponseEntity.ok("Hashtag marked as not interested");
    }

    @GetMapping
    public ResponseEntity<Page<Post>> getFeed(@RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "10") int size) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        System.out.println("Fetching feed for user: " + username);
        Page<Post> feed = postRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(page, size));
        System.out.println("Returned " + feed.getNumberOfElements() + " posts");
        return ResponseEntity.ok(feed);
    }

    @GetMapping("/search")
    public ResponseEntity<Page<Post>> searchPosts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String hashtag,
            @RequestParam(required = false) Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<Post> result;
        if (userId != null && keyword != null) {
            result = postRepository.searchByUserIdInAndKeyword(java.util.List.of(userId), keyword, PageRequest.of(page, size));
        } else if (userId != null && hashtag != null) {
            result = postRepository.searchByUserIdInAndHashtag(java.util.List.of(userId), hashtag, PageRequest.of(page, size));
        } else if (keyword != null) {
            result = postRepository.fuzzySearchByKeywordRanked(keyword, PageRequest.of(page, size));
        } else if (hashtag != null) {
            result = postRepository.searchByUserIdInAndHashtag(
                userRepository.findAll().stream().map(User::getId).toList(), hashtag, PageRequest.of(page, size));
        } else if (userId != null) {
            result = postRepository.findByUserId(userId, PageRequest.of(page, size));
        } else {
            result = postRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(page, size));
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/autocomplete/hashtags")
    public ResponseEntity<java.util.List<String>> autocompleteHashtags(@RequestParam String prefix) {
        var hashtags = hashtagRepository.findTop10ByNameStartingWithIgnoreCase(prefix);
        // Add typo-tolerance: get all hashtags, rank by Levenshtein distance, then by popularity
        var allHashtags = hashtagRepository.findAll();
        LevenshteinDistance ld = new LevenshteinDistance();
        var sorted = allHashtags.stream()
            .sorted((a, b) -> {
                int distA = ld.apply(prefix.toLowerCase(), a.getName().toLowerCase());
                int distB = ld.apply(prefix.toLowerCase(), b.getName().toLowerCase());
                if (distA != distB) return Integer.compare(distA, distB);
                return Integer.compare(b.getPosts().size(), a.getPosts().size());
            })
            .limit(10)
            .map(Hashtag::getName)
            .toList();
        return ResponseEntity.ok(sorted);
    }

    @GetMapping("/autocomplete/users")
    public ResponseEntity<java.util.List<String>> autocompleteUsers(@RequestParam String prefix) {
        var users = userRepository.findTop10ByUsernameStartingWithIgnoreCase(prefix);
        // Add typo-tolerance: get all users, rank by Levenshtein distance, then by follower count
        var allUsers = userRepository.findAll();
        LevenshteinDistance ld = new LevenshteinDistance();
        var sorted = allUsers.stream()
            .sorted((a, b) -> {
                int distA = ld.apply(prefix.toLowerCase(), a.getUsername().toLowerCase());
                int distB = ld.apply(prefix.toLowerCase(), b.getUsername().toLowerCase());
                if (distA != distB) return Integer.compare(distA, distB);
                return Integer.compare(
                    followRepository.findByFollowing(b).size(),
                    followRepository.findByFollowing(a).size()
                );
            })
            .limit(10)
            .map(User::getUsername)
            .toList();
        return ResponseEntity.ok(sorted);
    }

    @GetMapping("/for-you")
    public ResponseEntity<?> getForYouFeed(@RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "10") int size) {
        cleanupExpiredNotInterested();
        String username = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username).orElse(null);
        java.util.Set<String> interests = user != null ? user.getInterests() : java.util.Set.of();
        java.util.Set<Long> mutedUserIds = user != null ? mutedUserRepository.findByUser(user).stream().map(mu -> mu.getMutedUser().getId()).collect(java.util.stream.Collectors.toSet()) : java.util.Set.of();
        java.util.Set<String> mutedHashtags = user != null ? mutedHashtagRepository.findByUser(user).stream().map(mh -> mh.getHashtag().getName().toLowerCase()).collect(java.util.stream.Collectors.toSet()) : java.util.Set.of();
        java.util.Set<Long> likedPostIds = user != null ? likeRepository.findByUser(user).stream().map(l -> l.getPost() != null ? l.getPost().getId() : null).filter(java.util.Objects::nonNull).collect(java.util.stream.Collectors.toSet()) : java.util.Set.of();
        java.util.Set<Long> commentedPostIds = user != null ? commentRepository.findByUser(user).stream().map(c -> c.getPost() != null ? c.getPost().getId() : null).filter(java.util.Objects::nonNull).collect(java.util.stream.Collectors.toSet()) : java.util.Set.of();
        java.util.Set<Long> notInterested = user != null ? notInterestedPosts.getOrDefault(user.getId(), java.util.Map.of()).keySet() : java.util.Set.of();
        // Gather candidate posts: recent, popular, and from new users
        var allPosts = postRepository.findAll();
        var recentPosts = allPosts.stream().filter(p -> p.getCreatedAt() != null && p.getCreatedAt().isAfter(java.time.LocalDateTime.now().minusDays(2))).toList();
        var popularPosts = allPosts.stream().sorted((a, b) -> Long.compare(likeRepository.countByPostId(b.getId()), likeRepository.countByPostId(a.getId()))).limit(50).toList();
        var newUserPosts = allPosts.stream().filter(p -> p.getUser() != null && p.getUser().getCreatedAt() != null && p.getUser().getCreatedAt().isAfter(java.time.LocalDateTime.now().minusWeeks(2))).limit(50).toList();
        java.util.List<Post> candidates = new java.util.ArrayList<>();
        candidates.addAll(recentPosts);
        candidates.addAll(popularPosts);
        candidates.addAll(newUserPosts);
        // Remove duplicates
        java.util.Set<Long> seen = new java.util.HashSet<>();
        candidates = candidates.stream().filter(p -> seen.add(p.getId())).toList();
        // Filter: not muted, not already liked/commented, not too many from same user, matches interests or is popular, not 'not interested'
        java.util.Map<Long, Integer> userPostCounts = new java.util.HashMap<>();
        java.util.List<Post> filtered = new java.util.ArrayList<>();
        for (Post p : candidates) {
            if (mutedUserIds.contains(p.getUser().getId())) continue;
            if (p.getHashtags() != null && p.getHashtags().stream().anyMatch(h -> mutedHashtags.contains(h.getName().toLowerCase()))) continue;
            if (likedPostIds.contains(p.getId()) || commentedPostIds.contains(p.getId())) continue;
            if (notInterested.contains(p.getId())) continue;
            int count = userPostCounts.getOrDefault(p.getUser().getId(), 0);
            if (count >= 2) continue; // Max 2 posts per user
            if (!interests.isEmpty() && p.getHashtags() != null && p.getHashtags().stream().noneMatch(h -> interests.contains(h.getName().replace("#", "")))) {
                // If not matching interests, only include if very popular
                if (likeRepository.countByPostId(p.getId()) < 5) continue;
            }
            filtered.add(p);
            userPostCounts.put(p.getUser().getId(), count + 1);
        }
        // Pagination
        int from = Math.min(page * size, filtered.size());
        int to = Math.min(from + size, filtered.size());
        return ResponseEntity.ok(filtered.subList(from, to));
    }

    @GetMapping("/following")
    public ResponseEntity<?> getFollowingFeed(@RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "10") int size) {
        String username = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) return ResponseEntity.status(401).body("Unauthorized");
        var followingIds = followRepository.findByFollower(user).stream().map(f -> f.getFollowing().getId()).toList();
        var posts = postRepository.findByUserIdInOrderByCreatedAtDesc(followingIds, org.springframework.data.domain.PageRequest.of(page, size));
        return ResponseEntity.ok(posts.getContent());
    }

    @GetMapping("/trending-posts")
    public ResponseEntity<?> getTrendingPosts(@RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "10") int size) {
        String username = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username).orElse(null);
        java.util.Set<Long> mutedUserIds = user != null ? mutedUserRepository.findByUser(user).stream().map(mu -> mu.getMutedUser().getId()).collect(java.util.stream.Collectors.toSet()) : java.util.Set.of();
        java.util.Set<String> mutedHashtags = user != null ? mutedHashtagRepository.findByUser(user).stream().map(mh -> mh.getHashtag().getName().toLowerCase()).collect(java.util.stream.Collectors.toSet()) : java.util.Set.of();
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        var posts = postRepository.findAll().stream()
            .filter(p -> p.getCreatedAt() != null && p.getCreatedAt().isAfter(now.minusDays(3)))
            .filter(p -> !mutedUserIds.contains(p.getUser().getId()))
            .filter(p -> p.getHashtags() == null || p.getHashtags().stream().noneMatch(h -> mutedHashtags.contains(h.getName().toLowerCase())))
            .sorted((a, b) -> {
                double scoreA = trendingScore(a, now);
                double scoreB = trendingScore(b, now);
                return Double.compare(scoreB, scoreA);
            })
            .skip(page * size)
            .limit(size)
            .toList();
        return ResponseEntity.ok(posts);
    }

    // Helper for trending score with time decay and new user boost
    private double trendingScore(Post post, java.time.LocalDateTime now) {
        long likeCount = likeRepository.countByPostId(post.getId());
        long commentCount = commentRepository.findAll().stream().filter(c -> c.getPost().getId().equals(post.getId())).count();
        long followerCount = post.getUser() != null && post.getUser().getId() != null ? followRepository.findByFollowing(post.getUser()).size() : 0;
        long hoursSince = java.time.Duration.between(post.getCreatedAt(), now).toHours();
        double decay = Math.exp(-0.1 * hoursSince); // Exponential decay
        double engagement = (likeCount * 2 + commentCount) * decay;
        double newUserBoost = (followerCount < 10) ? 5.0 : 1.0;
        return engagement * newUserBoost;
    }

    @GetMapping("/suggested-users")
    public ResponseEntity<?> getSuggestedUsers(@RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "10") int size) {
        cleanupExpiredNotInterested();
        String username = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) return ResponseEntity.status(401).body("Unauthorized");
        var followingIds = followRepository.findByFollower(user).stream().map(f -> f.getFollowing().getId()).toList();
        var mutedUserIds = mutedUserRepository.findByUser(user).stream().map(mu -> mu.getMutedUser().getId()).collect(java.util.stream.Collectors.toSet());
        var blockedUserIds = userBlockRepository.findByBlocker(user).stream().map(b -> b.getBlocked().getId()).collect(java.util.stream.Collectors.toSet());
        var userInterests = user.getInterests();
        var notInterested = user != null ? notInterestedUsers.getOrDefault(user.getId(), java.util.Map.of()).keySet() : java.util.Set.of();
        var allUsers = userRepository.findAll();
        var suggestions = allUsers.stream()
            .filter(u -> !u.getId().equals(user.getId()) && !followingIds.contains(u.getId()) && !mutedUserIds.contains(u.getId()) && !blockedUserIds.contains(u.getId()) && !notInterested.contains(u.getId()))
            .sorted((a, b) -> {
                // Prioritize mutual followers and similar interests
                long mutualA = followRepository.findByFollower(a).stream().map(f -> f.getFollowing().getId()).filter(followingIds::contains).count();
                long mutualB = followRepository.findByFollower(b).stream().map(f -> f.getFollowing().getId()).filter(followingIds::contains).count();
                long interestA = a.getInterests() != null ? a.getInterests().stream().filter(userInterests::contains).count() : 0;
                long interestB = b.getInterests() != null ? b.getInterests().stream().filter(userInterests::contains).count() : 0;
                if (mutualA != mutualB) return Long.compare(mutualB, mutualA);
                return Long.compare(interestB, interestA);
            })
            .limit(100)
            .toList();
        int from = Math.min(page * size, suggestions.size());
        int to = Math.min(from + size, suggestions.size());
        return ResponseEntity.ok(suggestions.subList(from, to));
    }

    @GetMapping("/suggested-hashtags")
    public ResponseEntity<?> getSuggestedHashtags(@RequestParam(defaultValue = "0") int page,
                                                  @RequestParam(defaultValue = "10") int size) {
        cleanupExpiredNotInterested();
        String username = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username).orElse(null);
        java.util.Set<String> interests = user != null ? user.getInterests() : java.util.Set.of();
        var mutedHashtags = user != null ? mutedHashtagRepository.findByUser(user).stream().map(mh -> mh.getHashtag().getName().toLowerCase()).collect(java.util.stream.Collectors.toSet()) : java.util.Set.of();
        var notInterested = user != null ? notInterestedHashtags.getOrDefault(user.getId(), java.util.Map.of()).keySet() : java.util.Set.of();
        var followingIds = user != null ? new java.util.ArrayList<>(followRepository.findByFollower(user).stream().map(f -> f.getFollowing().getId()).toList()) : new java.util.ArrayList<Long>();
        java.util.List<Post> postsByFollowing = followingIds.isEmpty() ? java.util.List.of() : postRepository.findByUserIdIn(followingIds);
        java.util.Map<String, Integer> hashtagCounts = new java.util.HashMap<>();
        java.util.regex.Pattern hashtagPattern = java.util.regex.Pattern.compile("#\\w+");
        for (var post : postsByFollowing) {
            java.util.regex.Matcher matcher = hashtagPattern.matcher(post.getContent());
            while (matcher.find()) {
                String hashtag = matcher.group().toLowerCase();
                if (!mutedHashtags.contains(hashtag) && !interests.contains(hashtag.replace("#", "")) && !notInterested.contains(hashtag)) {
                    hashtagCounts.put(hashtag, hashtagCounts.getOrDefault(hashtag, 0) + 1);
                }
            }
        }
        var suggestions = hashtagCounts.entrySet().stream()
            .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
            .map(java.util.Map.Entry::getKey)
            .limit(100)
            .toList();
        int from = Math.min(page * size, suggestions.size());
        int to = Math.min(from + size, suggestions.size());
        return ResponseEntity.ok(suggestions.subList(from, to));
    }

    @GetMapping("/not-interested/posts")
    public ResponseEntity<?> getNotInterestedPosts() {
        cleanupExpiredNotInterested();
        String username = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) return ResponseEntity.status(401).body("Unauthorized");
        return ResponseEntity.ok(notInterestedPosts.getOrDefault(user.getId(), java.util.Map.of()).keySet());
    }

    @GetMapping("/not-interested/users")
    public ResponseEntity<?> getNotInterestedUsers() {
        cleanupExpiredNotInterested();
        String username = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) return ResponseEntity.status(401).body("Unauthorized");
        return ResponseEntity.ok(notInterestedUsers.getOrDefault(user.getId(), java.util.Map.of()).keySet());
    }

    @GetMapping("/not-interested/hashtags")
    public ResponseEntity<?> getNotInterestedHashtags() {
        cleanupExpiredNotInterested();
        String username = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) return ResponseEntity.status(401).body("Unauthorized");
        return ResponseEntity.ok(notInterestedHashtags.getOrDefault(user.getId(), java.util.Map.of()).keySet());
    }

    @DeleteMapping("/not-interested/posts/reset")
    public ResponseEntity<?> resetNotInterestedPosts() {
        String username = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) return ResponseEntity.status(401).body("Unauthorized");
        notInterestedPosts.remove(user.getId());
        return ResponseEntity.ok("Not interested posts reset");
    }

    @DeleteMapping("/not-interested/users/reset")
    public ResponseEntity<?> resetNotInterestedUsers() {
        String username = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) return ResponseEntity.status(401).body("Unauthorized");
        notInterestedUsers.remove(user.getId());
        return ResponseEntity.ok("Not interested users reset");
    }

    @DeleteMapping("/not-interested/hashtags/reset")
    public ResponseEntity<?> resetNotInterestedHashtags() {
        String username = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) return ResponseEntity.status(401).body("Unauthorized");
        notInterestedHashtags.remove(user.getId());
        return ResponseEntity.ok("Not interested hashtags reset");
    }

    @DeleteMapping("/not-interested/post/{postId}")
    public ResponseEntity<?> removeNotInterestedPost(@PathVariable Long postId) {
        cleanupExpiredNotInterested();
        String username = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) return ResponseEntity.status(401).body("Unauthorized");
        notInterestedPosts.computeIfPresent(user.getId(), (k, map) -> { map.remove(postId); return map; });
        return ResponseEntity.ok("Post removed from not interested");
    }

    @DeleteMapping("/not-interested/user/{userId}")
    public ResponseEntity<?> removeNotInterestedUser(@PathVariable Long userId) {
        cleanupExpiredNotInterested();
        String username = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) return ResponseEntity.status(401).body("Unauthorized");
        notInterestedUsers.computeIfPresent(user.getId(), (k, map) -> { map.remove(userId); return map; });
        return ResponseEntity.ok("User removed from not interested");
    }

    @DeleteMapping("/not-interested/hashtag")
    public ResponseEntity<?> removeNotInterestedHashtag(@RequestParam String hashtag) {
        cleanupExpiredNotInterested();
        String username = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) return ResponseEntity.status(401).body("Unauthorized");
        notInterestedHashtags.computeIfPresent(user.getId(), (k, map) -> { map.remove(hashtag.toLowerCase()); return map; });
        return ResponseEntity.ok("Hashtag removed from not interested");
    }
}