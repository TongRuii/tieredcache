package com.cache.plugin;

import com.cache.plugin.annotation.TieredCache;
import com.cache.plugin.annotation.CacheStrategy;
import com.cache.plugin.annotation.LocalCache;
import com.cache.plugin.annotation.RemoteCache;
import com.cache.plugin.core.TieredCacheManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.stereotype.Service;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 缓存集成测试
 */
@SpringBootTest
@ActiveProfiles("test")
public class CacheIntegrationTest {
    
    @Autowired
    private TestService testService;
    
    @Autowired
    private TieredCacheManager cacheManager;
    
    @Test
    public void testTieredCacheAnnotation() {
        // 第一次调用，应该执行方法并缓存结果
        String result1 = testService.getTieredCachedValue("test1");
        assertEquals("cached-test1", result1);
        
        // 第二次调用，应该从缓存获取
        String result2 = testService.getTieredCachedValue("test1");
        assertEquals("cached-test1", result2);
    }
    
    @Test
    public void testLocalCacheAnnotation() {
        String result1 = testService.getLocalCachedValue("test2");
        assertEquals("local-test2", result1);
        
        String result2 = testService.getLocalCachedValue("test2");
        assertEquals("local-test2", result2);
    }
    
    @Test
    public void testRemoteCacheAnnotation() {
        String result1 = testService.getRemoteCachedValue("test3");
        assertEquals("remote-test3", result1);
        
        String result2 = testService.getRemoteCachedValue("test3");
        assertEquals("remote-test3", result2);
    }
    
    @Test
    public void testCacheManager() {
        // 测试缓存管理器的基本功能
        String key = "test-key";
        String value = "test-value";
        
        // 存储
        cacheManager.put(key, value, CacheStrategy.LOCAL_FIRST, null);
        
        // 获取
        String cachedValue = cacheManager.get(key, String.class, CacheStrategy.LOCAL_FIRST);
        assertEquals(value, cachedValue);
        
        // 清除
        cacheManager.evict(key, CacheStrategy.LOCAL_FIRST);
        
        // 验证已清除
        String afterEvict = cacheManager.get(key, String.class, CacheStrategy.LOCAL_FIRST);
        assertNull(afterEvict);
    }
    
    @Service
    static class TestService {
        
        @TieredCache(
            local = @LocalCache(maxSize = 100, expireAfterWrite = 60),
            remote = @RemoteCache(ttl = 300),
            key = "'two-level:' + #param",
            strategy = CacheStrategy.LOCAL_FIRST
        )
        public String getTieredCachedValue(String param) {
            return "cached-" + param;
        }
        
        @LocalCache(
            key = "'local:' + #param",
            maxSize = 50,
            expireAfterWrite = 30
        )
        public String getLocalCachedValue(String param) {
            return "local-" + param;
        }
        
        @RemoteCache(
            key = "'remote:' + #param",
            ttl = 600
        )
        public String getRemoteCachedValue(String param) {
            return "remote-" + param;
        }
    }
}