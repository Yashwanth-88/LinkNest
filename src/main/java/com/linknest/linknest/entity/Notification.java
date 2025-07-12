package com.linknest.linknest.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "LINK_NEST_NOTIFICATION")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String type; // e.g., "comment", "like"

    @ManyToOne
    @JoinColumn(name = "post_id")
    private Post post;

    @ManyToOne
    @JoinColumn(name = "comment_id")
    private Comment comment;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private boolean isRead = false;

    @Column(length = 500)
    private String message;

    @Column
    private Long targetId;

    @Column(length = 50)
    private String targetType;

    @Column(columnDefinition = "TEXT")
    private String data;

    // Default constructor required by JPA
    public Notification() {
    }

    // Constructor with fields
    public Notification(User user, String type, Post post, Comment comment, String message, Long targetId, String targetType, String data) {
        this.user = user;
        this.type = type;
        this.post = post;
        this.comment = comment;
        this.message = message;
        this.targetId = targetId;
        this.targetType = targetType;
        this.data = data;
        this.createdAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public Post getPost() { return post; }
    public void setPost(Post post) { this.post = post; }
    public Comment getComment() { return comment; }
    public void setComment(Comment comment) { this.comment = comment; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { this.isRead = read; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public Long getTargetId() { return targetId; }
    public void setTargetId(Long targetId) { this.targetId = targetId; }
    public String getTargetType() { return targetType; }
    public void setTargetType(String targetType) { this.targetType = targetType; }
    public String getData() { return data; }
    public void setData(String data) { this.data = data; }

    public String toWebSocketMessage() {
        return type + ": " + (post != null ? "Post ID " + post.getId() : "") + (comment != null ? ", Comment ID " + comment.getId() : "") + ".";
    }
}