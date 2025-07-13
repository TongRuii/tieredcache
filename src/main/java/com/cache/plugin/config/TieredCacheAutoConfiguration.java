package com.cache.plugin.config;

import com.cache.plugin.core.TieredCacheManager;
import com.cache.plugin.local.LocalCache;
import com.cache.plugin.local.impl.CaffeineLocalCache;
import com.cache.plugin.remote.RemoteCache;
import com.cache.plugin.remote.impl.RedisRemoteCache;
import com.cache.plugin.aspect.CacheAspect;
import com.cache.plugin.metrics.CacheMetrics;
import com.cache.plugin.sync.CacheSyncManager;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.data.redis.core.RedisTemplate;
import io.micrometer.core.instrument.MeterRegistry;

/**
 * 分层缓存自动配置类
 */
@Configuration
@EnableAspectJAutoProxy
@EnableConfigurationProperties(TieredCacheProperties.class)
@ConditionalOnProperty(prefix = "tiered-cache", name = "enabled", havingValue = "true", matchIfMissing = true)
public class TieredCacheAutoConfiguration {
    
    /**
     * 本地缓存配置
     */
    @Configuration
    @ConditionalOnProperty(prefix = "two-level-cache.local", name = "provider", havingValue = "caffeine", matchIfMissing = true)
    @ConditionalOnClass(name = "com.github.benmanes.caffeine.cache.Caffeine")
    static class LocalCacheConfiguration {
        
        @Bean
        @ConditionalOnMissingBean
        public LocalCache<String, Object> localCache(TieredCacheProperties properties) {
            return new CaffeineLocalCache(properties.getLocal());
        }
    }
    
    /**
     * 远程缓存配置
     */
    @Configuration
    @ConditionalOnProperty(prefix = "two-level-cache.remote", name = "provider", havingValue = "redis", matchIfMissing = true)
    @ConditionalOnClass(RedisTemplate.class)
    static class RemoteCacheConfiguration {
        
        @Bean
        @ConditionalOnMissingBean
        public RemoteCache<String, Object> remoteCache(TieredCacheProperties properties,
                                                       RedisTemplate<String, Object> redisTemplate) {
            return new RedisRemoteCache(properties.getRemote(), redisTemplate);
        }
    }
    
    /**
     * 缓存管理器配置
     */
    @Bean
    @ConditionalOnMissingBean
    public TieredCacheManager twoLevelCacheManager(
            LocalCache<String, Object> localCache,
            RemoteCache<String, Object> remoteCache,
            TieredCacheProperties properties) {
        return new TieredCacheManager(localCache, remoteCache, properties);
    }
    
    /**
     * 缓存切面配置
     */
    @Bean
    @ConditionalOnMissingBean
    public CacheAspect cacheAspect(TieredCacheManager cacheManager) {
        return new CacheAspect(cacheManager);
    }
    
    /**
     * 缓存指标配置
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(MeterRegistry.class)
    @ConditionalOnProperty(prefix = "two-level-cache.monitoring.metrics", name = "enabled", havingValue = "true", matchIfMissing = true)
    public CacheMetrics cacheMetrics(MeterRegistry meterRegistry) {
        return new CacheMetrics(meterRegistry);
    }
    
    /**
     * 缓存同步管理器配置
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "two-level-cache.sync", name = "enabled", havingValue = "true", matchIfMissing = true)
    public CacheSyncManager cacheSyncManager(
            LocalCache<String, Object> localCache,
            RemoteCache<String, Object> remoteCache,
            TieredCacheProperties properties) {
        return new CacheSyncManager(localCache, remoteCache, properties.getSync());
    }
}