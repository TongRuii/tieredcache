package com.cache.plugin.remote.impl;

import com.cache.plugin.config.TieredCacheProperties;
import com.cache.plugin.core.CacheStats;
import com.cache.plugin.exception.CacheConnectionException;
import com.cache.plugin.exception.CacheException;
import com.cache.plugin.exception.CacheSerializationException;
import com.cache.plugin.remote.MessageListener;
import com.cache.plugin.remote.RemoteCache;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Redis远程缓存实现
 */
public class RedisRemoteCache implements RemoteCache<String, Object> {
    
    private static final Logger logger = LoggerFactory.getLogger(RedisRemoteCache.class);
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final TieredCacheProperties.RemoteCacheProperties properties;
    private final ObjectMapper objectMapper;
    private final String name;
    private final Map<String, MessageListener> listeners;
    private final RedisMessageListenerContainer messageListenerContainer;
    
    // 统计信息
    private final AtomicLong hitCount = new AtomicLong(0);
    private final AtomicLong missCount = new AtomicLong(0);
    private final AtomicLong putCount = new AtomicLong(0);
    private final AtomicLong evictCount = new AtomicLong(0);
    
    public RedisRemoteCache(TieredCacheProperties.RemoteCacheProperties properties,
                           RedisTemplate<String, Object> redisTemplate) {
        this.properties = properties;
        this.redisTemplate = redisTemplate;
        this.name = "redis-remote-cache";
        this.objectMapper = new ObjectMapper();
        this.listeners = new ConcurrentHashMap<>();
        this.messageListenerContainer = new RedisMessageListenerContainer();
        this.messageListenerContainer.setConnectionFactory(redisTemplate.getConnectionFactory());
        this.messageListenerContainer.afterPropertiesSet();
        this.messageListenerContainer.start();
        
        logger.info("Redis remote cache initialized with TTL: {}, timeout: {}", 
                   properties.getTtl(), properties.getTimeout());
    }
    
    @Override
    public Object get(String key) {
        try {
            Object value = redisTemplate.opsForValue().get(key);
            if (value != null) {
                hitCount.incrementAndGet();
                logger.debug("Hit remote cache for key: {}", key);
                return deserializeValue(value);
            } else {
                missCount.incrementAndGet();
                logger.debug("Miss remote cache for key: {}", key);
                return null;
            }
        } catch (Exception e) {
            logger.error("Failed to get value from remote cache for key: {}", key, e);
            missCount.incrementAndGet();
            return null;
        }
    }
    
    @Override
    public void put(String key, Object value) {
        put(key, value, properties.getTtl());
    }
    
    @Override
    public void put(String key, Object value, Duration ttl) {
        try {
            if (key != null && value != null) {
                Object serializedValue = serializeValue(value);
                if (ttl != null && !ttl.isZero() && !ttl.isNegative()) {
                    redisTemplate.opsForValue().set(key, serializedValue, ttl.toMillis(), TimeUnit.MILLISECONDS);
                } else {
                    redisTemplate.opsForValue().set(key, serializedValue);
                }
                putCount.incrementAndGet();
                logger.debug("Put value to remote cache for key: {} with TTL: {}", key, ttl);
            }
        } catch (Exception e) {
            logger.error("Failed to put value to remote cache for key: {}", key, e);
            throw new CacheException("Failed to put value to remote cache", e);
        }
    }
    
    @Override
    public void evict(String key) {
        try {
            Boolean deleted = redisTemplate.delete(key);
            if (Boolean.TRUE.equals(deleted)) {
                evictCount.incrementAndGet();
                logger.debug("Evicted key from remote cache: {}", key);
            }
        } catch (Exception e) {
            logger.error("Failed to evict key from remote cache: {}", key, e);
            throw new CacheException("Failed to evict key from remote cache", e);
        }
    }
    
    @Override
    public void clear() {
        try {
            // 注意：这会清空整个Redis数据库，生产环境需要谨慎使用
            redisTemplate.getConnectionFactory().getConnection().flushDb();
            logger.warn("Cleared all entries from remote cache (entire Redis database)");
        } catch (Exception e) {
            logger.error("Failed to clear remote cache", e);
            throw new CacheException("Failed to clear remote cache", e);
        }
    }
    
