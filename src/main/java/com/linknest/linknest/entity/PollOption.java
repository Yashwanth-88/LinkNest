package com.linknest.linknest.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "poll_options")
public class PollOption {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "poll_id")
    private Poll poll;

    @Column(nullable = false)
    private String text;

    @Column(nullable = false)
    private int voteCount = 0;

    public PollOption() {}
    public PollOption(Poll poll, String text) {
        this.poll = poll;
        this.text = text;
    }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Poll getPoll() { return poll; }
    public void setPoll(Poll poll) { this.poll = poll; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public int getVoteCount() { return voteCount; }
    public void setVoteCount(int voteCount) { this.voteCount = voteCount; }
} 