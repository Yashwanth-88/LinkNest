package com.linknest.linknest.dto;

import java.util.List;
import java.util.Map;

public class AdminAnalyticsResponse {
    private int totalUsers;
    private int totalPosts;
    private int totalComments;
    private int totalLikes;
    private List<UserActivity> mostActiveUsers;
    private List<PostPopularity> mostPopularPosts;
    private List<String> trendingHashtags;
    private Map<String, Integer> userGrowthLast30Days;
    private Map<String, Integer> postGrowthLast30Days;
    private double engagementRate;
    private Map<String, Double> dailyEngagementRateLast30Days;

    public static class UserActivity {
        public Long userId;
        public String username;
        public int postCount;
        public UserActivity(Long userId, String username, int postCount) {
            this.userId = userId;
            this.username = username;
            this.postCount = postCount;
        }
        public Long getUserId() { return userId; }
        public String getUsername() { return username; }
        public int getPostCount() { return postCount; }
    }

    public static class PostPopularity {
        public Long postId;
        public String title;
        public int likeCount;
        public PostPopularity(Long postId, String title, int likeCount) {
            this.postId = postId;
            this.title = title;
            this.likeCount = likeCount;
        }
        public Long getPostId() { return postId; }
        public String getTitle() { return title; }
        public int getLikeCount() { return likeCount; }
    }

    public int getTotalUsers() { return totalUsers; }
    public void setTotalUsers(int totalUsers) { this.totalUsers = totalUsers; }
    public int getTotalPosts() { return totalPosts; }
    public void setTotalPosts(int totalPosts) { this.totalPosts = totalPosts; }
    public int getTotalComments() { return totalComments; }
    public void setTotalComments(int totalComments) { this.totalComments = totalComments; }
    public int getTotalLikes() { return totalLikes; }
    public void setTotalLikes(int totalLikes) { this.totalLikes = totalLikes; }
    public List<UserActivity> getMostActiveUsers() { return mostActiveUsers; }
    public void setMostActiveUsers(List<UserActivity> mostActiveUsers) { this.mostActiveUsers = mostActiveUsers; }
    public List<PostPopularity> getMostPopularPosts() { return mostPopularPosts; }
    public void setMostPopularPosts(List<PostPopularity> mostPopularPosts) { this.mostPopularPosts = mostPopularPosts; }
    public List<String> getTrendingHashtags() { return trendingHashtags; }
    public void setTrendingHashtags(List<String> trendingHashtags) { this.trendingHashtags = trendingHashtags; }
    public Map<String, Integer> getUserGrowthLast30Days() { return userGrowthLast30Days; }
    public void setUserGrowthLast30Days(Map<String, Integer> userGrowthLast30Days) { this.userGrowthLast30Days = userGrowthLast30Days; }
    public Map<String, Integer> getPostGrowthLast30Days() { return postGrowthLast30Days; }
    public void setPostGrowthLast30Days(Map<String, Integer> postGrowthLast30Days) { this.postGrowthLast30Days = postGrowthLast30Days; }
    public double getEngagementRate() { return engagementRate; }
    public void setEngagementRate(double engagementRate) { this.engagementRate = engagementRate; }
    public Map<String, Double> getDailyEngagementRateLast30Days() { return dailyEngagementRateLast30Days; }
    public void setDailyEngagementRateLast30Days(Map<String, Double> dailyEngagementRateLast30Days) { this.dailyEngagementRateLast30Days = dailyEngagementRateLast30Days; }
} 