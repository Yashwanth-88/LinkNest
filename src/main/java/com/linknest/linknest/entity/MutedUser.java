package com.linknest.linknest.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "muted_users", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "muted_user_id"}))
public class MutedUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(optional = false)
    @JoinColumn(name = "muted_user_id")
    private User mutedUser;

    @Column(nullable = false)
    private LocalDateTime mutedAt = LocalDateTime.now();

    public MutedUser() {}
    public MutedUser(User user, User mutedUser) {
        this.user = user;
        this.mutedUser = mutedUser;
        this.mutedAt = LocalDateTime.now();
    }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public User getMutedUser() { return mutedUser; }
    public void setMutedUser(User mutedUser) { this.mutedUser = mutedUser; }
    public LocalDateTime getMutedAt() { return mutedAt; }
    public void setMutedAt(LocalDateTime mutedAt) { this.mutedAt = mutedAt; }
} 