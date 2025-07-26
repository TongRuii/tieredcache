package com.cache.plugin.core;

import com.cache.plugin.annotation.CacheStrategy;
import com.cache.plugin.config.TieredCacheProperties;
import com.cache.plugin.exception.CacheException;
import com.cache.plugin.local.LocalCache;
import com.cache.plugin.remote.RemoteCache;
import com.cache.plugin.metrics.CacheMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 分层缓存管理器
 */
public class TieredCacheManager {
    
    private static final Logger logger = LoggerFactory.getLogger(TieredCacheManager.class);
    
    private final LocalCache<String, Object> localCache;
    private final RemoteCache<String, Object> remoteCache;
    private final TieredCacheProperties properties;
    private final ExecutorService asyncExecutor;
    
    // 断路器相关字段
    private final AtomicInteger remoteFailureCount = new AtomicInteger(0);
    private static final int MAX_REMOTE_FAILURES = 5;
    private volatile boolean remoteCircuitOpen = false;
    
    // 性能监控相关字段
    private final AtomicInteger totalRequests = new AtomicInteger(0);
    private final AtomicInteger totalHits = new AtomicInteger(0);
    private static final double HIT_RATE_THRESHOLD = 0.8;
    
    @Autowired(required = false)
    private CacheMetrics metrics;
    
