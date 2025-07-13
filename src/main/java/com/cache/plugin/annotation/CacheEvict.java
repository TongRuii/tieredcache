package com.cache.plugin.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 缓存清除注解
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface CacheEvict {
    
    /**
     * 缓存键，支持SpEL表达式
     */
    String key() default "";
    
    /**
     * 缓存条件，支持SpEL表达式
     */
    String condition() default "";
    
    /**
     * 是否清除所有缓存
     */
    boolean allEntries() default false;
    
    /**
     * 是否在方法执行前清除缓存
     */
    boolean beforeInvocation() default false;
    
    /**
     * 缓存名称
     */
    String cacheName() default "default";
    
    /**
     * 缓存层级
     */
    CacheLevel level() default CacheLevel.ALL;
}