package com.cache.plugin.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * 分层缓存配置属性
 */
@Configuration
@ConfigurationProperties(prefix = "tiered-cache")
@Validated
public class TieredCacheProperties {
    
    private static final Logger logger = LoggerFactory.getLogger(TieredCacheProperties.class);
    
    /**
     * 是否启用分层缓存
     */
    private boolean enabled = true;
    
    /**
     * 本地缓存配置
     */
    private LocalCacheProperties local = new LocalCacheProperties();
    
    /**
     * 远程缓存配置
     */
    private RemoteCacheProperties remote = new RemoteCacheProperties();
    
    /**
     * 策略配置
     */
    private StrategyProperties strategy = new StrategyProperties();
    
    /**
     * 同步配置
     */
    private SyncProperties sync = new SyncProperties();
    
    /**
     * 安全配置
     */
    private SecurityProperties security = new SecurityProperties();
    
    /**
     * 监控配置
     */
    private MonitoringProperties monitoring = new MonitoringProperties();
    
    @PostConstruct
    public void validateAndAdjust() {
        // 验证本地缓存配置
        if (local.getMaxSize() <= 0) {
            logger.warn("Local cache max size is invalid: {}, using default value: 10000", local.getMaxSize());
            local.setMaxSize(10000L);
        }
        
        if (local.getExpireAfterWrite() != null && local.getExpireAfterWrite().getSeconds() <= 0) {
            logger.warn("Local cache expireAfterWrite is invalid: {}, disabling expiration", local.getExpireAfterWrite());
            local.setExpireAfterWrite(null);
        }
        
        if (local.getExpireAfterAccess() != null && local.getExpireAfterAccess().getSeconds() <= 0) {
            logger.warn("Local cache expireAfterAccess is invalid: {}, disabling expiration", local.getExpireAfterAccess());
            local.setExpireAfterAccess(null);
        }
        
        if (local.getInitialCapacity() <= 0) {
            logger.warn("Local cache initial capacity is invalid: {}, using default value: 100", local.getInitialCapacity());
            local.setInitialCapacity(100);
        }
        
        // 验证远程缓存配置
        if (remote.getTtl() != null && remote.getTtl().getSeconds() <= 0) {
            logger.warn("Remote cache TTL is invalid: {}, using default value: 3600s", remote.getTtl().getSeconds());
            remote.setTtl(Duration.ofHours(1));
        }
        
        if (remote.getTimeout() != null && remote.getTimeout().getSeconds() <= 0) {
            logger.warn("Remote cache timeout is invalid: {}, using default value: 5s", remote.getTimeout().getSeconds());
            remote.setTimeout(Duration.ofSeconds(5));
        }
        
        // 验证连接池配置
        if (remote.getPool().getMaxActive() <= 0) {
            logger.warn("Remote cache pool maxActive is invalid: {}, using default value: 8", remote.getPool().getMaxActive());
            remote.getPool().setMaxActive(8);
        }
        
        if (remote.getPool().getMaxIdle() <= 0) {
            logger.warn("Remote cache pool maxIdle is invalid: {}, using default value: 8", remote.getPool().getMaxIdle());
            remote.getPool().setMaxIdle(8);
        }
        
        // 验证同步配置
        if (sync.getBatchSize() <= 0) {
            logger.warn("Sync batch size is invalid: {}, using default value: 100", sync.getBatchSize());
            sync.setBatchSize(100);
        }
        
        if (sync.getFlushInterval() != null && sync.getFlushInterval().toMillis() <= 0) {
            logger.warn("Sync flush interval is invalid: {}, using default value: 1s", sync.getFlushInterval());
            sync.setFlushInterval(Duration.ofSeconds(1));
        }
        
        logger.info("TieredCache configuration validated and adjusted");
    }
    
    // Getters and Setters
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public LocalCacheProperties getLocal() {
        return local;
    }
    
    public void setLocal(LocalCacheProperties local) {
        this.local = local;
    }
    
    public RemoteCacheProperties getRemote() {
        return remote;
    }
    
    public void setRemote(RemoteCacheProperties remote) {
        this.remote = remote;
    }
    
    public StrategyProperties getStrategy() {
        return strategy;
    }
    
    public void setStrategy(StrategyProperties strategy) {
        this.strategy = strategy;
    }
    
    public SyncProperties getSync() {
        return sync;
    }
    
    public void setSync(SyncProperties sync) {
        this.sync = sync;
    }
    
    public SecurityProperties getSecurity() {
        return security;
    }
    
    public void setSecurity(SecurityProperties security) {
        this.security = security;
    }
    
