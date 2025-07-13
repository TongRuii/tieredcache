package com.cache.plugin.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 缓存指标监控
 */
public class CacheMetrics {
    
    private static final Logger logger = LoggerFactory.getLogger(CacheMetrics.class);
    
    private final MeterRegistry meterRegistry;
    
    // 计数器
    private final Counter localHitCounter;
    private final Counter remoteHitCounter;
    private final Counter missCounter;
    private final Counter putCounter;
    private final Counter evictCounter;
    private final Counter errorCounter;
    
    // 计时器
    private final Timer localGetTimer;
    private final Timer remoteGetTimer;
    private final Timer putTimer;
    private final Timer evictTimer;
    
    // 原子计数器用于实时统计
    private final AtomicLong totalRequests = new AtomicLong(0);
    private final AtomicLong localHits = new AtomicLong(0);
    private final AtomicLong remoteHits = new AtomicLong(0);
    private final AtomicLong misses = new AtomicLong(0);
    
    public CacheMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        
        // 初始化计数器
        this.localHitCounter = Counter.builder("cache.local.hit")
                .description("Local cache hit count")
                .tag("level", "local")
                .register(meterRegistry);
                
        this.remoteHitCounter = Counter.builder("cache.remote.hit")
                .description("Remote cache hit count")
                .tag("level", "remote")
                .register(meterRegistry);
                
        this.missCounter = Counter.builder("cache.miss")
                .description("Cache miss count")
                .register(meterRegistry);
                
        this.putCounter = Counter.builder("cache.put")
                .description("Cache put count")
                .register(meterRegistry);
                
        this.evictCounter = Counter.builder("cache.evict")
                .description("Cache evict count")
                .register(meterRegistry);
                
        this.errorCounter = Counter.builder("cache.error")
                .description("Cache error count")
                .register(meterRegistry);
        
        // 初始化计时器
        this.localGetTimer = Timer.builder("cache.local.get.duration")
                .description("Local cache get duration")
                .tag("level", "local")
                .register(meterRegistry);
                
        this.remoteGetTimer = Timer.builder("cache.remote.get.duration")
                .description("Remote cache get duration")
                .tag("level", "remote")
                .register(meterRegistry);
                
        this.putTimer = Timer.builder("cache.put.duration")
                .description("Cache put duration")
                .register(meterRegistry);
                
        this.evictTimer = Timer.builder("cache.evict.duration")
                .description("Cache evict duration")
                .register(meterRegistry);
        
        // 注册仪表盘指标
        registerGauges();
        
