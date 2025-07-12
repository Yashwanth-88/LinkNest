package com.linknest.linknest.dto;

public class UserAnalyticsResponse {
    private int postCount;
    private int commentCount;
    private int likeCount;
    private int followerCount;
    private int badgeCount;

    public UserAnalyticsResponse(int postCount, int commentCount, int likeCount, int followerCount, int badgeCount) {
        this.postCount = postCount;
        this.commentCount = commentCount;
        this.likeCount = likeCount;
        this.followerCount = followerCount;
        this.badgeCount = badgeCount;
    }
    public int getPostCount() { return postCount; }
    public void setPostCount(int postCount) { this.postCount = postCount; }
    public int getCommentCount() { return commentCount; }
    public void setCommentCount(int commentCount) { this.commentCount = commentCount; }
    public int getLikeCount() { return likeCount; }
    public void setLikeCount(int likeCount) { this.likeCount = likeCount; }
    public int getFollowerCount() { return followerCount; }
    public void setFollowerCount(int followerCount) { this.followerCount = followerCount; }
    public int getBadgeCount() { return badgeCount; }
    public void setBadgeCount(int badgeCount) { this.badgeCount = badgeCount; }
} 