package com.cache.plugin;

import com.cache.plugin.annotation.CacheStrategy;
import com.cache.plugin.annotation.TieredCache;
import com.cache.plugin.annotation.LocalCache;
import com.cache.plugin.annotation.RemoteCache;
import com.cache.plugin.aspect.CacheAspect;
import com.cache.plugin.core.TieredCacheManager;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 缓存切面测试
 */
public class CacheAspectTest {
    
    @Mock
    private TieredCacheManager cacheManager;
    
    @Mock
    private ProceedingJoinPoint joinPoint;
    
    @Mock
    private MethodSignature methodSignature;
    
    private CacheAspect cacheAspect;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        cacheAspect = new CacheAspect(cacheManager);
        
        when(joinPoint.getSignature()).thenReturn(methodSignature);
    }
    
    @Test
    void testTieredCacheHit() throws Throwable {
        // 准备数据
        Method method = TestService.class.getMethod("getUserById", Long.class);
        Object[] args = {123L};
        String expectedValue = "cached-user";
        
        TieredCache annotation = createTieredCacheAnnotation();
        
        when(methodSignature.getMethod()).thenReturn(method);
        when(joinPoint.getArgs()).thenReturn(args);
        when(cacheManager.get(anyString(), eq(String.class), eq(CacheStrategy.LOCAL_FIRST)))
                .thenReturn(expectedValue);
        
        // 执行测试
        Object result = cacheAspect.handleTieredCache(joinPoint, annotation);
        
        // 验证结果
        assertEquals(expectedValue, result);
        verify(joinPoint, never()).proceed();
        verify(cacheManager).get(anyString(), eq(String.class), eq(CacheStrategy.LOCAL_FIRST));
    }
    
    @Test
    void testTieredCacheMiss() throws Throwable {
        // 准备数据
        Method method = TestService.class.getMethod("getUserById", Long.class);
        Object[] args = {123L};
        String methodResult = "method-result";
        
        TieredCache annotation = createTieredCacheAnnotation();
        
        when(methodSignature.getMethod()).thenReturn(method);
        when(joinPoint.getArgs()).thenReturn(args);
        when(cacheManager.get(anyString(), eq(String.class), eq(CacheStrategy.LOCAL_FIRST)))
                .thenReturn(null);
        when(joinPoint.proceed()).thenReturn(methodResult);
        
        // 执行测试
        Object result = cacheAspect.handleTieredCache(joinPoint, annotation);
        
        // 验证结果
        assertEquals(methodResult, result);
        verify(joinPoint).proceed();
        verify(cacheManager).get(anyString(), eq(String.class), eq(CacheStrategy.LOCAL_FIRST));
        verify(cacheManager).put(anyString(), eq(methodResult), eq(CacheStrategy.LOCAL_FIRST), any());
    }
    
    @Test
    void testLocalCacheAnnotation() throws Throwable {
        // 准备数据
        Method method = TestService.class.getMethod("getUserById", Long.class);
        Object[] args = {123L};
        String methodResult = "method-result";
        
        LocalCache annotation = createLocalCacheAnnotation();
        
        when(methodSignature.getMethod()).thenReturn(method);
        when(joinPoint.getArgs()).thenReturn(args);
        when(cacheManager.get(anyString(), eq(String.class), eq(CacheStrategy.LOCAL_ONLY)))
                .thenReturn(null);
        when(joinPoint.proceed()).thenReturn(methodResult);
        
        // 执行测试
        Object result = cacheAspect.handleLocalCache(joinPoint, annotation);
        
        // 验证结果
        assertEquals(methodResult, result);
        verify(cacheManager).put(anyString(), eq(methodResult), eq(CacheStrategy.LOCAL_ONLY), isNull());
    }
    
    @Test
    void testRemoteCacheAnnotation() throws Throwable {
        // 准备数据
        Method method = TestService.class.getMethod("getUserById", Long.class);
        Object[] args = {123L};
        String methodResult = "method-result";
        
        RemoteCache annotation = createRemoteCacheAnnotation();
        
        when(methodSignature.getMethod()).thenReturn(method);
        when(joinPoint.getArgs()).thenReturn(args);
        when(cacheManager.get(anyString(), eq(String.class), eq(CacheStrategy.REMOTE_ONLY)))
                .thenReturn(null);
        when(joinPoint.proceed()).thenReturn(methodResult);
        
        // 执行测试
        Object result = cacheAspect.handleRemoteCache(joinPoint, annotation);
        
        // 验证结果
        assertEquals(methodResult, result);
        verify(cacheManager).put(anyString(), eq(methodResult), eq(CacheStrategy.REMOTE_ONLY), any());
    }
    
    /**
     * 创建TieredCache注解的模拟
     */
    private TieredCache createTieredCacheAnnotation() {
        return new TieredCache() {
            @Override
            public Class<? extends java.lang.annotation.Annotation> annotationType() {
                return TieredCache.class;
            }
            
            @Override
            public LocalCache local() {
                return createLocalCacheAnnotation();
            }
            
            @Override
            public RemoteCache remote() {
                return createRemoteCacheAnnotation();
            }
            
            @Override
            public String keyGenerator() {
                return "";
            }
            
            @Override
            public com.cache.plugin.annotation.CacheMode mode() {
                return com.cache.plugin.annotation.CacheMode.READ_WRITE;
            }
            
            @Override
            public CacheStrategy strategy() {
                return CacheStrategy.LOCAL_FIRST;
            }
            
            @Override
            public String key() {
                return "'user:' + #p0";
            }
            
            @Override
            public String condition() {
                return "";
            }
        };
    }
    
    /**
     * 创建LocalCache注解的模拟
     */
    private LocalCache createLocalCacheAnnotation() {
        return new LocalCache() {
            @Override
            public Class<? extends java.lang.annotation.Annotation> annotationType() {
                return LocalCache.class;
            }
            
            @Override
            public String key() {
                return "'user:' + #p0";
            }
            
            @Override
            public String condition() {
                return "";
            }
            
            @Override
            public long maxSize() {
                return 1000;
            }
            
            @Override
            public int expireAfterWrite() {
                return 300;
            }
            
            @Override
            public int expireAfterAccess() {
                return 600;
            }
            
            @Override
            public String cacheName() {
                return "default";
            }
            
            @Override
            public boolean sync() {
                return false;
            }
        };
    }
    
    /**
     * 创建RemoteCache注解的模拟
     */
    private RemoteCache createRemoteCacheAnnotation() {
        return new RemoteCache() {
            @Override
            public Class<? extends java.lang.annotation.Annotation> annotationType() {
                return RemoteCache.class;
            }
            
            @Override
            public String key() {
                return "'user:' + #p0";
            }
            
            @Override
            public String condition() {
                return "";
            }
            
            @Override
            public int ttl() {
                return 3600;
            }
            
            @Override
            public String namespace() {
                return "default";
            }
            
            @Override
            public boolean sync() {
                return false;
            }
            
            @Override
            public String cacheName() {
                return "default";
            }
        };
    }
    
    /**
     * 测试用的服务类
     */
    public static class TestService {
        public String getUserById(Long id) {
            return "user-" + id;
        }
    }
}