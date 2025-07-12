package com.linknest.linknest.dto;

import java.util.List;

public class PollResultResponse {
    private String question;
    private List<OptionResult> options;
    private Long userVotedOptionId;

    public PollResultResponse() {}
    public PollResultResponse(String question, List<OptionResult> options, Long userVotedOptionId) {
        this.question = question;
        this.options = options;
        this.userVotedOptionId = userVotedOptionId;
    }
    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }
    public List<OptionResult> getOptions() { return options; }
    public void setOptions(List<OptionResult> options) { this.options = options; }
    public Long getUserVotedOptionId() { return userVotedOptionId; }
    public void setUserVotedOptionId(Long userVotedOptionId) { this.userVotedOptionId = userVotedOptionId; }

    public static class OptionResult {
        private Long id;
        private String text;
        private int voteCount;
        public OptionResult() {}
        public OptionResult(Long id, String text, int voteCount) {
            this.id = id;
            this.text = text;
            this.voteCount = voteCount;
        }
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getText() { return text; }
        public void setText(String text) { this.text = text; }
        public int getVoteCount() { return voteCount; }
        public void setVoteCount(int voteCount) { this.voteCount = voteCount; }
    }
} 