        logger.info("Cache metrics initialized");
    }
    
    /**
     * 注册仪表盘指标
     */
    private void registerGauges() {
        // 命中率
        Gauge.builder("cache.hit.rate")
                .description("Cache hit rate")
                .register(meterRegistry, this, CacheMetrics::getHitRate);
                
        // 本地命中率
        Gauge.builder("cache.local.hit.rate")
                .description("Local cache hit rate")
                .tag("level", "local")
                .register(meterRegistry, this, CacheMetrics::getLocalHitRate);
                
        // 远程命中率
        Gauge.builder("cache.remote.hit.rate")
                .description("Remote cache hit rate")
                .tag("level", "remote")
                .register(meterRegistry, this, CacheMetrics::getRemoteHitRate);
                
        // 总请求数
        Gauge.builder("cache.requests.total")
                .description("Total cache requests")
                .register(meterRegistry, this, CacheMetrics::getTotalRequests);
    }
    
    /**
     * 记录本地缓存命中
     */
    public void recordLocalHit() {
        localHitCounter.increment();
        localHits.incrementAndGet();
        totalRequests.incrementAndGet();
        logger.debug("Recorded local cache hit");
    }
    
    /**
     * 记录远程缓存命中
     */
    public void recordRemoteHit() {
        remoteHitCounter.increment();
        remoteHits.incrementAndGet();
        totalRequests.incrementAndGet();
        logger.debug("Recorded remote cache hit");
    }
    
    /**
     * 记录缓存未命中
     */
    public void recordMiss() {
        missCounter.increment();
        misses.incrementAndGet();
        totalRequests.incrementAndGet();
        logger.debug("Recorded cache miss");
    }
    
    /**
     * 记录缓存存储
     */
    public void recordPut() {
        putCounter.increment();
        logger.debug("Recorded cache put");
    }
    
    /**
     * 记录缓存清除
     */
    public void recordEvict() {
        evictCounter.increment();
        logger.debug("Recorded cache evict");
    }
    
    /**
     * 记录缓存错误
     */
    public void recordError() {
        errorCounter.increment();
        logger.debug("Recorded cache error");
    }
    
    /**
     * 记录本地缓存获取耗时
     */
    public void recordLocalGetLatency(Duration duration) {
        localGetTimer.record(duration);
        logger.debug("Recorded local cache get latency: {}ms", duration.toMillis());
    }
    
    /**
     * 记录远程缓存获取耗时
     */
    public void recordRemoteGetLatency(Duration duration) {
        remoteGetTimer.record(duration);
        logger.debug("Recorded remote cache get latency: {}ms", duration.toMillis());
    }
    
    /**
     * 记录缓存存储耗时
     */
    public void recordPutLatency(Duration duration) {
        putTimer.record(duration);
        logger.debug("Recorded cache put latency: {}ms", duration.toMillis());
    }
    
    /**
     * 记录缓存清除耗时
     */
    public void recordEvictLatency(Duration duration) {
        evictTimer.record(duration);
        logger.debug("Recorded cache evict latency: {}ms", duration.toMillis());
    }
    
    /**
     * 获取总命中率
     */
    public double getHitRate() {
        long total = totalRequests.get();
        if (total == 0) {
            return 0.0;
        }
        long hits = localHits.get() + remoteHits.get();
        return (double) hits / total;
    }
    
    /**
     * 获取本地缓存命中率
     */
    public double getLocalHitRate() {
        long total = totalRequests.get();
        if (total == 0) {
            return 0.0;
        }
        return (double) localHits.get() / total;
    }
    
    /**
     * 获取远程缓存命中率
     */
    public double getRemoteHitRate() {
        long total = totalRequests.get();
        if (total == 0) {
            return 0.0;
        }
        return (double) remoteHits.get() / total;
    }
    
    /**
     * 获取总请求数
     */
    public long getTotalRequests() {
        return totalRequests.get();
    }
    
    /**
     * 获取本地命中数
     */
    public long getLocalHits() {
        return localHits.get();
    }
    
    /**
     * 获取远程命中数
     */
    public long getRemoteHits() {
        return remoteHits.get();
    }
    
    /**
     * 获取未命中数
     */
    public long getMisses() {
        return misses.get();
    }
    
    /**
     * 重置所有统计
     */
    public void reset() {
        totalRequests.set(0);
        localHits.set(0);
        remoteHits.set(0);
        misses.set(0);
        logger.info("Cache metrics reset");
    }
    
    /**
     * 获取缓存统计摘要
     */
    public CacheMetricsSummary getSummary() {
        return new CacheMetricsSummary(
            getTotalRequests(),
            getLocalHits(),
            getRemoteHits(),
            getMisses(),
            getHitRate(),
            getLocalHitRate(),
            getRemoteHitRate()
        );
    }
    
    /**
     * 缓存指标摘要
     */
    public static class CacheMetricsSummary {
        private final long totalRequests;
        private final long localHits;
        private final long remoteHits;
        private final long misses;
        private final double hitRate;
        private final double localHitRate;
        private final double remoteHitRate;
        
        public CacheMetricsSummary(long totalRequests, long localHits, long remoteHits, 
                                  long misses, double hitRate, double localHitRate, double remoteHitRate) {
            this.totalRequests = totalRequests;
            this.localHits = localHits;
            this.remoteHits = remoteHits;
            this.misses = misses;
            this.hitRate = hitRate;
            this.localHitRate = localHitRate;
            this.remoteHitRate = remoteHitRate;
        }
        
        // Getters
        public long getTotalRequests() { return totalRequests; }
        public long getLocalHits() { return localHits; }
        public long getRemoteHits() { return remoteHits; }
        public long getMisses() { return misses; }
        public double getHitRate() { return hitRate; }
        public double getLocalHitRate() { return localHitRate; }
        public double getRemoteHitRate() { return remoteHitRate; }
        
        @Override
        public String toString() {
            return String.format(
                "CacheMetrics{totalRequests=%d, localHits=%d, remoteHits=%d, misses=%d, " +
                "hitRate=%.2f%%, localHitRate=%.2f%%, remoteHitRate=%.2f%%}",
                totalRequests, localHits, remoteHits, misses,
                hitRate * 100, localHitRate * 100, remoteHitRate * 100
            );
        }
    }
}