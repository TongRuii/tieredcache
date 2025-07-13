package com.cache.plugin.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 本地缓存注解
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface LocalCache {
    
    /**
     * 缓存键，支持SpEL表达式
     */
    String key() default "";
    
    /**
     * 缓存条件，支持SpEL表达式
     */
    String condition() default "";
    
    /**
     * 最大缓存大小
     */
    long maxSize() default 1000;
    
    /**
     * 写入后过期时间（秒）
     */
    int expireAfterWrite() default 300;
    
    /**
     * 访问后过期时间（秒）
     */
    int expireAfterAccess() default 600;
    
    /**
     * 缓存名称
     */
    String cacheName() default "default";
    
    /**
     * 是否同步加载
     */
    boolean sync() default false;
}