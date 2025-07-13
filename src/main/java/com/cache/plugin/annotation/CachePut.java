package com.cache.plugin.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 缓存更新注解
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface CachePut {
    
    /**
     * 缓存键，支持SpEL表达式
     */
    String key() default "";
    
    /**
     * 缓存条件，支持SpEL表达式
     */
    String condition() default "";
    
    /**
     * 缓存名称
     */
    String cacheName() default "default";
    
    /**
     * 缓存层级
     */
    CacheLevel level() default CacheLevel.ALL;
    
    /**
     * 过期时间（秒），仅对远程缓存有效
     */
    int ttl() default -1;
}