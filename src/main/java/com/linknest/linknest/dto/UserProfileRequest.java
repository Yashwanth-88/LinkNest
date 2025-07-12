package com.linknest.linknest.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public class UserProfileRequest {
    @Size(max = 500, message = "Bio must be 500 characters or less")
    private String bio;
    
    @Size(max = 100, message = "Location must be 100 characters or less")
    private String location;
    
    @Size(max = 200, message = "Website must be 200 characters or less")
    private String website;
    
    @Email(message = "Email should be valid")
    private String email;
    
    private String profilePictureUrl;
    
    private boolean privateProfile = false;
    
    // Getters and Setters
    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getWebsite() { return website; }
    public void setWebsite(String website) { this.website = website; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getProfilePictureUrl() { return profilePictureUrl; }
    public void setProfilePictureUrl(String profilePictureUrl) { this.profilePictureUrl = profilePictureUrl; }
    public boolean isPrivateProfile() { return privateProfile; }
    public void setPrivateProfile(boolean privateProfile) { this.privateProfile = privateProfile; }
} 