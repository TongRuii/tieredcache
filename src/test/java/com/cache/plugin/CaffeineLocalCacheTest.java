package com.cache.plugin;

import com.cache.plugin.config.TieredCacheProperties;
import com.cache.plugin.local.impl.CaffeineLocalCache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Caffeine本地缓存测试
 */
public class CaffeineLocalCacheTest {
    
    private CaffeineLocalCache cache;
    private TieredCacheProperties.LocalCacheProperties properties;
    
    @BeforeEach
    void setUp() {
        properties = new TieredCacheProperties.LocalCacheProperties();
        properties.setMaxSize(100);
        properties.setExpireAfterWrite(Duration.ofSeconds(10));
        properties.setExpireAfterAccess(Duration.ofSeconds(20));
        properties.setRecordStats(true);
        
        cache = new CaffeineLocalCache(properties);
    }
    
    @Test
    void testPutAndGet() {
        // 测试存储和获取
        String key = "test-key";
        String value = "test-value";
        
        cache.put(key, value);
        Object result = cache.get(key);
        
        assertEquals(value, result);
    }
    
    @Test
    void testContainsKey() {
        String key = "test-key";
        String value = "test-value";
        
        assertFalse(cache.containsKey(key));
        
        cache.put(key, value);
        assertTrue(cache.containsKey(key));
    }
    
    @Test
    void testEvict() {
        String key = "test-key";
        String value = "test-value";
        
        cache.put(key, value);
        assertTrue(cache.containsKey(key));
        
        cache.evict(key);
        assertFalse(cache.containsKey(key));
    }
    
    @Test
    void testClear() {
        cache.put("key1", "value1");
        cache.put("key2", "value2");
        
        assertEquals(2, cache.size());
        
        cache.clear();
        assertEquals(0, cache.size());
    }
    
    @Test
    void testMultiGet() {
        Map<String, Object> data = new HashMap<>();
        data.put("key1", "value1");
        data.put("key2", "value2");
        data.put("key3", "value3");
        
        cache.multiPut(data);
        
        Set<String> keys = Set.of("key1", "key2", "key4");
        Map<String, Object> result = cache.multiGet(keys);
        
        assertEquals(2, result.size());
        assertEquals("value1", result.get("key1"));
        assertEquals("value2", result.get("key2"));
        assertNull(result.get("key4"));
    }
    
    @Test
    void testMultiPut() {
        Map<String, Object> data = new HashMap<>();
        data.put("key1", "value1");
        data.put("key2", "value2");
        
        cache.multiPut(data);
        
        assertEquals("value1", cache.get("key1"));
        assertEquals("value2", cache.get("key2"));
    }
    
    @Test
    void testMultiEvict() {
        cache.put("key1", "value1");
        cache.put("key2", "value2");
        cache.put("key3", "value3");
        
        Set<String> keysToEvict = Set.of("key1", "key3");
        cache.multiEvict(keysToEvict);
        
        assertFalse(cache.containsKey("key1"));
        assertTrue(cache.containsKey("key2"));
        assertFalse(cache.containsKey("key3"));
    }
    
    @Test
    void testStats() {
        // 执行一些操作
        cache.put("key1", "value1");
        cache.get("key1"); // hit
        cache.get("key2"); // miss
        
        var stats = cache.getStats();
        assertNotNull(stats);
        assertTrue(stats.getHitCount() >= 1);
        assertTrue(stats.getMissCount() >= 1);
    }
    
    @Test
    void testIsAvailable() {
        assertTrue(cache.isAvailable());
    }
    
    @Test
    void testGetName() {
        assertNotNull(cache.getName());
        assertTrue(cache.getName().contains("caffeine"));
    }
    
    @Test
    void testMaxSize() {
        assertEquals(100, cache.getMaxSize());
    }
    
    @Test
    void testNullValues() {
        // 测试null值处理
        cache.put("null-key", null);
        assertNull(cache.get("null-key"));
        
        cache.put(null, "value");
        // 应该不会抛出异常，但也不会存储
        
        assertNull(cache.get(null));
    }
}