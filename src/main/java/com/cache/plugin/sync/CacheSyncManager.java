package com.cache.plugin.sync;

import com.cache.plugin.config.TieredCacheProperties;
import com.cache.plugin.local.LocalCache;
import com.cache.plugin.remote.RemoteCache;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 缓存同步管理器
 */
public class CacheSyncManager {
    
    private static final Logger logger = LoggerFactory.getLogger(CacheSyncManager.class);
    
    private final LocalCache<String, Object> localCache;
    private final RemoteCache<String, Object> remoteCache;
    private final TieredCacheProperties.SyncProperties syncProperties;
    private final ObjectMapper objectMapper;
    private final ScheduledExecutorService syncExecutor;
    
    private ApplicationEventPublisher eventPublisher;
    
    public CacheSyncManager(LocalCache<String, Object> localCache,
                           RemoteCache<String, Object> remoteCache,
                           TieredCacheProperties.SyncProperties syncProperties) {
        this.localCache = localCache;
        this.remoteCache = remoteCache;
        this.syncProperties = syncProperties;
        this.objectMapper = new ObjectMapper();
        this.syncExecutor = Executors.newScheduledThreadPool(2, r -> {
            Thread t = new Thread(r, "cache-sync-");
            t.setDaemon(true);
            return t;
        });
    }
    
    public void setEventPublisher(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }
    
    @PostConstruct
    public void initialize() {
        if (syncProperties.isEnabled()) {
            subscribeToSyncChannel();
            startPeriodicSync();
            logger.info("Cache sync manager initialized with channel: {}", syncProperties.getChannel());
        } else {
            logger.info("Cache sync is disabled");
        }
    }
    
