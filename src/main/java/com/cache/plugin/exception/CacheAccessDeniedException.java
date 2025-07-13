package com.cache.plugin.exception;

/**
 * 缓存访问被拒绝异常
 */
public class CacheAccessDeniedException extends CacheException {
    
    public CacheAccessDeniedException() {
        super("Cache access denied");
    }
    
    public CacheAccessDeniedException(String message) {
        super(message);
    }
    
    public CacheAccessDeniedException(String message, Throwable cause) {
        super(message, cause);
    }
}