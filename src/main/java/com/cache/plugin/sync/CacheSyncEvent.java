package com.cache.plugin.sync;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 缓存同步事件
 */
public class CacheSyncEvent {
    
    /**
     * 同步事件类型
     */
    public enum Type {
        PUT,    // 缓存存储
        EVICT,  // 缓存清除
        CLEAR   // 缓存清空
    }
    
    private final Type type;
    private final String key;
    private final Object value;
    private final long timestamp;
    private final String nodeId;
    
    @JsonCreator
    public CacheSyncEvent(@JsonProperty("type") Type type,
                         @JsonProperty("key") String key,
                         @JsonProperty("value") Object value,
                         @JsonProperty("timestamp") long timestamp,
                         @JsonProperty("nodeId") String nodeId) {
        this.type = type;
        this.key = key;
        this.value = value;
        this.timestamp = timestamp;
        this.nodeId = nodeId;
    }
    
    public Type getType() {
        return type;
    }
    
    public String getKey() {
        return key;
    }
    
    public Object getValue() {
        return value;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public String getNodeId() {
        return nodeId;
    }
    
    /**
     * 检查是否来自当前节点
     */
    public boolean isFromCurrentNode() {
        String currentNodeId = System.getProperty("cache.node.id", "node-" + System.currentTimeMillis());
        return currentNodeId.equals(nodeId);
    }
    
    @Override
    public String toString() {
        return "CacheSyncEvent{" +
                "type=" + type +
                ", key='" + key + '\'' +
                ", value=" + value +
                ", timestamp=" + timestamp +
                ", nodeId='" + nodeId + '\'' +
                '}';
    }
}