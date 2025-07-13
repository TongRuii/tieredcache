package com.cache.plugin.remote;

import com.cache.plugin.core.TieredCache;

import java.time.Duration;

/**
 * 远程缓存接口
 * 
 * @param <K> 键类型
 * @param <V> 值类型
 */
public interface RemoteCache<K, V> extends TieredCache<K, V> {
    
    /**
     * 设置默认TTL
     * 
     * @param ttl 过期时间
     */
    void setDefaultTtl(Duration ttl);
    
    /**
     * 获取默认TTL
     * 
     * @return 默认过期时间
     */
    Duration getDefaultTtl();
    
    /**
     * 发布消息到指定频道
     * 
     * @param channel 频道名称
     * @param message 消息内容
     */
    void publish(String channel, Object message);
    
    /**
     * 订阅指定频道
     * 
     * @param channel 频道名称
     * @param listener 消息监听器
     */
    void subscribe(String channel, MessageListener listener);
    
    /**
     * 取消订阅指定频道
     * 
     * @param channel 频道名称
     */
    void unsubscribe(String channel);
    
    /**
     * 检查连接状态
     * 
     * @return 是否已连接
     */
    boolean isConnected();
    
    /**
     * 重新连接
     */
    void reconnect();
    
    /**
     * 关闭连接
     */
    void close();
    
    /**
     * 获取缓存名称
     * 
     * @return 缓存名称
     */
    String getName();
    
    /**
     * 设置键的过期时间
     * 
     * @param key 缓存键
     * @param ttl 过期时间
     * @return 是否设置成功
     */
    boolean expire(K key, Duration ttl);
    
    /**
     * 获取键的剩余过期时间
     * 
     * @param key 缓存键
     * @return 剩余过期时间，如果键不存在或没有设置过期时间返回null
     */
    Duration getExpire(K key);
}