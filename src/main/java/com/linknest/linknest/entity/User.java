package com.linknest.linknest.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.util.Set;
import java.util.HashSet;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;
    @JsonIgnore
    private String password;
    private String bio;
    private String location;
    private String website;
    private String profilePictureUrl;
    private boolean privateProfile = false;
    private boolean notifyLikes = true;
    private boolean notifyComments = true;
    private boolean notifyFollows = true;
    private String status = "active";
    private LocalDateTime createdAt;
    private String email;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }

    @ElementCollection(fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "role", columnDefinition = "VARCHAR(255)") // Explicitly define column type
    private Set<Role> roles;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_interests", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "interest")
    private Set<String> interests = new HashSet<>();

    // Getters, setters, and constructors
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public Set<Role> getRoles() { return roles; }
    public void setRoles(Set<Role> roles) { this.roles = roles; }
    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getWebsite() { return website; }
    public void setWebsite(String website) { this.website = website; }
    public String getProfilePictureUrl() { return profilePictureUrl; }
    public void setProfilePictureUrl(String profilePictureUrl) { this.profilePictureUrl = profilePictureUrl; }
    public Set<String> getInterests() { return interests; }
    public void setInterests(Set<String> interests) { this.interests = interests; }
    public boolean isPrivateProfile() { return privateProfile; }
    public void setPrivateProfile(boolean privateProfile) { this.privateProfile = privateProfile; }
    public boolean isNotifyLikes() { return notifyLikes; }
    public void setNotifyLikes(boolean notifyLikes) { this.notifyLikes = notifyLikes; }
    public boolean isNotifyComments() { return notifyComments; }
    public void setNotifyComments(boolean notifyComments) { this.notifyComments = notifyComments; }
    public boolean isNotifyFollows() { return notifyFollows; }
    public void setNotifyFollows(boolean notifyFollows) { this.notifyFollows = notifyFollows; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}