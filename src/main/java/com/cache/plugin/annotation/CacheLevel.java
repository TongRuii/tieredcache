package com.cache.plugin.annotation;

/**
 * 缓存层级枚举
 */
public enum CacheLevel {
    
    /**
     * 本地缓存层
     */
    LOCAL,
    
    /**
     * 远程缓存层
     */
    REMOTE,
    
    /**
     * 所有缓存层
     */
    ALL
}