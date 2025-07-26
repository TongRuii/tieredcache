package com.cache.plugin;

import com.cache.plugin.aspect.CacheKeyGenerator;

import java.lang.reflect.Method;

/**
 * Debug key generation test class
 */
public class KeyGenerationDebugTest {
    public static void main(String[] args) throws NoSuchMethodException {
        CacheKeyGenerator keyGenerator = new CacheKeyGenerator();
        
        Method method = TestService.class.getMethod("getUserById", Long.class);
        Object[] argsArray = {123L};
        
        String key = keyGenerator.generate("#className + '.' + #methodName + ':' + #p0", "", method, argsArray, null);
        
        System.out.println("Generated key: '" + key + "'");
        System.out.println("Expected key: 'TestService_getUserById_123'");
    }
    
    /**
     * Test service class
     */
    public static class TestService {
        public String getUserById(Long id) {
            return "user-" + id;
        }
    }
}