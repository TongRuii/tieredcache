package com.cache.plugin.exception;

/**
 * 缓存异常基类
 */
public class CacheException extends RuntimeException {
    
    public CacheException() {
        super();
    }
    
    public CacheException(String message) {
        super(message);
    }
    
    public CacheException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public CacheException(Throwable cause) {
        super(cause);
    }
}