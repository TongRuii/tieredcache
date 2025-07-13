package com.cache.plugin.metrics;

import com.cache.plugin.core.TieredCacheManager;
import com.cache.plugin.local.LocalCache;
import com.cache.plugin.remote.RemoteCache;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Component;

/**
 * 缓存健康检查指示器
 */
@Component
@ConditionalOnClass(HealthIndicator.class)
public class CacheHealthIndicator implements HealthIndicator {
    
    private final TieredCacheManager cacheManager;
    
    public CacheHealthIndicator(TieredCacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }
    
    @Override
    public Health health() {
        try {
            Health.Builder builder = Health.up();
            
            // 检查本地缓存健康状态
            boolean localHealthy = checkLocalCacheHealth(builder);
            
            // 检查远程缓存健康状态
            boolean remoteHealthy = checkRemoteCacheHealth(builder);
            
            // 添加整体统计信息
            addOverallStats(builder);
            
            // 如果任一缓存不健康，整体状态为DOWN
            if (!localHealthy || !remoteHealthy) {
                return builder.down().build();
            }
            
            return builder.up().build();
            
        } catch (Exception e) {
            return Health.down()
                    .withException(e)
                    .withDetail("error", "Failed to check cache health")
                    .build();
        }
    }
    
    /**
     * 检查本地缓存健康状态
     */
    private boolean checkLocalCacheHealth(Health.Builder builder) {
        try {
            LocalCache<String, Object> localCache = cacheManager.getLocalCache();
            
            if (localCache == null) {
                builder.withDetail("local-cache", "NOT_CONFIGURED");
                return false;
            }
            
            boolean available = localCache.isAvailable();
            if (available) {
                builder.withDetail("local-cache", "UP")
                       .withDetail("local-cache-size", localCache.size())
                       .withDetail("local-cache-max-size", localCache.getMaxSize())
                       .withDetail("local-cache-name", localCache.getName());
                
                // 添加统计信息
                try {
                    var stats = localCache.getStats();
                    builder.withDetail("local-cache-hit-rate", String.format("%.2f%%", stats.getHitRate() * 100))
                           .withDetail("local-cache-hit-count", stats.getHitCount())
                           .withDetail("local-cache-miss-count", stats.getMissCount());
                } catch (Exception e) {
                    builder.withDetail("local-cache-stats", "UNAVAILABLE");
                }
            } else {
                builder.withDetail("local-cache", "DOWN");
            }
            
            return available;
            
        } catch (Exception e) {
            builder.withDetail("local-cache", "ERROR")
                   .withDetail("local-cache-error", e.getMessage());
            return false;
        }
    }
    
    /**
     * 检查远程缓存健康状态
     */
    private boolean checkRemoteCacheHealth(Health.Builder builder) {
        try {
            RemoteCache<String, Object> remoteCache = cacheManager.getRemoteCache();
            
            if (remoteCache == null) {
                builder.withDetail("remote-cache", "NOT_CONFIGURED");
                return false;
            }
            
            boolean connected = remoteCache.isConnected();
            if (connected) {
                builder.withDetail("remote-cache", "UP")
                       .withDetail("remote-cache-connected", true)
                       .withDetail("remote-cache-name", remoteCache.getName())
                       .withDetail("remote-cache-default-ttl", remoteCache.getDefaultTtl().toString());
                
                // 添加统计信息
                try {
                    var stats = remoteCache.getStats();
                    builder.withDetail("remote-cache-hit-rate", String.format("%.2f%%", stats.getHitRate() * 100))
                           .withDetail("remote-cache-hit-count", stats.getHitCount())
                           .withDetail("remote-cache-miss-count", stats.getMissCount());
                } catch (Exception e) {
                    builder.withDetail("remote-cache-stats", "UNAVAILABLE");
                }
                
                // 测试连接
                try {
                    String testKey = "health-check-" + System.currentTimeMillis();
                    remoteCache.put(testKey, "test-value");
                    Object testValue = remoteCache.get(testKey);
                    remoteCache.evict(testKey);
                    
                    if ("test-value".equals(testValue)) {
                        builder.withDetail("remote-cache-test", "PASSED");
                    } else {
                        builder.withDetail("remote-cache-test", "FAILED");
                        return false;
                    }
                } catch (Exception e) {
                    builder.withDetail("remote-cache-test", "ERROR")
                           .withDetail("remote-cache-test-error", e.getMessage());
                    return false;
                }
            } else {
                builder.withDetail("remote-cache", "DOWN")
                       .withDetail("remote-cache-connected", false);
            }
            
            return connected;
            
        } catch (Exception e) {
            builder.withDetail("remote-cache", "ERROR")
                   .withDetail("remote-cache-error", e.getMessage());
            return false;
        }
    }
    
    /**
     * 添加整体统计信息
     */
    private void addOverallStats(Health.Builder builder) {
        try {
            builder.withDetail("cache-manager", "ACTIVE")
                   .withDetail("check-time", System.currentTimeMillis());
        } catch (Exception e) {
            builder.withDetail("overall-stats", "ERROR")
                   .withDetail("overall-stats-error", e.getMessage());
        }
    }
}