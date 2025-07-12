package com.linknest.linknest.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "muted_hashtags", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "hashtag_id"}))
public class MutedHashtag {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(optional = false)
    @JoinColumn(name = "hashtag_id")
    private Hashtag hashtag;

    @Column(nullable = false)
    private LocalDateTime mutedAt = LocalDateTime.now();

    public MutedHashtag() {}
    public MutedHashtag(User user, Hashtag hashtag) {
        this.user = user;
        this.hashtag = hashtag;
        this.mutedAt = LocalDateTime.now();
    }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public Hashtag getHashtag() { return hashtag; }
    public void setHashtag(Hashtag hashtag) { this.hashtag = hashtag; }
    public LocalDateTime getMutedAt() { return mutedAt; }
    public void setMutedAt(LocalDateTime mutedAt) { this.mutedAt = mutedAt; }
} 