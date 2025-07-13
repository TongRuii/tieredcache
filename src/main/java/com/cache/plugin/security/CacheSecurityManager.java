package com.cache.plugin.security;

import com.cache.plugin.config.TieredCacheProperties;
import com.cache.plugin.exception.CacheAccessDeniedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 缓存安全管理器
 */
public class CacheSecurityManager {
    
    private static final Logger logger = LoggerFactory.getLogger(CacheSecurityManager.class);
    
    private final TieredCacheProperties.SecurityProperties securityProperties;
    private final CacheEncryption encryption;
    private final CacheAccessControl accessControl;
    
    public CacheSecurityManager(TieredCacheProperties.SecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
        this.encryption = new CacheEncryption(securityProperties.getEncryption());
        this.accessControl = new CacheAccessControl(securityProperties.getAccessControl());
    }
    
    /**
     * 安全获取缓存值
     */
    public Object secureGet(String key, Object value) {
        // 访问控制检查
        checkReadPermission(key);
        
        // 解密
        if (securityProperties.getEncryption().isEnabled() && value != null) {
            return encryption.decrypt(value);
        }
        
        return value;
    }
    
    /**
     * 安全存储缓存值
     */
    public Object securePut(String key, Object value) {
        // 访问控制检查
        checkWritePermission(key);
        
        // 加密
        if (securityProperties.getEncryption().isEnabled() && value != null) {
            return encryption.encrypt(value);
        }
        
        return value;
    }
    
    /**
     * 安全删除缓存
     */
    public void secureEvict(String key) {
        // 访问控制检查
        checkWritePermission(key);
    }
    
    /**
     * 检查读权限
     */
    private void checkReadPermission(String key) {
        if (!securityProperties.getAccessControl().isEnabled()) {
            return;
        }
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!accessControl.hasReadPermission(key, authentication)) {
            logger.warn("Read access denied for key: {} by user: {}", 
                       key, authentication != null ? authentication.getName() : "anonymous");
            throw new CacheAccessDeniedException("Read access denied for key: " + key);
        }
    }
    
    /**
     * 检查写权限
     */
    private void checkWritePermission(String key) {
        if (!securityProperties.getAccessControl().isEnabled()) {
            return;
        }
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!accessControl.hasWritePermission(key, authentication)) {
            logger.warn("Write access denied for key: {} by user: {}", 
                       key, authentication != null ? authentication.getName() : "anonymous");
            throw new CacheAccessDeniedException("Write access denied for key: " + key);
        }
    }
    
    /**
     * 检查是否启用安全功能
     */
    public boolean isSecurityEnabled() {
        return securityProperties.getEncryption().isEnabled() || 
               securityProperties.getAccessControl().isEnabled();
    }
    
    /**
     * 检查是否启用加密
     */
    public boolean isEncryptionEnabled() {
        return securityProperties.getEncryption().isEnabled();
    }
    
    /**
     * 检查是否启用访问控制
     */
    public boolean isAccessControlEnabled() {
        return securityProperties.getAccessControl().isEnabled();
    }
}