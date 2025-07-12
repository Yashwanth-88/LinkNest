package com.linknest.linknest.dto;

import java.util.Set;

public class UserResponse {
    private Long id;
    private String username;
    private String bio;
    private String location;
    private String website;
    private String profilePictureUrl;
    private Set<String> interests;
    
    public UserResponse(Long id, String username, String bio, String location, String website, String profilePictureUrl, Set<String> interests) {
        this.id = id;
        this.username = username;
        this.bio = bio;
        this.location = location;
        this.website = website;
        this.profilePictureUrl = profilePictureUrl;
        this.interests = interests;
    }
    
    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getBio() { return bio; }
    public String getLocation() { return location; }
    public String getWebsite() { return website; }
    public String getProfilePictureUrl() { return profilePictureUrl; }
    public Set<String> getInterests() { return interests; }
}