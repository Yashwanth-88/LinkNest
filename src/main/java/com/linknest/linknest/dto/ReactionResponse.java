package com.linknest.linknest.dto;

import java.util.Map;

public class ReactionResponse {
    private Map<String, Integer> counts; // type -> count
    private String userReaction; // the current user's reaction type, if any

    public ReactionResponse() {}
    public ReactionResponse(Map<String, Integer> counts, String userReaction) {
        this.counts = counts;
        this.userReaction = userReaction;
    }
    public Map<String, Integer> getCounts() { return counts; }
    public void setCounts(Map<String, Integer> counts) { this.counts = counts; }
    public String getUserReaction() { return userReaction; }
    public void setUserReaction(String userReaction) { this.userReaction = userReaction; }
} 