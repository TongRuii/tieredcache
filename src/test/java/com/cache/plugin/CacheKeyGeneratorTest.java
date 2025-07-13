package com.cache.plugin;

import com.cache.plugin.aspect.CacheKeyGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 缓存键生成器测试
 */
public class CacheKeyGeneratorTest {
    
    private CacheKeyGenerator keyGenerator;
    
    @BeforeEach
    void setUp() {
        keyGenerator = new CacheKeyGenerator();
    }
    
    @Test
    void testDefaultKeyGeneration() throws NoSuchMethodException {
        Method method = TestService.class.getMethod("getUserById", Long.class);
        Object[] args = {123L};
        
        String key = keyGenerator.generate("", "", method, args, null);
        
        assertEquals("TestService.getUserById(123)", key);
    }
    
    @Test
    void testSpELKeyGeneration() throws NoSuchMethodException {
        Method method = TestService.class.getMethod("getUserById", Long.class);
        Object[] args = {123L};
        
        String key = keyGenerator.generate("'user:' + #p0", "", method, args, null);
        
        assertEquals("user:123", key);
    }
    
    @Test
    void testSpELWithMethodName() throws NoSuchMethodException {
        Method method = TestService.class.getMethod("getUserById", Long.class);
        Object[] args = {123L};
        
        String key = keyGenerator.generate("#methodName + ':' + #p0", "", method, args, null);
        
        assertEquals("getUserById:123", key);
    }
    
    @Test
    void testSpELWithClassName() throws NoSuchMethodException {
        Method method = TestService.class.getMethod("getUserById", Long.class);
        Object[] args = {123L};
        
        String key = keyGenerator.generate("#className + '.' + #methodName + ':' + #p0", "", method, args, null);
        
        assertEquals("TestService.getUserById:123", key);
    }
    
    @Test
    void testSpELWithResult() throws NoSuchMethodException {
        Method method = TestService.class.getMethod("getUserById", Long.class);
        Object[] args = {123L};
        String result = "user-data";
        
        String key = keyGenerator.generate("'result:' + #result", "", method, args, result);
        
        assertEquals("result:user-data", key);
    }
    
    @Test
    void testMultipleParameters() throws NoSuchMethodException {
        Method method = TestService.class.getMethod("getUserByNameAndAge", String.class, Integer.class);
        Object[] args = {"John", 25};
        
        String key = keyGenerator.generate("'user:' + #p0 + ':' + #p1", "", method, args, null);
        
        assertEquals("user:John:25", key);
    }
    
    @Test
    void testDefaultKeyWithMultipleParams() throws NoSuchMethodException {
        Method method = TestService.class.getMethod("getUserByNameAndAge", String.class, Integer.class);
        Object[] args = {"John", 25};
        
        String key = keyGenerator.generate("", "", method, args, null);
        
        assertEquals("TestService.getUserByNameAndAge(John,25)", key);
    }
    
    @Test
    void testDefaultKeyWithNoParams() throws NoSuchMethodException {
        Method method = TestService.class.getMethod("getAllUsers");
        Object[] args = {};
        
        String key = keyGenerator.generate("", "", method, args, null);
        
        assertEquals("TestService.getAllUsers()", key);
    }
    
    @Test
    void testDefaultKeyWithNullParam() throws NoSuchMethodException {
        Method method = TestService.class.getMethod("getUserById", Long.class);
        Object[] args = {null};
        
        String key = keyGenerator.generate("", "", method, args, null);
        
        assertEquals("TestService.getUserById(null)", key);
    }
    
    @Test
    void testInvalidSpELExpression() throws NoSuchMethodException {
        Method method = TestService.class.getMethod("getUserById", Long.class);
        Object[] args = {123L};
        
        // 无效的SpEL表达式应该回退到默认策略
        String key = keyGenerator.generate("'user:' + #invalidVar", "", method, args, null);
        
        assertEquals("TestService.getUserById(123)", key);
    }
    
    /**
     * 测试用的服务类
     */
    public static class TestService {
        public String getUserById(Long id) {
            return "user-" + id;
        }
        
        public String getUserByNameAndAge(String name, Integer age) {
            return name + "-" + age;
        }
        
        public String getAllUsers() {
            return "all-users";
        }
    }
}