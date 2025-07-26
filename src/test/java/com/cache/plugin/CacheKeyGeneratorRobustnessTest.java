package com.cache.plugin;

import com.cache.plugin.aspect.CacheKeyGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 缓存键生成器健壮性测试
 */
public class CacheKeyGeneratorRobustnessTest {
    
    private CacheKeyGenerator keyGenerator;
    
    @BeforeEach
    void setUp() {
        keyGenerator = new CacheKeyGenerator();
    }
    
    @Test
    void testNullSpelExpressionResult() throws NoSuchMethodException {
        Method method = TestService.class.getMethod("getUserById", Long.class);
        Object[] args = {123L};
        
        // 测试SpEL表达式结果为null的情况
        String key = keyGenerator.generate("#null", "", method, args, null);
        
        // 应该生成默认键而不是抛出异常
        assertNotNull(key);
        assertFalse(key.isEmpty());
        assertTrue(key.contains("TestService.getUserById"));
    }
    
    @Test
    void testEmptySpelExpressionResult() throws NoSuchMethodException {
        Method method = TestService.class.getMethod("getUserById", Long.class);
        Object[] args = {123L};
        
        // 测试SpEL表达式结果为空字符串的情况
        String key = keyGenerator.generate("''", "", method, args, null);
        
        // 应该生成默认键而不是使用空字符串
        assertNotNull(key);
        assertFalse(key.isEmpty());
        assertTrue(key.contains("TestService.getUserById"));
    }
    
    @Test
    void testSpecialCharacterEscaping() throws NoSuchMethodException {
        Method method = TestService.class.getMethod("getUserById", Long.class);
        Object[] args = {123L};
        
        // 测试特殊字符转义
        String key = keyGenerator.generate("'user:123?*[{test}]'", "", method, args, null);
        
        assertNotNull(key);
        // 验证特殊字符已被转义而不是直接包含在键中
        assertFalse(key.contains("user:123?*[{test}]"));
        // 验证键不为空
        assertFalse(key.isEmpty());
    }
    
    @Test
    void testNullParameterHandling() throws NoSuchMethodException {
        Method method = TestService.class.getMethod("getUserById", Long.class);
        Object[] args = {null};
        
        // 测试参数为null的情况
        String key = keyGenerator.generate("#p0", "", method, args, null);
        
        // 当使用#p0表达式且参数为null时，SpEL表达式会返回null，然后会使用默认键生成策略
        // 默认键应该是类名.方法名(参数)的形式
        assertNotNull(key);
        assertEquals("TestService.getUserById(null)", key);
    }

    /**
     * 测试用的服务类
     */
    static class TestService {
        public String getUserById(Long id) {
            return "user-" + id;
        }
    }
}