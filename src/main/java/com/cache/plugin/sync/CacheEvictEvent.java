package com.cache.plugin.sync;

import org.springframework.context.ApplicationEvent;

/**
 * 缓存清除事件
 */
public class CacheEvictEvent extends ApplicationEvent {
    
    private final String key;
    private final boolean allEntries;
    private boolean fromRemote;
    
    public CacheEvictEvent(Object source, String key) {
        super(source);
        this.key = key;
        this.allEntries = false;
        this.fromRemote = false;
    }
    
    public CacheEvictEvent(Object source, String key, boolean allEntries) {
        super(source);
        this.key = key;
        this.allEntries = allEntries;
        this.fromRemote = false;
    }
    
    public CacheEvictEvent(Object source, String key, boolean allEntries, boolean fromRemote) {
        super(source);
        this.key = key;
        this.allEntries = allEntries;
        this.fromRemote = fromRemote;
    }
    
    public String getKey() {
        return key;
    }
    
    public boolean isAllEntries() {
        return allEntries;
    }
    
    public boolean isFromRemote() {
        return fromRemote;
    }
    
    public void setFromRemote(boolean fromRemote) {
        this.fromRemote = fromRemote;
    }
    
    @Override
    public String toString() {
        return "CacheEvictEvent{" +
                "key='" + key + '\'' +
                ", allEntries=" + allEntries +
                ", fromRemote=" + fromRemote +
                '}';
    }
}