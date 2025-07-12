package com.linknest.linknest.dto;

import jakarta.validation.constraints.NotBlank;

public class StoryRequest {
    private String mediaUrl;
    public StoryRequest() {}
    public StoryRequest(String mediaUrl) { this.mediaUrl = mediaUrl; }
    public String getMediaUrl() { return mediaUrl; }
    public void setMediaUrl(String mediaUrl) { this.mediaUrl = mediaUrl; }
} 