package com.cache.plugin.annotation;

/**
 * 缓存策略枚举
 */
public enum CacheStrategy {
    
    /**
     * 本地优先策略
     * 先查本地缓存，未命中再查远程缓存
     */
    LOCAL_FIRST,
    
    /**
     * 远程优先策略
     * 先查远程缓存，未命中再查本地缓存
     */
    REMOTE_FIRST,
    
    /**
     * 仅本地缓存
     */
    LOCAL_ONLY,
    
    /**
     * 仅远程缓存
     */
    REMOTE_ONLY,
    
    /**
     * 写穿透模式
     * 同时写入本地和远程缓存
     */
    WRITE_THROUGH,
    
    /**
     * 写回模式
     * 先写本地缓存，异步写入远程缓存
     */
    WRITE_BEHIND
}