    public TieredCacheManager(LocalCache<String, Object> localCache,
                               RemoteCache<String, Object> remoteCache,
                               TieredCacheProperties properties) {
        this.localCache = localCache;
        this.remoteCache = remoteCache;
        this.properties = properties;
        this.asyncExecutor = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors(),
            r -> {
                Thread t = new Thread(r, "cache-async-");
                t.setDaemon(true);
                return t;
            }
        );
    }
    
    /**
     * 根据策略获取缓存值
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> type, CacheStrategy strategy) {
        try {
            // 当远程缓存不可用时，自动降级到本地策略
            if (remoteCache == null && 
                (strategy == CacheStrategy.REMOTE_FIRST || strategy == CacheStrategy.REMOTE_ONLY)) {
                strategy = CacheStrategy.LOCAL_FIRST;
            }
            
            switch (strategy) {
                case LOCAL_FIRST:
                    return getWithLocalFirst(key, type);
                case REMOTE_FIRST:
                    return getWithRemoteFirst(key, type);
                case LOCAL_ONLY:
                    return getFromLocal(key, type);
                case REMOTE_ONLY:
                    return getFromRemote(key, type);
                default:
                    return getWithLocalFirst(key, type);
            }
        } catch (Exception e) {
            logger.error("Failed to get cache value for key: {}", key, e);
            recordMiss();
            return null;
        }
    }
    
    /**
     * 本地优先获取策略
     */
    @SuppressWarnings("unchecked")
    private <T> T getWithLocalFirst(String key, Class<T> type) {
        // 1. 先查本地缓存
        Object value = localCache.get(key);
        if (value != null) {
            recordLocalHit();
            return (T) value;
        }
        
        // 2. 检查断路器状态
        if (!remoteCircuitOpen && remoteCache != null) {
            try {
                value = remoteCache.get(key);
                if (value != null) {
                    // 成功时重置失败计数
                    remoteFailureCount.set(0);
                    // 异步回写到本地缓存
                    asyncPutToLocal(key, value);
                    recordRemoteHit();
                    return (T) value;
                }
            } catch (Exception e) {
                // 记录失败并检查是否需要打开断路器
                int failureCount = remoteFailureCount.incrementAndGet();
                if (failureCount >= MAX_REMOTE_FAILURES) {
                    remoteCircuitOpen = true;
                    logger.warn("Remote cache circuit opened due to repeated failures");
                }
                logger.warn("Failed to get from remote cache, falling back to method execution", e);
            }
        }
        
        recordMiss();
        return null;
    }
    
    /**
     * 远程优先获取策略
     */
    @SuppressWarnings("unchecked")
    private <T> T getWithRemoteFirst(String key, Class<T> type) {
        // 1. 检查断路器状态
        if (!remoteCircuitOpen && remoteCache != null) {
            try {
                Object value = remoteCache.get(key);
                if (value != null) {
                    // 成功时重置失败计数
                    remoteFailureCount.set(0);
                    // 异步回写到本地缓存
                    asyncPutToLocal(key, value);
                    recordRemoteHit();
                    return (T) value;
                }
            } catch (Exception e) {
                // 记录失败并检查是否需要打开断路器
                int failureCount = remoteFailureCount.incrementAndGet();
                if (failureCount >= MAX_REMOTE_FAILURES) {
                    remoteCircuitOpen = true;
                    logger.warn("Remote cache circuit opened due to repeated failures");
                }
                logger.warn("Failed to get from remote cache", e);
            }
        }
        
        // 2. 查本地缓存
        Object value = localCache.get(key);
        if (value != null) {
            recordLocalHit();
            return (T) value;
        }
        
        recordMiss();
        return null;
    }
    
    /**
     * 仅从本地缓存获取
     */
    @SuppressWarnings("unchecked")
    private <T> T getFromLocal(String key, Class<T> type) {
        Object value = localCache.get(key);
        if (value != null) {
            recordLocalHit();
            return (T) value;
        }
        recordMiss();
        return null;
    }
    
    /**
     * 仅从远程缓存获取
     */
    @SuppressWarnings("unchecked")
    private <T> T getFromRemote(String key, Class<T> type) {
        // 当远程缓存不可用时直接返回null
        if (remoteCache == null) {
            recordMiss();
            return null;
        }
        
        // 检查断路器状态
        if (remoteCircuitOpen) {
            recordMiss();
            return null;
        }
        
        try {
            Object value = remoteCache.get(key);
            if (value != null) {
                // 成功时重置失败计数
                remoteFailureCount.set(0);
                recordRemoteHit();
                return (T) value;
            }
        } catch (Exception e) {
            // 记录失败并检查是否需要打开断路器
            int failureCount = remoteFailureCount.incrementAndGet();
            if (failureCount >= MAX_REMOTE_FAILURES) {
                remoteCircuitOpen = true;
                logger.warn("Remote cache circuit opened due to repeated failures");
            }
            logger.error("Failed to get from remote cache", e);
        }
        
        recordMiss();
        return null;
    }
    
    /**
     * 根据策略存储缓存值
     */
    public void put(String key, Object value, CacheStrategy strategy, Duration ttl) {
        try {
            switch (strategy) {
                case LOCAL_FIRST:
                case LOCAL_ONLY:
                    putToLocal(key, value);
                    // 只有当远程缓存存在时才尝试异步写入
                    if (strategy == CacheStrategy.LOCAL_FIRST && remoteCache != null) {
                        asyncPutToRemote(key, value, ttl);
                    }
                    break;
                case REMOTE_FIRST:
                case REMOTE_ONLY:
                    // 只有当远程缓存存在时才尝试写入
                    if (remoteCache != null) {
                        putToRemote(key, value, ttl);
                        if (strategy == CacheStrategy.REMOTE_FIRST) {
                            asyncPutToLocal(key, value);
                        }
                    } else {
                        // 远程缓存不存在时降级到本地缓存
                        putToLocal(key, value);
                    }
                    break;
                case WRITE_THROUGH:
                    putToLocal(key, value);
                    if (remoteCache != null) {
                        putToRemote(key, value, ttl);
                    }
                    break;
                case WRITE_BEHIND:
                    putToLocal(key, value);
                    if (remoteCache != null) {
                        asyncPutToRemote(key, value, ttl);
                    }
                    break;
            }
        } catch (Exception e) {
            logger.error("Failed to put cache value for key: {}", key, e);
            throw new CacheException("Failed to put cache value", e);
        }
    }
    
    /**
     * 根据策略删除缓存
     */
    public void evict(String key, CacheStrategy strategy) {
        try {
            switch (strategy) {
                case LOCAL_ONLY:
                    localCache.evict(key);
                    break;
                case REMOTE_ONLY:
                    if (remoteCache != null) {
                        remoteCache.evict(key);
                    }
                    break;
                default:
                    localCache.evict(key);
                    if (remoteCache != null) {
                        remoteCache.evict(key);
                    }
                    break;
            }
        } catch (Exception e) {
            logger.error("Failed to evict cache for key: {}", key, e);
            throw new CacheException("Failed to evict cache", e);
        }
    }
    
    /**
     * 清空所有缓存
     */
    public void clear() {
        try {
            localCache.clear();
            if (remoteCache != null) {
                remoteCache.clear();
            }
        } catch (Exception e) {
            logger.error("Failed to clear cache", e);
            throw new CacheException("Failed to clear cache", e);
        }
    }
    
    /**
     * 批量获取
     */
    public Map<String, Object> multiGet(Set<String> keys, CacheStrategy strategy) {
        // 实现批量获取逻辑
        // 这里简化实现，实际应该根据策略优化
        return localCache.multiGet(keys);
    }
    
    /**
     * 批量存储
     */
    public void multiPut(Map<String, Object> keyValues, CacheStrategy strategy, Duration ttl) {
        try {
            switch (strategy) {
                case LOCAL_ONLY:
                    localCache.multiPut(keyValues);
                    break;
                case REMOTE_ONLY:
                    if (remoteCache != null) {
                        remoteCache.multiPut(keyValues);
                    }
                    break;
                default:
                    localCache.multiPut(keyValues);
                    if (remoteCache != null) {
                        remoteCache.multiPut(keyValues);
                    }
                    break;
            }
        } catch (Exception e) {
            logger.error("Failed to multi put cache values", e);
            throw new CacheException("Failed to multi put cache values", e);
        }
    }
    
    /**
     * 存储到本地缓存
     */
    private void putToLocal(String key, Object value) {
        localCache.put(key, value);
    }
    
    /**
     * 存储到远程缓存
     */
    private void putToRemote(String key, Object value, Duration ttl) {
        if (ttl != null) {
            remoteCache.put(key, value, ttl);
        } else {
            remoteCache.put(key, value);
        }
    }
    
    /**
     * 异步存储到本地缓存
     */
    private void asyncPutToLocal(String key, Object value) {
        CompletableFuture.runAsync(() -> putToLocal(key, value), asyncExecutor)
            .exceptionally(throwable -> {
                logger.warn("Failed to async put to local cache for key: {}", key, throwable);
                return null;
            });
    }
    
    /**
     * 异步存储到远程缓存
     */
    private void asyncPutToRemote(String key, Object value, Duration ttl) {
        CompletableFuture.runAsync(() -> putToRemote(key, value, ttl), asyncExecutor)
            .exceptionally(throwable -> {
                logger.warn("Failed to async put to remote cache for key: {}", key, throwable);
                return null;
            });
    }
    
    /**
     * 记录本地缓存命中
     */
    private void recordLocalHit() {
        int hits = totalHits.incrementAndGet();
        int requests = totalRequests.incrementAndGet();
        
        if (metrics != null) {
            metrics.recordLocalHit();
        }
        
        // 检查命中率是否过低
        checkHitRate(requests, hits);
    }
    
    /**
     * 记录远程缓存命中
     */
    private void recordRemoteHit() {
        int hits = totalHits.incrementAndGet();
        int requests = totalRequests.incrementAndGet();
        
        if (metrics != null) {
            metrics.recordRemoteHit();
        }
        
        checkHitRate(requests, hits);
    }
    
    /**
     * 记录缓存未命中
     */
    private void recordMiss() {
        totalRequests.incrementAndGet();
        
        if (metrics != null) {
            metrics.recordMiss();
        }
    }
    
    /**
     * 检查命中率
     */
    private void checkHitRate(int totalRequests, int totalHits) {
        if (totalRequests > 100) { // 至少100个请求后才检查
            double hitRate = (double) totalHits / totalRequests;
            if (hitRate < HIT_RATE_THRESHOLD) {
                logger.warn("Cache hit rate is low: {:.2%}, below threshold: {:.2%}", hitRate, HIT_RATE_THRESHOLD);
            }
        }
    }
    
    /**
     * 定期检查断路器状态
     */
    @Scheduled(fixedRate = 60000) // 每分钟检查一次
    public void checkCircuitStatus() {
        if (remoteCircuitOpen) {
            remoteCircuitOpen = false;
            remoteFailureCount.set(0);
            logger.info("Remote cache circuit reset for retry");
        }
    }
    
    /**
     * 获取本地缓存
     */
    public LocalCache<String, Object> getLocalCache() {
        return localCache;
    }
    
    /**
     * 获取远程缓存
     */
    public RemoteCache<String, Object> getRemoteCache() {
        return remoteCache;
    }
    
    /**
     * 关闭缓存管理器
     */
    public void shutdown() {
        try {
            asyncExecutor.shutdown();
            if (remoteCache != null) {
                remoteCache.close();
            }
        } catch (Exception e) {
            logger.error("Failed to shutdown cache manager", e);
        }
    }
}