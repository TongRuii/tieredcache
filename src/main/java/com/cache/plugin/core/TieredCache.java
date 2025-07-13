package com.cache.plugin.core;

import java.time.Duration;
import java.util.Map;
import java.util.Set;

/**
 * 统一缓存接口
 * 
 * @param <K> 键类型
 * @param <V> 值类型
 */
public interface TieredCache<K, V> {
    
    /**
     * 获取缓存值
     * 
     * @param key 缓存键
     * @return 缓存值，如果不存在返回null
     */
    V get(K key);
    
    /**
     * 存储缓存值
     * 
     * @param key 缓存键
     * @param value 缓存值
     */
    void put(K key, V value);
    
    /**
     * 存储缓存值并设置过期时间
     * 
     * @param key 缓存键
     * @param value 缓存值
     * @param ttl 过期时间
     */
    void put(K key, V value, Duration ttl);
    
    /**
     * 删除缓存
     * 
     * @param key 缓存键
     */
    void evict(K key);
    
    /**
     * 清空所有缓存
     */
    void clear();
    
    /**
     * 检查是否包含指定键
     * 
     * @param key 缓存键
     * @return 是否包含
     */
    boolean containsKey(K key);
    
    /**
     * 获取缓存大小
     * 
     * @return 缓存大小
     */
    long size();
    
    /**
     * 批量获取
     * 
     * @param keys 键集合
     * @return 键值对映射
     */
    Map<K, V> multiGet(Set<K> keys);
    
    /**
     * 批量存储
     * 
     * @param keyValues 键值对映射
     */
    void multiPut(Map<K, V> keyValues);
    
    /**
     * 批量删除
     * 
     * @param keys 键集合
     */
    void multiEvict(Set<K> keys);
    
    /**
     * 获取缓存统计信息
     * 
     * @return 缓存统计
     */
    CacheStats getStats();
    
    /**
     * 检查缓存是否可用
     * 
     * @return 是否可用
     */
    boolean isAvailable();
}