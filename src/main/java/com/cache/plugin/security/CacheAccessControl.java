package com.cache.plugin.security;

import com.cache.plugin.config.TieredCacheProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 缓存访问控制组件
 */
public class CacheAccessControl {
    
    private static final Logger logger = LoggerFactory.getLogger(CacheAccessControl.class);
    
    private final TieredCacheProperties.AccessControlProperties properties;
    
    public CacheAccessControl(TieredCacheProperties.AccessControlProperties properties) {
        this.properties = properties;
        
        if (properties.isEnabled()) {
            logger.info("Cache access control initialized with default policy: {}", properties.getDefaultPolicy());
        }
    }
    
    /**
     * 检查读权限
     */
    public boolean hasReadPermission(String key, Authentication authentication) {
        if (!properties.isEnabled()) {
            return true;
        }
        
        return checkPermission(key, authentication, "read");
    }
    
    /**
     * 检查写权限
     */
    public boolean hasWritePermission(String key, Authentication authentication) {
        if (!properties.isEnabled()) {
            return true;
        }
        
        return checkPermission(key, authentication, "write");
    }
    
    /**
     * 检查权限
     */
    private boolean checkPermission(String key, Authentication authentication, String operation) {
        try {
            // 如果没有认证信息，根据默认策略决定
            if (authentication == null || !authentication.isAuthenticated()) {
                return isDefaultAllow();
            }
            
            // 检查具体的访问规则
            for (Map.Entry<String, String> rule : properties.getRules().entrySet()) {
                String pattern = rule.getKey();
                String permission = rule.getValue();
                
                if (matchesPattern(key, pattern)) {
                    return checkRolePermission(authentication, permission, operation);
                }
            }
            
            // 没有匹配的规则，使用默认策略
            return isDefaultAllow();
            
        } catch (Exception e) {
            logger.error("Error checking cache permission for key: {}, operation: {}", key, operation, e);
            // 出错时根据默认策略决定
            return isDefaultAllow();
        }
    }
    
    /**
     * 检查模式匹配
     */
    private boolean matchesPattern(String key, String pattern) {
        try {
            // 支持通配符模式
            String regexPattern = pattern
                    .replace("*", ".*")
                    .replace("?", ".");
            return Pattern.matches(regexPattern, key);
        } catch (Exception e) {
            logger.warn("Invalid pattern: {}", pattern, e);
            return false;
        }
    }
    
    /**
     * 检查角色权限
     */
    private boolean checkRolePermission(Authentication authentication, String permission, String operation) {
        try {
            // 解析权限配置
            // 格式：role:ADMIN:read,write 或 user:john:read 或 allow 或 deny
            String[] parts = permission.split(":");
            
            if (parts.length == 1) {
                // 简单的allow/deny
                return "allow".equalsIgnoreCase(parts[0]);
            }
            
            if (parts.length >= 3) {
                String type = parts[0]; // role 或 user
                String value = parts[1]; // 角色名或用户名
                String operations = parts[2]; // 操作列表
                
                // 检查操作是否被允许
                if (!operations.contains(operation)) {
                    return false;
                }
                
                if ("role".equalsIgnoreCase(type)) {
                    return hasRole(authentication, value);
                } else if ("user".equalsIgnoreCase(type)) {
                    return isUser(authentication, value);
                }
            }
            
            return isDefaultAllow();
            
        } catch (Exception e) {
            logger.error("Error parsing permission: {}", permission, e);
            return isDefaultAllow();
        }
    }
    
    /**
     * 检查用户是否具有指定角色
     */
    private boolean hasRole(Authentication authentication, String roleName) {
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        if (authorities == null) {
            return false;
        }
        
        String roleToCheck = roleName.startsWith("ROLE_") ? roleName : "ROLE_" + roleName;
        
        return authorities.stream()
                .anyMatch(authority -> roleToCheck.equals(authority.getAuthority()));
    }
    
    /**
     * 检查是否为指定用户
     */
    private boolean isUser(Authentication authentication, String username) {
        return username.equals(authentication.getName());
    }
    
    /**
     * 检查默认策略是否允许
     */
    private boolean isDefaultAllow() {
        return "allow".equalsIgnoreCase(properties.getDefaultPolicy());
    }
    
    /**
     * 检查是否启用访问控制
     */
    public boolean isEnabled() {
        return properties.isEnabled();
    }
    
    /**
     * 获取默认策略
     */
    public String getDefaultPolicy() {
        return properties.getDefaultPolicy();
    }
    
    /**
     * 添加访问规则
     */
    public void addRule(String pattern, String permission) {
        properties.getRules().put(pattern, permission);
        logger.info("Added cache access rule: {} -> {}", pattern, permission);
    }
    
    /**
     * 移除访问规则
     */
    public void removeRule(String pattern) {
        properties.getRules().remove(pattern);
        logger.info("Removed cache access rule: {}", pattern);
    }
    
    /**
     * 获取所有访问规则
     */
    public Map<String, String> getRules() {
        return properties.getRules();
    }
}