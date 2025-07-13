package com.cache.plugin.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 分层缓存注解
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface TieredCache {
    
    /**
     * 本地缓存配置
     */
    LocalCache local() default @LocalCache;
    
    /**
     * 远程缓存配置
     */
    RemoteCache remote() default @RemoteCache;
    
    /**
     * 缓存键生成器
     */
    String keyGenerator() default "";
    
    /**
     * 缓存模式
     */
    CacheMode mode() default CacheMode.READ_WRITE;
    
    /**
     * 缓存策略
     */
    CacheStrategy strategy() default CacheStrategy.LOCAL_FIRST;
    
    /**
     * 缓存键，支持SpEL表达式
     */
    String key() default "";
    
    /**
     * 缓存条件，支持SpEL表达式
     */
    String condition() default "";
}