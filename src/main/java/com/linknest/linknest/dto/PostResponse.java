package com.linknest.linknest.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public class PostResponse {
    private Long id;
    private String title;
    private String content;
    private String mediaUrl;
    private LocalDateTime createdAt;
    private UserResponse user;
    private Long likeCount;

    public PostResponse(Long id, String content, LocalDateTime createdAt, UserResponse userResponse, Long likeCount) {}

    public PostResponse(Long id, String title, String content, String mediaUrl, LocalDateTime createdAt, UserResponse user, Long likeCount) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.mediaUrl = mediaUrl;
        this.createdAt = createdAt;
        this.user = user;
        this.likeCount = likeCount;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public UserResponse getUser() { return user; }
    public void setUser(UserResponse user) { this.user = user; }
    public Long getLikeCount() { return likeCount; }
    public void setLikeCount(Long likeCount) { this.likeCount = likeCount; }
    public String getMediaUrl() { return mediaUrl; }
    public void setMediaUrl(String mediaUrl) { this.mediaUrl = mediaUrl; }

    // Nested DTO for user info
    public static class UserResponse {
        private Long id;
        private String username;
        private String bio;
        private String location;
        private String website;
        private String profilePictureUrl;
        private Set<String> interests;

        public UserResponse() {}
        public UserResponse(Long id, String username, String bio, String location, String website, String profilePictureUrl, Set<String> interests) {
            this.id = id;
            this.username = username;
            this.bio = bio;
            this.location = location;
            this.website = website;
            this.profilePictureUrl = profilePictureUrl;
            this.interests = interests;
        }
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getBio() { return bio; }
        public void setBio(String bio) { this.bio = bio; }
        public String getLocation() { return location; }
        public void setLocation(String location) { this.location = location; }
        public String getWebsite() { return website; }
        public void setWebsite(String website) { this.website = website; }
        public String getProfilePictureUrl() { return profilePictureUrl; }
        public void setProfilePictureUrl(String profilePictureUrl) { this.profilePictureUrl = profilePictureUrl; }
        public Set<String> getInterests() { return interests; }
        public void setInterests(Set<String> interests) { this.interests = interests; }
    }
}
