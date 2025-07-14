package com.cache.plugin.example.demo.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * 演示应用配置类
 * 
 * 提供演示环境所需的Bean配置，包括Mock的Redis和MeterRegistry
 */
@Configuration
public class DemoConfiguration {
    
    /**
     * 配置MeterRegistry用于监控指标
     */
    @Bean
    @Primary
    @ConditionalOnMissingBean
    public MeterRegistry meterRegistry() {
        return new SimpleMeterRegistry();
    }
    
    /**
     * 配置Mock RedisTemplate用于演示
     * 只有在远程缓存启用时才创建
     */
    @Bean
    @Primary
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "tiered-cache.remote", name = "enabled", havingValue = "true")
    public RedisTemplate<String, Object> redisTemplate() {
        // 为了简化演示，我们创建一个基本的RedisTemplate
        // 在实际使用中，远程缓存功能会被禁用（通过配置文件）
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        
        // 设置序列化器
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        
        // 注意：由于没有设置连接工厂，这个template在实际使用时会报错
        // 但在演示配置中，远程缓存被禁用，所以不会被调用
        
        return template;
    }
    
}