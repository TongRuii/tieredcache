package com.cache.plugin;

import com.cache.plugin.aspect.CacheKeyGenerator;

import java.lang.reflect.Method;

public class DebugCacheKeyGenerator {
    public static void main(String[] args) throws NoSuchMethodException {
        CacheKeyGenerator keyGenerator = new CacheKeyGenerator();
        
        Method method = TestService.class.getMethod("getUserById", Long.class);
        Object[] methodArgs = {null};
        
        // 测试参数为null的情况
        String key = keyGenerator.generate("#p0", "", method, methodArgs, null);
        
        System.out.println("Generated key: '" + key + "'");
        System.out.println("Key is null: " + (key == null));
        System.out.println("Key equals 'null': " + "null".equals(key));
    }
    
    static class TestService {
        public String getUserById(Long id) {
            return "user-" + id;
        }
    }
}