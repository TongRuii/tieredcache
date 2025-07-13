package com.cache.plugin.exception;

/**
 * 缓存序列化异常
 */
public class CacheSerializationException extends CacheException {
    
    public CacheSerializationException() {
        super("Cache serialization failed");
    }
    
    public CacheSerializationException(String message) {
        super(message);
    }
    
    public CacheSerializationException(String message, Throwable cause) {
        super(message, cause);
    }
}