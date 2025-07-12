package com.linknest.linknest.dto;

import java.time.LocalDateTime;

public class StoryResponse {
    private Long id;
    private Long userId;
    private String mediaUrl;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;

    public StoryResponse() {}
    public StoryResponse(Long id, Long userId, String mediaUrl, LocalDateTime createdAt, LocalDateTime expiresAt) {
        this.id = id;
        this.userId = userId;
        this.mediaUrl = mediaUrl;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getMediaUrl() { return mediaUrl; }
    public void setMediaUrl(String mediaUrl) { this.mediaUrl = mediaUrl; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
} 