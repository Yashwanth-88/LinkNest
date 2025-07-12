package com.linknest.linknest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CommentEditRequest {
    @NotBlank(message = "Content is required")
    @Size(max = 500, message = "Content must be 500 characters or less")
    private String content;

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
} 