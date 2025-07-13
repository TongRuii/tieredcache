package com.cache.plugin.remote;

/**
 * 消息监听器接口
 */
@FunctionalInterface
public interface MessageListener {
    
    /**
     * 处理接收到的消息
     * 
     * @param channel 频道名称
     * @param message 消息内容
     */
    void onMessage(String channel, Object message);
}