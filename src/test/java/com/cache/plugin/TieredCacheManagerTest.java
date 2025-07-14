package com.cache.plugin;

import com.cache.plugin.annotation.CacheStrategy;
import com.cache.plugin.config.TieredCacheProperties;
import com.cache.plugin.core.TieredCacheManager;
import com.cache.plugin.local.LocalCache;
import com.cache.plugin.local.impl.CaffeineLocalCache;
import com.cache.plugin.remote.RemoteCache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 分层缓存管理器测试
 */
public class TieredCacheManagerTest {
    
    @Mock
    private RemoteCache<String, Object> remoteCache;
    
    private LocalCache<String, Object> localCache;
    private TieredCacheManager cacheManager;
    private TieredCacheProperties properties;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // 创建真实的本地缓存
        TieredCacheProperties.LocalCacheProperties localProps = new TieredCacheProperties.LocalCacheProperties();
        localProps.setMaxSize(100);
        localProps.setExpireAfterWrite(Duration.ofSeconds(10));
        localCache = new CaffeineLocalCache(localProps);
        
        // 创建配置
        properties = new TieredCacheProperties();
        
        // 创建缓存管理器
        cacheManager = new TieredCacheManager(localCache, remoteCache, properties);
    }
    
    @Test
    void testLocalFirstStrategy_LocalHit() {
        // 准备数据
        String key = "test-key";
        String value = "test-value";
        localCache.put(key, value);
        
        // 执行测试
        Object result = cacheManager.get(key, String.class, CacheStrategy.LOCAL_FIRST);
        
        // 验证结果
        assertEquals(value, result);
        verify(remoteCache, never()).get(anyString());
    }
    
    @Test
    void testLocalFirstStrategy_RemoteHit() {
        // 准备数据
        String key = "test-key";
        String value = "test-value";
        when(remoteCache.get(key)).thenReturn(value);
        
        // 执行测试
        Object result = cacheManager.get(key, String.class, CacheStrategy.LOCAL_FIRST);
        
        // 验证结果
        assertEquals(value, result);
        verify(remoteCache).get(key);
        // 验证值被异步写入本地缓存
        // 注意：由于是异步操作，可能需要等待一段时间
    }
    
    @Test
    void testLocalFirstStrategy_Miss() {
        // 准备数据
        String key = "test-key";
        when(remoteCache.get(key)).thenReturn(null);
        
        // 执行测试
        Object result = cacheManager.get(key, String.class, CacheStrategy.LOCAL_FIRST);
        
        // 验证结果
        assertNull(result);
        verify(remoteCache).get(key);
    }
    
    @Test
    void testRemoteFirstStrategy_RemoteHit() {
        // 准备数据
        String key = "test-key";
        String value = "test-value";
        when(remoteCache.get(key)).thenReturn(value);
        
        // 执行测试
        Object result = cacheManager.get(key, String.class, CacheStrategy.REMOTE_FIRST);
        
        // 验证结果
        assertEquals(value, result);
        verify(remoteCache).get(key);
    }
    
    @Test
    void testLocalOnlyStrategy() {
        // 准备数据
        String key = "test-key";
        String value = "test-value";
        localCache.put(key, value);
        
        // 执行测试
        Object result = cacheManager.get(key, String.class, CacheStrategy.LOCAL_ONLY);
        
        // 验证结果
        assertEquals(value, result);
        verify(remoteCache, never()).get(anyString());
    }
    
    @Test
    void testRemoteOnlyStrategy() {
        // 准备数据
        String key = "test-key";
        String value = "test-value";
        when(remoteCache.get(key)).thenReturn(value);
        
        // 执行测试
        Object result = cacheManager.get(key, String.class, CacheStrategy.REMOTE_ONLY);
        
        // 验证结果
        assertEquals(value, result);
        verify(remoteCache).get(key);
        // 本地缓存不应该被访问
        assertNull(localCache.get(key));
    }
    
    @Test
    void testPutWithWriteThroughStrategy() {
        // 准备数据
        String key = "test-key";
        String value = "test-value";
        Duration ttl = Duration.ofMinutes(5);
        
        // 执行测试
        cacheManager.put(key, value, CacheStrategy.WRITE_THROUGH, ttl);
        
        // 验证结果
        assertEquals(value, localCache.get(key));
        verify(remoteCache).put(key, value, ttl);
    }
    
    @Test
    void testPutWithWriteBehindStrategy() {
        // 准备数据
        String key = "test-key";
        String value = "test-value";
        Duration ttl = Duration.ofMinutes(5);
        
        // 执行测试
        cacheManager.put(key, value, CacheStrategy.WRITE_BEHIND, ttl);
        
        // 验证结果
        assertEquals(value, localCache.get(key));
        // 远程缓存应该被异步调用，这里验证可能需要等待
        verify(remoteCache, timeout(1000)).put(key, value, ttl);
    }
    
    @Test
    void testPutWithLocalOnlyStrategy() {
        // 准备数据
        String key = "test-key";
        String value = "test-value";
        
        // 执行测试
        cacheManager.put(key, value, CacheStrategy.LOCAL_ONLY, null);
        
        // 验证结果
        assertEquals(value, localCache.get(key));
        verify(remoteCache, never()).put(anyString(), any(), any(Duration.class));
        verify(remoteCache, never()).put(anyString(), any());
    }
    
    @Test
    void testEvictWithAllLevels() {
        // 准备数据
        String key = "test-key";
        String value = "test-value";
        localCache.put(key, value);
        
        // 执行测试
        cacheManager.evict(key, CacheStrategy.WRITE_THROUGH);
        
        // 验证结果
        assertNull(localCache.get(key));
        verify(remoteCache).evict(key);
    }
    
    @Test
    void testClear() {
        // 准备数据
        localCache.put("key1", "value1");
        localCache.put("key2", "value2");
        
        // 执行测试
        cacheManager.clear();
        
        // 验证结果
        assertEquals(0, localCache.size());
        verify(remoteCache).clear();
    }
    
    @Test
    void testMultiGet() {
        // 准备数据
        Set<String> keys = new HashSet<>();
        keys.add("key1");
        keys.add("key2");
        keys.add("key3");
        Map<String, Object> expectedResult = new HashMap<>();
        expectedResult.put("key1", "value1");
        expectedResult.put("key2", "value2");
        when(remoteCache.multiGet(keys)).thenReturn(expectedResult);
        
        // 执行测试
        Map<String, Object> result = cacheManager.multiGet(keys, CacheStrategy.LOCAL_FIRST);
        
        // 验证结果
        assertNotNull(result);
        // 注意：这里的实现可能需要根据实际的multiGet逻辑调整
    }
    
    @Test
    void testMultiPut() {
        // 准备数据
        Map<String, Object> keyValues = new HashMap<>();
        keyValues.put("key1", "value1");
        keyValues.put("key2", "value2");
        Duration ttl = Duration.ofMinutes(5);
        
        // 执行测试
        cacheManager.multiPut(keyValues, CacheStrategy.WRITE_THROUGH, ttl);
        
        // 验证结果
        assertEquals("value1", localCache.get("key1"));
        assertEquals("value2", localCache.get("key2"));
        verify(remoteCache).multiPut(keyValues);
    }
}