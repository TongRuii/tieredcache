package com.cache.plugin.local.impl;

import com.cache.plugin.config.TieredCacheProperties;
import com.cache.plugin.core.CacheStats;
import com.cache.plugin.exception.CacheException;
import com.cache.plugin.local.LocalCache;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Caffeine本地缓存实现
 */
public class CaffeineLocalCache implements LocalCache<String, Object> {
    
    private static final Logger logger = LoggerFactory.getLogger(CaffeineLocalCache.class);
    
    private final Cache<String, Object> cache;
    private final String name;
    private final TieredCacheProperties.LocalCacheProperties properties;
    
    public CaffeineLocalCache(TieredCacheProperties.LocalCacheProperties properties) {
        this.properties = properties;
        this.name = "caffeine-local-cache";
        this.cache = buildCache(properties);
        logger.info("Caffeine local cache initialized with maxSize: {}, expireAfterWrite: {}, expireAfterAccess: {}", 
                   properties.getMaxSize(), properties.getExpireAfterWrite(), properties.getExpireAfterAccess());
    }
    
    /**
     * 构建Caffeine缓存
     */
    private Cache<String, Object> buildCache(TieredCacheProperties.LocalCacheProperties properties) {
        Caffeine<Object, Object> builder = Caffeine.newBuilder()
                .maximumSize(properties.getMaxSize())
                .initialCapacity(properties.getInitialCapacity());
        
        // 设置写入后过期时间
        if (properties.getExpireAfterWrite() != null) {
            builder.expireAfterWrite(properties.getExpireAfterWrite().toMillis(), TimeUnit.MILLISECONDS);
        }
        
        // 设置访问后过期时间
        if (properties.getExpireAfterAccess() != null) {
            builder.expireAfterAccess(properties.getExpireAfterAccess().toMillis(), TimeUnit.MILLISECONDS);
        }
        
        // 启用统计
        if (properties.isRecordStats()) {
            builder.recordStats();
        }
        
        return builder.build();
    }
    
    @Override
    public Object get(String key) {
        try {
            return cache.getIfPresent(key);
        } catch (Exception e) {
            logger.error("Failed to get value from local cache for key: {}", key, e);
            return null;
        }
    }
    
    @Override
    public void put(String key, Object value) {
        try {
            if (key != null && value != null) {
                cache.put(key, value);
                logger.debug("Put value to local cache for key: {}", key);
            }
        } catch (Exception e) {
            logger.error("Failed to put value to local cache for key: {}", key, e);
            throw new CacheException("Failed to put value to local cache", e);
        }
    }
    
    @Override
    public void put(String key, Object value, Duration ttl) {
        // Caffeine不支持单独设置TTL，使用默认的过期策略
        put(key, value);
    }
    
    @Override
    public void evict(String key) {
        try {
            cache.invalidate(key);
            logger.debug("Evicted key from local cache: {}", key);
        } catch (Exception e) {
            logger.error("Failed to evict key from local cache: {}", key, e);
            throw new CacheException("Failed to evict key from local cache", e);
        }
    }
    
    @Override
    public void clear() {
        try {
            cache.invalidateAll();
            logger.info("Cleared all entries from local cache");
        } catch (Exception e) {
            logger.error("Failed to clear local cache", e);
            throw new CacheException("Failed to clear local cache", e);
        }
    }
    
    @Override
    public boolean containsKey(String key) {
        try {
            return cache.getIfPresent(key) != null;
        } catch (Exception e) {
            logger.error("Failed to check if local cache contains key: {}", key, e);
            return false;
        }
    }
    
    @Override
    public long size() {
        try {
            return cache.estimatedSize();
        } catch (Exception e) {
            logger.error("Failed to get local cache size", e);
            return 0;
        }
    }
    
    @Override
    public Map<String, Object> multiGet(Set<String> keys) {
        try {
            Map<String, Object> result = new HashMap<>();
            for (String key : keys) {
                Object value = cache.getIfPresent(key);
                if (value != null) {
                    result.put(key, value);
                }
            }
            return result;
        } catch (Exception e) {
            logger.error("Failed to multi get from local cache", e);
            return new HashMap<>();
        }
    }
    
    @Override
    public void multiPut(Map<String, Object> keyValues) {
        try {
            cache.putAll(keyValues);
            logger.debug("Multi put {} entries to local cache", keyValues.size());
        } catch (Exception e) {
            logger.error("Failed to multi put to local cache", e);
            throw new CacheException("Failed to multi put to local cache", e);
        }
    }
    
    @Override
    public void multiEvict(Set<String> keys) {
        try {
            cache.invalidateAll(keys);
            logger.debug("Multi evicted {} keys from local cache", keys.size());
        } catch (Exception e) {
            logger.error("Failed to multi evict from local cache", e);
            throw new CacheException("Failed to multi evict from local cache", e);
        }
    }
    
    @Override
    public CacheStats getStats() {
        try {
            com.github.benmanes.caffeine.cache.stats.CacheStats caffeineStats = cache.stats();
            return new CacheStats(
                caffeineStats.hitCount(),
                caffeineStats.missCount(),
                caffeineStats.loadCount(),
                caffeineStats.evictionCount(),
                caffeineStats.averageLoadPenalty()
            );
        } catch (Exception e) {
            logger.error("Failed to get local cache stats", e);
            return new CacheStats(0, 0, 0, 0, 0.0);
        }
    }
    
    @Override
    public boolean isAvailable() {
        return cache != null;
    }
    
    @Override
    public void setMaxSize(long maxSize) {
        // Caffeine不支持动态修改最大大小，需要重建缓存
        logger.warn("Caffeine cache does not support dynamic max size change. Current maxSize: {}, requested: {}", 
                   properties.getMaxSize(), maxSize);
    }
    
    @Override
    public void setExpireAfterWrite(Duration duration) {
        // Caffeine不支持动态修改过期时间，需要重建缓存
        logger.warn("Caffeine cache does not support dynamic expire time change. Current expireAfterWrite: {}, requested: {}", 
                   properties.getExpireAfterWrite(), duration);
    }
    
    @Override
    public void setExpireAfterAccess(Duration duration) {
        // Caffeine不支持动态修改过期时间，需要重建缓存
        logger.warn("Caffeine cache does not support dynamic expire time change. Current expireAfterAccess: {}, requested: {}", 
                   properties.getExpireAfterAccess(), duration);
    }
    
    @Override
    public long getCurrentSize() {
        return cache.estimatedSize();
    }
    
    @Override
    public long getMaxSize() {
        return properties.getMaxSize();
    }
    
    @Override
    public void cleanUp() {
        try {
            cache.cleanUp();
            logger.debug("Cleaned up local cache");
        } catch (Exception e) {
            logger.error("Failed to clean up local cache", e);
        }
    }
    
    @Override
    public String getName() {
        return name;
    }
    
    /**
     * 获取底层Caffeine缓存实例
     */
    public Cache<String, Object> getNativeCache() {
        return cache;
    }
}