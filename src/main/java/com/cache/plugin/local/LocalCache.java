package com.cache.plugin.local;

import com.cache.plugin.core.TieredCache;

import java.time.Duration;

/**
 * 本地缓存接口
 * 
 * @param <K> 键类型
 * @param <V> 值类型
 */
public interface LocalCache<K, V> extends TieredCache<K, V> {
    
    /**
     * 设置最大缓存大小
     * 
     * @param maxSize 最大大小
     */
    void setMaxSize(long maxSize);
    
    /**
     * 设置写入后过期时间
     * 
     * @param duration 过期时间
     */
    void setExpireAfterWrite(Duration duration);
    
    /**
     * 设置访问后过期时间
     * 
     * @param duration 过期时间
     */
    void setExpireAfterAccess(Duration duration);
    
    /**
     * 获取当前缓存大小
     * 
     * @return 当前大小
     */
    long getCurrentSize();
    
    /**
     * 获取最大缓存大小
     * 
     * @return 最大大小
     */
    long getMaxSize();
    
    /**
     * 手动触发清理过期条目
     */
    void cleanUp();
    
    /**
     * 获取缓存名称
     * 
     * @return 缓存名称
     */
    String getName();
}