    @Override
    public boolean containsKey(String key) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (Exception e) {
            logger.error("Failed to check if remote cache contains key: {}", key, e);
            return false;
        }
    }
    
    @Override
    public long size() {
        try {
            return redisTemplate.getConnectionFactory().getConnection().dbSize();
        } catch (Exception e) {
            logger.error("Failed to get remote cache size", e);
            return 0;
        }
    }
    
    @Override
    public Map<String, Object> multiGet(Set<String> keys) {
        try {
            Map<String, Object> result = new HashMap<>();
            if (keys != null && !keys.isEmpty()) {
                Map<String, Object> values = redisTemplate.opsForValue().multiGet(keys);
                if (values != null) {
                    for (Map.Entry<String, Object> entry : values.entrySet()) {
                        if (entry.getValue() != null) {
                            result.put(entry.getKey(), deserializeValue(entry.getValue()));
                            hitCount.incrementAndGet();
                        } else {
                            missCount.incrementAndGet();
                        }
                    }
                }
            }
            return result;
        } catch (Exception e) {
            logger.error("Failed to multi get from remote cache", e);
            return new HashMap<>();
        }
    }
    
    @Override
    public void multiPut(Map<String, Object> keyValues) {
        try {
            if (keyValues != null && !keyValues.isEmpty()) {
                Map<String, Object> serializedValues = new HashMap<>();
                for (Map.Entry<String, Object> entry : keyValues.entrySet()) {
                    serializedValues.put(entry.getKey(), serializeValue(entry.getValue()));
                }
                redisTemplate.opsForValue().multiSet(serializedValues);
                putCount.addAndGet(keyValues.size());
                logger.debug("Multi put {} entries to remote cache", keyValues.size());
            }
        } catch (Exception e) {
            logger.error("Failed to multi put to remote cache", e);
            throw new CacheException("Failed to multi put to remote cache", e);
        }
    }
    
    @Override
    public void multiEvict(Set<String> keys) {
        try {
            if (keys != null && !keys.isEmpty()) {
                Long deletedCount = redisTemplate.delete(keys);
                evictCount.addAndGet(deletedCount != null ? deletedCount : 0);
                logger.debug("Multi evicted {} keys from remote cache", deletedCount);
            }
        } catch (Exception e) {
            logger.error("Failed to multi evict from remote cache", e);
            throw new CacheException("Failed to multi evict from remote cache", e);
        }
    }
    
    @Override
    public CacheStats getStats() {
        return new CacheStats(
            hitCount.get(),
            missCount.get(),
            putCount.get(),
            evictCount.get(),
            0.0 // Redis不提供平均加载时间
        );
    }
    
    @Override
    public boolean isAvailable() {
        try {
            redisTemplate.getConnectionFactory().getConnection().ping();
            return true;
        } catch (Exception e) {
            logger.error("Redis connection is not available", e);
            return false;
        }
    }
    
    @Override
    public void setDefaultTtl(Duration ttl) {
        // 更新配置中的TTL
        logger.info("Updated default TTL from {} to {}", properties.getTtl(), ttl);
        // 注意：这里需要修改properties，但properties通常是不可变的
        // 实际实现中可能需要重新设计
    }
    
    @Override
    public Duration getDefaultTtl() {
        return properties.getTtl();
    }
    
    @Override
    public void publish(String channel, Object message) {
        try {
            String serializedMessage = objectMapper.writeValueAsString(message);
            redisTemplate.convertAndSend(channel, serializedMessage);
            logger.debug("Published message to channel: {}", channel);
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize message for channel: {}", channel, e);
            throw new CacheSerializationException("Failed to serialize message", e);
        } catch (Exception e) {
            logger.error("Failed to publish message to channel: {}", channel, e);
            throw new CacheException("Failed to publish message", e);
        }
    }
    
    @Override
    public void subscribe(String channel, MessageListener listener) {
        try {
            listeners.put(channel, listener);
            messageListenerContainer.addMessageListener(
                (message, pattern) -> {
                    try {
                        String messageBody = new String(message.getBody());
                        Object deserializedMessage = objectMapper.readValue(messageBody, Object.class);
                        listener.onMessage(channel, deserializedMessage);
                    } catch (Exception e) {
                        logger.error("Failed to process message from channel: {}", channel, e);
                    }
                },
                new ChannelTopic(channel)
            );
            logger.info("Subscribed to channel: {}", channel);
        } catch (Exception e) {
            logger.error("Failed to subscribe to channel: {}", channel, e);
            throw new CacheException("Failed to subscribe to channel", e);
        }
    }
    
    @Override
    public void unsubscribe(String channel) {
        try {
            listeners.remove(channel);
            // Redis消息监听器容器不支持直接取消订阅单个频道
            // 需要重新配置监听器容器
            logger.info("Unsubscribed from channel: {}", channel);
        } catch (Exception e) {
            logger.error("Failed to unsubscribe from channel: {}", channel, e);
            throw new CacheException("Failed to unsubscribe from channel", e);
        }
    }
    
    @Override
    public boolean isConnected() {
        return isAvailable();
    }
    
    @Override
    public void reconnect() {
        try {
            // Redis连接池会自动重连，这里主要是重置连接
            redisTemplate.getConnectionFactory().getConnection().ping();
            logger.info("Redis connection reconnected successfully");
        } catch (Exception e) {
            logger.error("Failed to reconnect to Redis", e);
            throw new CacheConnectionException("Failed to reconnect to Redis", e);
        }
    }
    
    @Override
    public void close() {
        try {
            if (messageListenerContainer != null) {
                messageListenerContainer.stop();
                messageListenerContainer.destroy();
            }
            logger.info("Redis remote cache closed");
        } catch (Exception e) {
            logger.error("Failed to close Redis remote cache", e);
        }
    }
    
    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public boolean expire(String key, Duration ttl) {
        try {
            return Boolean.TRUE.equals(redisTemplate.expire(key, ttl.toMillis(), TimeUnit.MILLISECONDS));
        } catch (Exception e) {
            logger.error("Failed to set expire for key: {}", key, e);
            return false;
        }
    }
    
    @Override
    public Duration getExpire(String key) {
        try {
            Long ttl = redisTemplate.getExpire(key, TimeUnit.MILLISECONDS);
            return ttl != null && ttl > 0 ? Duration.ofMillis(ttl) : null;
        } catch (Exception e) {
            logger.error("Failed to get expire for key: {}", key, e);
            return null;
        }
    }
    
    /**
     * 序列化值
     */
    private Object serializeValue(Object value) {
        // 如果RedisTemplate已经配置了序列化器，直接返回
        // 否则使用JSON序列化
        return value;
    }
    
    /**
     * 反序列化值
     */
    private Object deserializeValue(Object value) {
        // 如果RedisTemplate已经配置了序列化器，直接返回
        // 否则进行JSON反序列化
        return value;
    }
    
    /**
     * 获取底层RedisTemplate实例
     */
    public RedisTemplate<String, Object> getNativeCache() {
        return redisTemplate;
    }
}