    public MonitoringProperties getMonitoring() {
        return monitoring;
    }
    
    public void setMonitoring(MonitoringProperties monitoring) {
        this.monitoring = monitoring;
    }
    
    /**
     * 本地缓存配置
     */
    public static class LocalCacheProperties {
        private String provider = "caffeine";
        private long maxSize = 10000;
        private Duration expireAfterWrite = Duration.ofMinutes(5);
        private Duration expireAfterAccess = Duration.ofMinutes(10);
        private int initialCapacity = 100;
        private boolean recordStats = true;
        
        // Getters and Setters
        public String getProvider() {
            return provider;
        }
        
        public void setProvider(String provider) {
            this.provider = provider;
        }
        
        public long getMaxSize() {
            return maxSize;
        }
        
        public void setMaxSize(long maxSize) {
            this.maxSize = maxSize;
        }
        
        public Duration getExpireAfterWrite() {
            return expireAfterWrite;
        }
        
        public void setExpireAfterWrite(Duration expireAfterWrite) {
            this.expireAfterWrite = expireAfterWrite;
        }
        
        public Duration getExpireAfterAccess() {
            return expireAfterAccess;
        }
        
        public void setExpireAfterAccess(Duration expireAfterAccess) {
            this.expireAfterAccess = expireAfterAccess;
        }
        
        public int getInitialCapacity() {
            return initialCapacity;
        }
        
        public void setInitialCapacity(int initialCapacity) {
            this.initialCapacity = initialCapacity;
        }
        
        public boolean isRecordStats() {
            return recordStats;
        }
        
        public void setRecordStats(boolean recordStats) {
            this.recordStats = recordStats;
        }
    }
    
    /**
     * 远程缓存配置
     */
    public static class RemoteCacheProperties {
        private boolean enabled = true;
        private String provider = "redis";
        private Duration ttl = Duration.ofHours(1);
        private String clusterNodes = "localhost:6379";
        private String password;
        private int database = 0;
        private Duration timeout = Duration.ofSeconds(5);
        private PoolProperties pool = new PoolProperties();
        
        // Getters and Setters
        public boolean isEnabled() {
            return enabled;
        }
        
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
        
        public String getProvider() {
            return provider;
        }
        
        public void setProvider(String provider) {
            this.provider = provider;
        }
        
        public Duration getTtl() {
            return ttl;
        }
        
        public void setTtl(Duration ttl) {
            this.ttl = ttl;
        }
        
        public String getClusterNodes() {
            return clusterNodes;
        }
        
        public void setClusterNodes(String clusterNodes) {
            this.clusterNodes = clusterNodes;
        }
        
        public String getPassword() {
            return password;
        }
        
        public void setPassword(String password) {
            this.password = password;
        }
        
        public int getDatabase() {
            return database;
        }
        
        public void setDatabase(int database) {
            this.database = database;
        }
        
        public Duration getTimeout() {
            return timeout;
        }
        
        public void setTimeout(Duration timeout) {
            this.timeout = timeout;
        }
        
        public PoolProperties getPool() {
            return pool;
        }
        
        public void setPool(PoolProperties pool) {
            this.pool = pool;
        }
    }
    
    /**
     * 连接池配置
     */
    public static class PoolProperties {
        private int maxActive = 8;
        private int maxIdle = 8;
        private int minIdle = 0;
        private Duration maxWait = Duration.ofMillis(-1);
        
        // Getters and Setters
        public int getMaxActive() {
            return maxActive;
        }
        
        public void setMaxActive(int maxActive) {
            this.maxActive = maxActive;
        }
        
        public int getMaxIdle() {
            return maxIdle;
        }
        
        public void setMaxIdle(int maxIdle) {
            this.maxIdle = maxIdle;
        }
        
        public int getMinIdle() {
            return minIdle;
        }
        
        public void setMinIdle(int minIdle) {
            this.minIdle = minIdle;
        }
        
        public Duration getMaxWait() {
            return maxWait;
        }
        
        public void setMaxWait(Duration maxWait) {
            this.maxWait = maxWait;
        }
    }
    
    /**
     * 策略配置
     */
    public static class StrategyProperties {
        private String defaultStrategy = "local-first";
        private String writeMode = "write-through";
        private String readMode = "read-through";
        private String consistency = "eventual";
        
        // Getters and Setters
        public String getDefaultStrategy() {
            return defaultStrategy;
        }
        
        public void setDefaultStrategy(String defaultStrategy) {
            this.defaultStrategy = defaultStrategy;
        }
        
