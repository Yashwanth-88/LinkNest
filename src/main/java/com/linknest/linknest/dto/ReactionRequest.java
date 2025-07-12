package com.linknest.linknest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class ReactionRequest {
    private String type; // e.g., like, love, laugh, wow
    private Long postId; // nullable if reacting to a comment
    private Long commentId; // nullable if reacting to a post

    public ReactionRequest() {}
    public ReactionRequest(String type, Long postId, Long commentId) {
        this.type = type;
        this.postId = postId;
        this.commentId = commentId;
    }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public Long getPostId() { return postId; }
    public void setPostId(Long postId) { this.postId = postId; }
    public Long getCommentId() { return commentId; }
    public void setCommentId(Long commentId) { this.commentId = commentId; }
} 