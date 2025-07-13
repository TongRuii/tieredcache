package com.cache.plugin.aspect;

import com.cache.plugin.annotation.*;
import com.cache.plugin.core.TieredCacheManager;
import com.cache.plugin.exception.CacheException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;

import java.lang.reflect.Method;
import java.time.Duration;

/**
 * 缓存切面处理器
 */
@Aspect
@Order(1)
public class CacheAspect {
    
    private static final Logger logger = LoggerFactory.getLogger(CacheAspect.class);
    
    private final TieredCacheManager cacheManager;
    private final CacheKeyGenerator keyGenerator;
    private final CacheConditionEvaluator conditionEvaluator;
    
    public CacheAspect(TieredCacheManager cacheManager) {
        this.cacheManager = cacheManager;
        this.keyGenerator = new CacheKeyGenerator();
        this.conditionEvaluator = new CacheConditionEvaluator();
    }
    
    /**
     * 处理分层缓存注解
     */
    @Around("@annotation(twoLevelCache)")
    public Object handleTieredCache(ProceedingJoinPoint joinPoint, TieredCache twoLevelCache) throws Throwable {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        Object[] args = joinPoint.getArgs();
        
        // 生成缓存键
        String key = generateCacheKey(twoLevelCache.key(), twoLevelCache.keyGenerator(), method, args);
        
        // 评估缓存条件
        if (!evaluateCondition(twoLevelCache.condition(), method, args)) {
            logger.debug("Cache condition not met for key: {}, executing method directly", key);
            return joinPoint.proceed();
        }
        
        // 根据缓存模式处理
        switch (twoLevelCache.mode()) {
            case READ_ONLY:
                return handleReadOnlyCache(joinPoint, key, twoLevelCache.strategy(), method.getReturnType());
            case WRITE_ONLY:
                return handleWriteOnlyCache(joinPoint, key, twoLevelCache.strategy(), getTtl(twoLevelCache));
            case READ_WRITE:
            default:
                return handleReadWriteCache(joinPoint, key, twoLevelCache.strategy(), getTtl(twoLevelCache), method.getReturnType());
        }
    }
    
    /**
     * 处理本地缓存注解
     */
    @Around("@annotation(localCache)")
    public Object handleLocalCache(ProceedingJoinPoint joinPoint, LocalCache localCache) throws Throwable {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        Object[] args = joinPoint.getArgs();
        
        String key = generateCacheKey(localCache.key(), "", method, args);
        
        if (!evaluateCondition(localCache.condition(), method, args)) {
            return joinPoint.proceed();
        }
        
        return handleReadWriteCache(joinPoint, key, CacheStrategy.LOCAL_ONLY, null, method.getReturnType());
    }
    
    /**
     * 处理远程缓存注解
     */
    @Around("@annotation(remoteCache)")
    public Object handleRemoteCache(ProceedingJoinPoint joinPoint, RemoteCache remoteCache) throws Throwable {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        Object[] args = joinPoint.getArgs();
        
        String key = generateCacheKey(remoteCache.key(), "", method, args);
        
        if (!evaluateCondition(remoteCache.condition(), method, args)) {
            return joinPoint.proceed();
        }
        
        Duration ttl = Duration.ofSeconds(remoteCache.ttl());
        return handleReadWriteCache(joinPoint, key, CacheStrategy.REMOTE_ONLY, ttl, method.getReturnType());
    }
    
    /**
     * 处理缓存清除注解
     */
    @Around("@annotation(cacheEvict)")
    public Object handleCacheEvict(ProceedingJoinPoint joinPoint, CacheEvict cacheEvict) throws Throwable {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        Object[] args = joinPoint.getArgs();
        
        // 如果是方法执行前清除缓存
        if (cacheEvict.beforeInvocation()) {
            performEviction(cacheEvict, method, args);
        }
        
        Object result;
        try {
            result = joinPoint.proceed();
        } catch (Exception e) {
            // 如果方法执行失败且设置了beforeInvocation=false，则不清除缓存
            if (cacheEvict.beforeInvocation()) {
                throw e;
            } else {
                logger.warn("Method execution failed, skipping cache eviction for safety");
                throw e;
            }
        }
        
        // 如果是方法执行后清除缓存
        if (!cacheEvict.beforeInvocation()) {
            performEviction(cacheEvict, method, args);
        }
        
        return result;
    }
    
    /**
     * 处理缓存更新注解
     */
    @Around("@annotation(cachePut)")
    public Object handleCachePut(ProceedingJoinPoint joinPoint, CachePut cachePut) throws Throwable {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        Object[] args = joinPoint.getArgs();
        
        // 先执行方法获取结果
        Object result = joinPoint.proceed();
        
        // 评估缓存条件
        if (!evaluateCondition(cachePut.condition(), method, args, result)) {
            return result;
        }
        
        // 生成缓存键
        String key = generateCacheKey(cachePut.key(), "", method, args, result);
        
        // 根据缓存层级更新缓存
        CacheStrategy strategy = determineCacheStrategy(cachePut.level());
        Duration ttl = cachePut.ttl() > 0 ? Duration.ofSeconds(cachePut.ttl()) : null;
        
        try {
            cacheManager.put(key, result, strategy, ttl);
            logger.debug("Updated cache for key: {} with strategy: {}", key, strategy);
        } catch (Exception e) {
            logger.error("Failed to update cache for key: {}", key, e);
            // 缓存更新失败不影响方法返回结果
        }
        
        return result;
    }
    
