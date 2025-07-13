package com.cache.plugin.example;

import java.io.Serializable;

/**
 * 用户设置
 */
public class UserSettings implements Serializable {
    
    private Long userId;
    private String settingKey;
    private String settingValue;
    private Boolean enabled;
    
    public UserSettings() {
    }
    
    public UserSettings(Long userId, String settingKey, String settingValue, Boolean enabled) {
        this.userId = userId;
        this.settingKey = settingKey;
        this.settingValue = settingValue;
        this.enabled = enabled;
    }
    
    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    public String getSettingKey() {
        return settingKey;
    }
    
    public void setSettingKey(String settingKey) {
        this.settingKey = settingKey;
    }
    
    public String getSettingValue() {
        return settingValue;
    }
    
    public void setSettingValue(String settingValue) {
        this.settingValue = settingValue;
    }
    
    public Boolean getEnabled() {
        return enabled;
    }
    
    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }
    
    @Override
    public String toString() {
        return "UserSettings{" +
                "userId=" + userId +
                ", settingKey='" + settingKey + '\'' +
                ", settingValue='" + settingValue + '\'' +
                ", enabled=" + enabled +
                '}';
    }
}