        public String getWriteMode() {
            return writeMode;
        }
        
        public void setWriteMode(String writeMode) {
            this.writeMode = writeMode;
        }
        
        public String getReadMode() {
            return readMode;
        }
        
        public void setReadMode(String readMode) {
            this.readMode = readMode;
        }
        
        public String getConsistency() {
            return consistency;
        }
        
        public void setConsistency(String consistency) {
            this.consistency = consistency;
        }
    }
    
    /**
     * 同步配置
     */
    public static class SyncProperties {
        private boolean enabled = true;
        private String channel = "cache-sync";
        private int batchSize = 100;
        private Duration flushInterval = Duration.ofSeconds(1);
        
        // Getters and Setters
        public boolean isEnabled() {
            return enabled;
        }
        
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
        
        public String getChannel() {
            return channel;
        }
        
        public void setChannel(String channel) {
            this.channel = channel;
        }
        
        public int getBatchSize() {
            return batchSize;
        }
        
        public void setBatchSize(int batchSize) {
            this.batchSize = batchSize;
        }
        
        public Duration getFlushInterval() {
            return flushInterval;
        }
        
        public void setFlushInterval(Duration flushInterval) {
            this.flushInterval = flushInterval;
        }
    }
    
    /**
     * 安全配置
     */
    public static class SecurityProperties {
        private EncryptionProperties encryption = new EncryptionProperties();
        private AccessControlProperties accessControl = new AccessControlProperties();
        
        // Getters and Setters
        public EncryptionProperties getEncryption() {
            return encryption;
        }
        
        public void setEncryption(EncryptionProperties encryption) {
            this.encryption = encryption;
        }
        
        public AccessControlProperties getAccessControl() {
            return accessControl;
        }
        
        public void setAccessControl(AccessControlProperties accessControl) {
            this.accessControl = accessControl;
        }
    }
    
    /**
     * 加密配置
     */
    public static class EncryptionProperties {
        private boolean enabled = false;
        private String algorithm = "AES-256-GCM";
        private String key;
        
        // Getters and Setters
        public boolean isEnabled() {
            return enabled;
        }
        
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
        
        public String getAlgorithm() {
            return algorithm;
        }
        
        public void setAlgorithm(String algorithm) {
            this.algorithm = algorithm;
        }
        
        public String getKey() {
            return key;
        }
        
        public void setKey(String key) {
            this.key = key;
        }
    }
    
    /**
     * 访问控制配置
     */
    public static class AccessControlProperties {
        private boolean enabled = false;
        private String defaultPolicy = "deny";
        private Map<String, String> rules = new HashMap<>();
        
        // Getters and Setters
        public boolean isEnabled() {
            return enabled;
        }
        
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
        
        public String getDefaultPolicy() {
            return defaultPolicy;
        }
        
        public void setDefaultPolicy(String defaultPolicy) {
            this.defaultPolicy = defaultPolicy;
        }
        
        public Map<String, String> getRules() {
            return rules;
        }
        
        public void setRules(Map<String, String> rules) {
            this.rules = rules;
        }
    }
    
    /**
     * 监控配置
     */
    public static class MonitoringProperties {
        private MetricsProperties metrics = new MetricsProperties();
        private HealthCheckProperties healthCheck = new HealthCheckProperties();
        
        // Getters and Setters
        public MetricsProperties getMetrics() {
            return metrics;
        }
        
        public void setMetrics(MetricsProperties metrics) {
            this.metrics = metrics;
        }
        
        public HealthCheckProperties getHealthCheck() {
            return healthCheck;
        }
        
        public void setHealthCheck(HealthCheckProperties healthCheck) {
            this.healthCheck = healthCheck;
        }
    }
    
    /**
     * 指标配置
     */
    public static class MetricsProperties {
        private boolean enabled = true;
        private Duration exportInterval = Duration.ofSeconds(30);
        
        // Getters and Setters
        public boolean isEnabled() {
            return enabled;
        }
        
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
        
        public Duration getExportInterval() {
            return exportInterval;
        }
        
        public void setExportInterval(Duration exportInterval) {
            this.exportInterval = exportInterval;
        }
    }
    
    /**
     * 健康检查配置
     */
    public static class HealthCheckProperties {
        private boolean enabled = true;
        private Duration interval = Duration.ofSeconds(10);
        
        // Getters and Setters
        public boolean isEnabled() {
            return enabled;
        }
        
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
        
        public Duration getInterval() {
            return interval;
        }
        
        public void setInterval(Duration interval) {
            this.interval = interval;
        }
    }
}