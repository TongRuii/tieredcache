package com.cache.plugin.core;

/**
 * 缓存统计信息
 */
public class CacheStats {
    
    private final long hitCount;
    private final long missCount;
    private final long loadCount;
    private final long evictionCount;
    private final double averageLoadTime;
    
    public CacheStats(long hitCount, long missCount, long loadCount, 
                     long evictionCount, double averageLoadTime) {
        this.hitCount = hitCount;
        this.missCount = missCount;
        this.loadCount = loadCount;
        this.evictionCount = evictionCount;
        this.averageLoadTime = averageLoadTime;
    }
    
    /**
     * 命中次数
     */
    public long getHitCount() {
        return hitCount;
    }
    
    /**
     * 未命中次数
     */
    public long getMissCount() {
        return missCount;
    }
    
    /**
     * 加载次数
     */
    public long getLoadCount() {
        return loadCount;
    }
    
    /**
     * 驱逐次数
     */
    public long getEvictionCount() {
        return evictionCount;
    }
    
    /**
     * 平均加载时间（纳秒）
     */
    public double getAverageLoadTime() {
        return averageLoadTime;
    }
    
    /**
     * 命中率
     */
    public double getHitRate() {
        long requestCount = hitCount + missCount;
        return requestCount == 0 ? 1.0 : (double) hitCount / requestCount;
    }
    
    /**
     * 未命中率
     */
    public double getMissRate() {
        return 1.0 - getHitRate();
    }
    
    /**
     * 总请求次数
     */
    public long getRequestCount() {
        return hitCount + missCount;
    }
    
    @Override
    public String toString() {
        return "CacheStats{" +
                "hitCount=" + hitCount +
                ", missCount=" + missCount +
                ", hitRate=" + String.format("%.2f%%", getHitRate() * 100) +
                ", loadCount=" + loadCount +
                ", evictionCount=" + evictionCount +
                ", averageLoadTime=" + String.format("%.2f ns", averageLoadTime) +
                '}';
    }
}