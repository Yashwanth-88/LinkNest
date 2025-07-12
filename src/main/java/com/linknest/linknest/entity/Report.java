package com.linknest.linknest.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "reports")
public class Report {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "reporter_id", nullable = false)
    private User reporter;

    @ManyToOne
    @JoinColumn(name = "reported_user_id")
    private User reportedUser;

    @ManyToOne
    @JoinColumn(name = "reported_post_id")
    private Post reportedPost;

    @ManyToOne
    @JoinColumn(name = "reported_comment_id")
    private Comment reportedComment;

    private String reason;
    private LocalDateTime createdAt = LocalDateTime.now();
    private String status = "open";

    public Report() {}
    public Report(User reporter, User reportedUser, Post reportedPost, String reason) {
        this.reporter = reporter;
        this.reportedUser = reportedUser;
        this.reportedPost = reportedPost;
        this.reason = reason;
        this.createdAt = LocalDateTime.now();
        this.status = "open";
    }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getReporter() { return reporter; }
    public void setReporter(User reporter) { this.reporter = reporter; }
    public User getReportedUser() { return reportedUser; }
    public void setReportedUser(User reportedUser) { this.reportedUser = reportedUser; }
    public Post getReportedPost() { return reportedPost; }
    public void setReportedPost(Post reportedPost) { this.reportedPost = reportedPost; }
    public Comment getReportedComment() { return reportedComment; }
    public void setReportedComment(Comment reportedComment) { this.reportedComment = reportedComment; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
} 