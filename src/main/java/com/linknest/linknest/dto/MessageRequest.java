package com.linknest.linknest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class MessageRequest {
    private Long recipientId;
    private String content;
    private String mediaUrl;

    public MessageRequest() {}
    public MessageRequest(Long recipientId, String content, String mediaUrl) {
        this.recipientId = recipientId;
        this.content = content;
        this.mediaUrl = mediaUrl;
    }
    public Long getRecipientId() { return recipientId; }
    public void setRecipientId(Long recipientId) { this.recipientId = recipientId; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getMediaUrl() { return mediaUrl; }
    public void setMediaUrl(String mediaUrl) { this.mediaUrl = mediaUrl; }
} 