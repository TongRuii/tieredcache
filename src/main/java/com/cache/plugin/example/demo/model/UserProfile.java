package com.cache.plugin.example.demo.model;

/**
 * 用户详情类
 */
public class UserProfile {
    
    private Long userId;
    private String displayName;
    private String avatar;
    private String bio;
    private String phone;
    private String address;
    
    public UserProfile() {}
    
    public UserProfile(Long userId, String displayName) {
        this.userId = userId;
        this.displayName = displayName;
        this.bio = "这是用户 " + displayName + " 的个人简介";
        this.avatar = "https://avatar.example.com/" + userId + ".jpg";
    }
    
    // Getters and Setters
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    
    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }
    
    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }
    
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    
    @Override
    public String toString() {
        return String.format("UserProfile{userId=%d, displayName='%s'}", userId, displayName);
    }
}