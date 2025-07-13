package com.cache.plugin.annotation;

/**
 * 缓存模式枚举
 */
public enum CacheMode {
    
    /**
     * 只读模式
     */
    READ_ONLY,
    
    /**
     * 只写模式
     */
    WRITE_ONLY,
    
    /**
     * 读写模式
     */
    READ_WRITE
}