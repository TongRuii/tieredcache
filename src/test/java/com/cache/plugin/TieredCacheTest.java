package com.cache.plugin;

import com.cache.plugin.config.TieredCacheAutoConfiguration;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * 分层缓存测试类
 */
@SpringBootTest(classes = {
    TieredCacheAutoConfiguration.class,
    TieredCacheTest.TestConfiguration.class
})
public class TieredCacheTest {
    
    @Test
    public void contextLoads() {
        // 基础上下文加载测试
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
        public org.springframework.data.redis.core.RedisTemplate<String, Object> redisTemplate() {
            // 创建一个Mock RedisTemplate用于测试
            org.springframework.data.redis.core.RedisTemplate<String, Object> template = 
                new org.springframework.data.redis.core.RedisTemplate<>();
            // 设置一个Mock连接工厂，避免真实Redis连接
            template.setConnectionFactory(org.mockito.Mockito.mock(
                org.springframework.data.redis.connection.RedisConnectionFactory.class));
            template.setKeySerializer(new org.springframework.data.redis.serializer.StringRedisSerializer());
            template.setValueSerializer(new org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer());
            template.afterPropertiesSet();
            return template;
        }
    }
}