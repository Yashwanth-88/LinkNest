package com.linknest.linknest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public class PollCreateRequest {
    private String question;
    private List<String> options;

    public PollCreateRequest() {}
    public PollCreateRequest(String question, List<String> options) {
        this.question = question;
        this.options = options;
    }
    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }
    public List<String> getOptions() { return options; }
    public void setOptions(List<String> options) { this.options = options; }
} 