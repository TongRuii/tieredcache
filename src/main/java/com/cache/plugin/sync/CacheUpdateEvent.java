package com.cache.plugin.sync;

import org.springframework.context.ApplicationEvent;

/**
 * 缓存更新事件
 */
public class CacheUpdateEvent extends ApplicationEvent {
    
    private final String key;
    private final Object value;
    private boolean fromRemote;
    
    public CacheUpdateEvent(Object source, String key, Object value) {
        super(source);
        this.key = key;
        this.value = value;
        this.fromRemote = false;
    }
    
    public CacheUpdateEvent(Object source, String key, Object value, boolean fromRemote) {
        super(source);
        this.key = key;
        this.value = value;
        this.fromRemote = fromRemote;
    }
    
    public String getKey() {
        return key;
    }
    
    public Object getValue() {
        return value;
    }
    
    public boolean isFromRemote() {
        return fromRemote;
    }
    
    public void setFromRemote(boolean fromRemote) {
        this.fromRemote = fromRemote;
    }
    
    @Override
    public String toString() {
        return "CacheUpdateEvent{" +
                "key='" + key + '\'' +
                ", value=" + value +
                ", fromRemote=" + fromRemote +
                '}';
    }
}