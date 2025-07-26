package com.cache.plugin;

import com.cache.plugin.config.TieredCacheAutoConfiguration;
import com.cache.plugin.config.TieredCacheProperties;
import com.cache.plugin.core.TieredCacheManager;
import com.cache.plugin.local.LocalCache;
import com.cache.plugin.local.impl.CaffeineLocalCache;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.RedisTemplate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 远程缓存禁用测试
 */
@SpringBootTest(
    classes = {
        RemoteCacheDisabledTest.TestConfiguration.class,
        TieredCacheAutoConfiguration.class
    },
    properties = {
        "tiered-cache.remote.enabled=false"
    }
)
public class RemoteCacheDisabledTest {

    @Autowired
    private TieredCacheManager cacheManager;

    @Test
    public void testCacheManagerWithRemoteDisabled() {
        // 验证缓存管理器已正确创建
        assertNotNull(cacheManager);
        
        // 验证本地缓存存在
        assertNotNull(cacheManager.getLocalCache());
        
        // 验证远程缓存为null
        assertNull(cacheManager.getRemoteCache());
        
        // 测试本地缓存功能
        String key = "test-key";
        String value = "test-value";
        
        // 存储值
        cacheManager.put(key, value, com.cache.plugin.annotation.CacheStrategy.LOCAL_FIRST, null);
        
        // 获取值
        String result = cacheManager.get(key, String.class, com.cache.plugin.annotation.CacheStrategy.LOCAL_FIRST);
        assertEquals(value, result);
    }

    @Test
    public void testRemoteOnlyStrategyFallsBackToLocal() {
        String key = "remote-test-key";
        String value = "remote-test-value";
        
        // 当远程缓存禁用时，REMOTE_ONLY策略应该降级到本地缓存
        cacheManager.put(key, value, com.cache.plugin.annotation.CacheStrategy.REMOTE_ONLY, null);
        
        // 值应该存储在本地缓存中
        String result = cacheManager.get(key, String.class, com.cache.plugin.annotation.CacheStrategy.REMOTE_ONLY);
        assertEquals(value, result);
    }

    @Configuration
    static class TestConfiguration {
        
        @Bean
        @Primary
        public MeterRegistry meterRegistry() {
            return new SimpleMeterRegistry();
        }
        
        @Bean
        @Primary
        public LocalCache<String, Object> localCache(TieredCacheProperties properties) {
            return new CaffeineLocalCache(properties.getLocal());
        }
    }
}