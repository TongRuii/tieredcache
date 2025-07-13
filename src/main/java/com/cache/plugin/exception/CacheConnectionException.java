package com.cache.plugin.exception;

/**
 * 缓存连接异常
 */
public class CacheConnectionException extends CacheException {
    
    public CacheConnectionException() {
        super("Cache connection failed");
    }
    
    public CacheConnectionException(String message) {
        super(message);
    }
    
    public CacheConnectionException(String message, Throwable cause) {
        super(message, cause);
    }
}