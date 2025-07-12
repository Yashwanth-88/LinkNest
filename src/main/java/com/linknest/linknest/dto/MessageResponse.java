package com.linknest.linknest.dto;

import java.time.LocalDateTime;

public class MessageResponse {
    private Long id;
    private Long senderId;
    private Long recipientId;
    private String content;
    private LocalDateTime createdAt;
    private boolean read;

    public MessageResponse() {}
    public MessageResponse(Long id, Long senderId, Long recipientId, String content, LocalDateTime createdAt, boolean read) {
        this.id = id;
        this.senderId = senderId;
        this.recipientId = recipientId;
        this.content = content;
        this.createdAt = createdAt;
        this.read = read;
    }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getSenderId() { return senderId; }
    public void setSenderId(Long senderId) { this.senderId = senderId; }
    public Long getRecipientId() { return recipientId; }
    public void setRecipientId(Long recipientId) { this.recipientId = recipientId; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }
} 