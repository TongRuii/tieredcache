package com.cache.plugin.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 远程缓存注解
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RemoteCache {
    
    /**
     * 缓存键，支持SpEL表达式
     */
    String key() default "";
    
    /**
     * 缓存条件，支持SpEL表达式
     */
    String condition() default "";
    
    /**
     * 过期时间（秒）
     */
    int ttl() default 3600;
    
    /**
     * 缓存名称空间
     */
    String namespace() default "default";
    
    /**
     * 是否同步操作
     */
    boolean sync() default false;
    
    /**
     * 缓存名称
     */
    String cacheName() default "default";
}