    /**
     * 处理只读缓存
     */
    private Object handleReadOnlyCache(ProceedingJoinPoint joinPoint, String key, CacheStrategy strategy, Class<?> returnType) throws Throwable {
        Object cachedValue = cacheManager.get(key, returnType, strategy);
        if (cachedValue != null) {
            logger.debug("Cache hit for key: {} with strategy: {}", key, strategy);
            return cachedValue;
        }
        
        logger.debug("Cache miss for key: {}, executing method", key);
        return joinPoint.proceed();
    }
    
    /**
     * 处理只写缓存
     */
    private Object handleWriteOnlyCache(ProceedingJoinPoint joinPoint, String key, CacheStrategy strategy, Duration ttl) throws Throwable {
        Object result = joinPoint.proceed();
        
        try {
            cacheManager.put(key, result, strategy, ttl);
            logger.debug("Cached result for key: {} with strategy: {}", key, strategy);
        } catch (Exception e) {
            logger.error("Failed to cache result for key: {}", key, e);
            // 缓存失败不影响方法返回结果
        }
        
        return result;
    }
    
    /**
     * 处理读写缓存
     */
    private Object handleReadWriteCache(ProceedingJoinPoint joinPoint, String key, CacheStrategy strategy, Duration ttl, Class<?> returnType) throws Throwable {
        // 先尝试从缓存获取
        Object cachedValue = cacheManager.get(key, returnType, strategy);
        if (cachedValue != null) {
            logger.debug("Cache hit for key: {} with strategy: {}", key, strategy);
            return cachedValue;
        }
        
        // 缓存未命中，执行方法
        logger.debug("Cache miss for key: {}, executing method", key);
        Object result = joinPoint.proceed();
        
        // 将结果存入缓存
        if (result != null) {
            try {
                cacheManager.put(key, result, strategy, ttl);
                logger.debug("Cached result for key: {} with strategy: {}", key, strategy);
            } catch (Exception e) {
                logger.error("Failed to cache result for key: {}", key, e);
                // 缓存失败不影响方法返回结果
            }
        }
        
        return result;
    }
    
    /**
     * 执行缓存清除
     */
    private void performEviction(CacheEvict cacheEvict, Method method, Object[] args) {
        try {
            if (cacheEvict.allEntries()) {
                // 清除所有缓存
                cacheManager.clear();
                logger.info("Cleared all cache entries");
            } else {
                // 清除指定键的缓存
                String key = generateCacheKey(cacheEvict.key(), "", method, args);
                if (!evaluateCondition(cacheEvict.condition(), method, args)) {
                    return;
                }
                
                CacheStrategy strategy = determineCacheStrategy(cacheEvict.level());
                cacheManager.evict(key, strategy);
                logger.debug("Evicted cache for key: {} with strategy: {}", key, strategy);
            }
        } catch (Exception e) {
            logger.error("Failed to evict cache", e);
            throw new CacheException("Failed to evict cache", e);
        }
    }
    
    /**
     * 生成缓存键
     */
    private String generateCacheKey(String keyExpression, String keyGeneratorName, Method method, Object[] args) {
        return keyGenerator.generate(keyExpression, keyGeneratorName, method, args, null);
    }
    
    /**
     * 生成缓存键（包含返回值）
     */
    private String generateCacheKey(String keyExpression, String keyGeneratorName, Method method, Object[] args, Object result) {
        return keyGenerator.generate(keyExpression, keyGeneratorName, method, args, result);
    }
    
    /**
     * 评估缓存条件
     */
    private boolean evaluateCondition(String condition, Method method, Object[] args) {
        return conditionEvaluator.evaluate(condition, method, args, null);
    }
    
    /**
     * 评估缓存条件（包含返回值）
     */
    private boolean evaluateCondition(String condition, Method method, Object[] args, Object result) {
        return conditionEvaluator.evaluate(condition, method, args, result);
    }
    
    /**
     * 获取TTL
     */
    private Duration getTtl(TieredCache twoLevelCache) {
        int remoteTtl = twoLevelCache.remote().ttl();
        return remoteTtl > 0 ? Duration.ofSeconds(remoteTtl) : null;
    }
    
    /**
     * 根据缓存层级确定缓存策略
     */
    private CacheStrategy determineCacheStrategy(CacheLevel level) {
        switch (level) {
            case LOCAL:
                return CacheStrategy.LOCAL_ONLY;
            case REMOTE:
                return CacheStrategy.REMOTE_ONLY;
            case ALL:
            default:
                return CacheStrategy.WRITE_THROUGH;
        }
    }
}