    @PreDestroy
    public void destroy() {
        if (syncExecutor != null && !syncExecutor.isShutdown()) {
            syncExecutor.shutdown();
            try {
                if (!syncExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    syncExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                syncExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        logger.info("Cache sync manager destroyed");
    }
    
    /**
     * 订阅同步频道
     */
    private void subscribeToSyncChannel() {
        try {
            remoteCache.subscribe(syncProperties.getChannel(), (channel, message) -> {
                try {
                    handleSyncMessage(message);
                } catch (Exception e) {
                    logger.error("Failed to handle sync message from channel: {}", channel, e);
                }
            });
            logger.info("Subscribed to sync channel: {}", syncProperties.getChannel());
        } catch (Exception e) {
            logger.error("Failed to subscribe to sync channel: {}", syncProperties.getChannel(), e);
        }
    }
    
    /**
     * 启动定期同步
     */
    private void startPeriodicSync() {
        long intervalMillis = syncProperties.getFlushInterval().toMillis();
        syncExecutor.scheduleAtFixedRate(
            this::performPeriodicSync,
            intervalMillis,
            intervalMillis,
            TimeUnit.MILLISECONDS
        );
        logger.info("Started periodic sync with interval: {}", syncProperties.getFlushInterval());
    }
    
    /**
     * 处理同步消息
     */
    private void handleSyncMessage(Object message) {
        try {
            if (message instanceof String) {
                CacheSyncEvent event = objectMapper.readValue((String) message, CacheSyncEvent.class);
                processSyncEvent(event);
            } else if (message instanceof CacheSyncEvent) {
                processSyncEvent((CacheSyncEvent) message);
            } else {
                logger.warn("Unknown sync message type: {}", message.getClass());
            }
        } catch (Exception e) {
            logger.error("Failed to process sync message: {}", message, e);
        }
    }
    
    /**
     * 处理同步事件
     */
    private void processSyncEvent(CacheSyncEvent event) {
        if (event.isFromCurrentNode()) {
            // 忽略来自当前节点的事件，避免循环
            return;
        }
        
        try {
            switch (event.getType()) {
                case PUT:
                    localCache.put(event.getKey(), event.getValue());
                    logger.debug("Synced PUT operation for key: {}", event.getKey());
                    break;
                case EVICT:
                    localCache.evict(event.getKey());
                    logger.debug("Synced EVICT operation for key: {}", event.getKey());
                    break;
                case CLEAR:
                    localCache.clear();
                    logger.debug("Synced CLEAR operation");
                    break;
                default:
                    logger.warn("Unknown sync event type: {}", event.getType());
            }
        } catch (Exception e) {
            logger.error("Failed to process sync event: {}", event, e);
        }
    }
    
    /**
     * 发布缓存更新事件
     */
    public void publishCacheUpdate(String key, Object value) {
        if (!syncProperties.isEnabled()) {
            return;
        }
        
        try {
            CacheSyncEvent event = new CacheSyncEvent(
                CacheSyncEvent.Type.PUT,
                key,
                value,
                System.currentTimeMillis(),
                getCurrentNodeId()
            );
            publishSyncEvent(event);
        } catch (Exception e) {
            logger.error("Failed to publish cache update event for key: {}", key, e);
        }
    }
    
    /**
     * 发布缓存清除事件
     */
    public void publishCacheEvict(String key) {
        if (!syncProperties.isEnabled()) {
            return;
        }
        
        try {
            CacheSyncEvent event = new CacheSyncEvent(
                CacheSyncEvent.Type.EVICT,
                key,
                null,
                System.currentTimeMillis(),
                getCurrentNodeId()
            );
            publishSyncEvent(event);
        } catch (Exception e) {
            logger.error("Failed to publish cache evict event for key: {}", key, e);
        }
    }
    
    /**
     * 发布缓存清空事件
     */
    public void publishCacheClear() {
        if (!syncProperties.isEnabled()) {
            return;
        }
        
        try {
            CacheSyncEvent event = new CacheSyncEvent(
                CacheSyncEvent.Type.CLEAR,
                null,
                null,
                System.currentTimeMillis(),
                getCurrentNodeId()
            );
            publishSyncEvent(event);
        } catch (Exception e) {
            logger.error("Failed to publish cache clear event", e);
        }
    }
    
    /**
     * 发布同步事件
     */
    private void publishSyncEvent(CacheSyncEvent event) {
        try {
            String message = objectMapper.writeValueAsString(event);
            remoteCache.publish(syncProperties.getChannel(), message);
            logger.debug("Published sync event: {}", event);
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize sync event: {}", event, e);
        } catch (Exception e) {
            logger.error("Failed to publish sync event: {}", event, e);
        }
    }
    
    /**
     * 执行定期同步
     */
    private void performPeriodicSync() {
        try {
            // 这里可以实现定期的缓存同步逻辑
            // 例如：检查缓存一致性、清理过期数据等
            logger.debug("Performing periodic cache sync");
        } catch (Exception e) {
            logger.error("Failed to perform periodic sync", e);
        }
    }
    
    /**
     * 获取当前节点ID
     */
    private String getCurrentNodeId() {
        // 这里可以使用更复杂的节点ID生成策略
        // 例如：IP地址 + 端口 + 进程ID
        return System.getProperty("cache.node.id", "node-" + System.currentTimeMillis());
    }
    
    /**
     * 处理本地缓存更新事件
     */
    @EventListener
    public void handleLocalCacheUpdate(CacheUpdateEvent event) {
        if (event.isFromRemote()) {
            // 来自远程的更新，同步到本地缓存
            localCache.put(event.getKey(), event.getValue());
        } else {
            // 本地更新，发布到远程
            publishCacheUpdate(event.getKey(), event.getValue());
        }
    }
    
    /**
     * 处理本地缓存清除事件
     */
    @EventListener
    public void handleLocalCacheEvict(CacheEvictEvent event) {
        if (event.isFromRemote()) {
            // 来自远程的清除，同步到本地缓存
            if (event.isAllEntries()) {
                localCache.clear();
            } else {
                localCache.evict(event.getKey());
            }
        } else {
            // 本地清除，发布到远程
            if (event.isAllEntries()) {
                publishCacheClear();
            } else {
                publishCacheEvict(event.getKey());
            }
        }
    }
}