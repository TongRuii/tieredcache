package com.cache.plugin.example.demo.service;

import com.cache.plugin.annotation.TieredCache;
import com.cache.plugin.annotation.LocalCache;
import com.cache.plugin.annotation.RemoteCache;
import com.cache.plugin.annotation.CacheEvict;
import com.cache.plugin.annotation.CacheStrategy;
import com.cache.plugin.example.demo.model.Order;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 订单服务 - 演示缓存清除和更新
 */
@Service
public class OrderService {
    
    /**
     * 获取订单信息 - 分层缓存
     */
    @TieredCache(
        local = @LocalCache(maxSize = 200, expireAfterWrite = 300),
        remote = @RemoteCache(ttl = 1800),
        key = "'order:' + #orderId",
        strategy = CacheStrategy.LOCAL_FIRST
    )
    public Order getOrderById(Long orderId) {
        // 模拟数据库查询
        simulateDbDelay(150);
        
        return createMockOrder(orderId);
    }
    
    /**
     * 更新订单状态 - 清除缓存
     */
    @CacheEvict(key = "'order:' + #orderId")
    public void updateOrderStatus(Long orderId, String newStatus) {
        // 模拟数据库更新
        simulateDbDelay(100);
        
        System.out.printf("订单 %d 状态已更新为: %s\n", orderId, newStatus);
    }
    
    /**
     * 创建订单 - 结果会被缓存
     */
    @TieredCache(
        local = @LocalCache(maxSize = 200, expireAfterWrite = 300),
        remote = @RemoteCache(ttl = 1800),
        key = "'order:' + #result.id"
    )
    public Order createOrder(Long userId, List<String> productIds) {
        // 模拟订单创建
        simulateDbDelay(200);
        
        Long orderId = System.currentTimeMillis() % 100000;
        String orderNo = "ORD" + System.currentTimeMillis();
        
        Order order = new Order(orderId, orderNo, userId);
        order.setTotalAmount(BigDecimal.valueOf(Math.random() * 1000 + 100));
        order.setShippingAddress("北京市朝阳区示例地址");
        
        return order;
    }
    
    /**
     * 获取用户订单列表 - 分页缓存
     */
    @TieredCache(
        local = @LocalCache(maxSize = 50, expireAfterWrite = 180),
        remote = @RemoteCache(ttl = 900),
        key = "'orders:user:' + #userId + ':page:' + #page + ':size:' + #size"
    )
    public List<Order> getUserOrders(Long userId, int page, int size) {
        simulateDbDelay(180);
        
        List<Order> orders = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            Long orderId = userId * 1000 + page * size + i + 1;
            orders.add(createMockOrder(orderId));
        }
        
        return orders;
    }
    
    /**
     * 获取订单统计 - 长时间缓存
     */
    @TieredCache(
        local = @LocalCache(maxSize = 10, expireAfterWrite = 600),
        remote = @RemoteCache(ttl = 3600),
        key = "'order:stats:' + #userId + ':' + #year + '-' + #month"
    )
    public OrderStats getOrderStats(Long userId, int year, int month) {
        simulateDbDelay(300);
        
        // 模拟统计数据
        int orderCount = (int)(Math.random() * 20) + 1;
        BigDecimal totalAmount = BigDecimal.valueOf(Math.random() * 10000 + 1000);
        
        return new OrderStats(userId, year, month, orderCount, totalAmount);
    }
    
    /**
     * 取消订单 - 清除相关缓存
     */
    @CacheEvict(key = "'order:' + #orderId")
    public void cancelOrder(Long orderId) {
        simulateDbDelay(120);
        
        System.out.printf("订单 %d 已取消\n", orderId);
    }
    
    /**
     * 获取今日订单 - 短时间缓存
     */
    @LocalCache(
        key = "'orders:today'",
        maxSize = 1,
        expireAfterWrite = 60 // 1分钟过期
    )
    public List<Order> getTodayOrders() {
        simulateDbDelay(250);
        
        List<Order> todayOrders = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            todayOrders.add(createMockOrder((long)(i + 1)));
        }
        
        return todayOrders;
    }
    
    /**
     * 清除用户所有订单缓存
     */
    public void clearUserOrderCache(Long userId) {
        // 这里可以通过缓存管理器清除特定前缀的缓存
        System.out.printf("清除用户 %d 的所有订单缓存\n", userId);
    }
    
    /**
     * 创建模拟订单数据
     */
    private Order createMockOrder(Long orderId) {
        String[] statuses = {"PENDING", "CONFIRMED", "SHIPPED", "DELIVERED", "CANCELLED"};
        
        String orderNo = "ORD" + String.format("%08d", orderId);
        Long userId = 1000L + (orderId % 100);
        String status = statuses[(int)(orderId % statuses.length)];
        
        Order order = new Order(orderId, orderNo, userId);
        order.setStatus(status);
        order.setTotalAmount(BigDecimal.valueOf(100 + (orderId % 9900)));
        order.setShippingAddress("北京市朝阳区示例地址 " + orderId);
        order.setCreatedAt(LocalDateTime.now().minusDays(orderId % 30));
        order.setUpdatedAt(LocalDateTime.now().minusHours(orderId % 24));
        
        return order;
    }
    
    /**
     * 模拟数据库查询延迟
     */
    private void simulateDbDelay(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

/**
 * 订单统计类
 */
class OrderStats {
    private Long userId;
    private int year;
    private int month;
    private int orderCount;
    private BigDecimal totalAmount;
    
    public OrderStats(Long userId, int year, int month, int orderCount, BigDecimal totalAmount) {
        this.userId = userId;
        this.year = year;
        this.month = month;
        this.orderCount = orderCount;
        this.totalAmount = totalAmount;
    }
    
    // Getters
    public Long getUserId() { return userId; }
    public int getYear() { return year; }
    public int getMonth() { return month; }
    public int getOrderCount() { return orderCount; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    
    @Override
    public String toString() {
        return String.format("OrderStats{userId=%d, %d-%02d, orders=%d, total=%.2f}", 
                           userId, year, month, orderCount, totalAmount);
    }
}