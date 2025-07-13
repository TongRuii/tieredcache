package com.cache.plugin.example;

import java.io.Serializable;

/**
 * 用户配置文件
 */
public class UserProfile implements Serializable {
    
    private Long userId;
    private String displayName;
    private String bio;
    
    public UserProfile() {
    }
    
    public UserProfile(Long userId, String displayName, String bio) {
        this.userId = userId;
        this.displayName = displayName;
        this.bio = bio;
    }
    
    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
    
    public String getBio() {
        return bio;
    }
    
    public void setBio(String bio) {
        this.bio = bio;
    }
    
    @Override
    public String toString() {
        return "UserProfile{" +
                "userId=" + userId +
                ", displayName='" + displayName + '\'' +
                ", bio='" + bio + '\'' +
                '}';
    }
}