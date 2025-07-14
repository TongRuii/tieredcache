package com.cache.plugin.example.demo;

import com.cache.plugin.example.demo.model.User;
import com.cache.plugin.example.demo.model.Product;
import com.cache.plugin.example.demo.model.Order;
import com.cache.plugin.example.demo.service.UserService;
import com.cache.plugin.example.demo.service.ProductService;
import com.cache.plugin.example.demo.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * 演示运行器 - 展示各种缓存使用场景
 */
@Component
public class DemoRunner implements CommandLineRunner {
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private ProductService productService;
    
    @Autowired
    private OrderService orderService;
    
    @Override
    public void run(String... args) throws Exception {
        System.out.println("\n🚀 开始TieredCache功能演示...\n");
        
        // 演示1: 用户信息缓存
        demonstrateUserCache();
        
        // 演示2: 产品信息缓存
        demonstrateProductCache();
        
        // 演示3: 订单缓存和清除
        demonstrateOrderCache();
        
        // 演示4: 缓存性能对比
        demonstratePerformance();
        
        // 演示5: 缓存策略对比
        demonstrateCacheStrategies();
        
        System.out.println("\n✅ 演示完成！");
    }
    
    /**
     * 演示用户信息缓存 - 分层缓存策略
     */
    private void demonstrateUserCache() {
        System.out.println("📱 === 用户信息缓存演示 ===");
        
        Long userId = 1001L;
        
        // 第一次调用 - 从数据库获取
        System.out.println("🔍 第一次获取用户信息 (从数据库):");
        long start1 = System.currentTimeMillis();
        User user1 = userService.getUserById(userId);
        long time1 = System.currentTimeMillis() - start1;
        System.out.printf("   结果: %s (耗时: %dms)\n", user1.getName(), time1);
        
        // 第二次调用 - 从本地缓存获取
        System.out.println("🚀 第二次获取用户信息 (从本地缓存):");
        long start2 = System.currentTimeMillis();
        User user2 = userService.getUserById(userId);
        long time2 = System.currentTimeMillis() - start2;
        System.out.printf("   结果: %s (耗时: %dms)\n", user2.getName(), time2);
        System.out.printf("   性能提升: %.1fx\n", (double)time1/time2);
        
        // 获取用户详情 - 仅本地缓存
        System.out.println("💾 获取用户详情 (仅本地缓存):");
        com.cache.plugin.example.demo.model.UserProfile profile = userService.getUserProfile(userId);
        System.out.printf("   详情: %s\n", profile.getDisplayName());
        
        System.out.println();
    }
    
    /**
     * 演示产品信息缓存 - 远程缓存策略
     */
    private void demonstrateProductCache() {
        System.out.println("🛍️ === 产品信息缓存演示 ===");
        
        String productId = "PROD-001";
        
        // 第一次调用
        System.out.println("🔍 第一次获取产品信息:");
        Product product1 = productService.getProductById(productId);
        System.out.printf("   产品: %s - ¥%.2f\n", product1.getName(), product1.getPrice());
        
        // 第二次调用 - 从远程缓存获取
        System.out.println("🌐 第二次获取产品信息 (从远程缓存):");
        Product product2 = productService.getProductById(productId);
        System.out.printf("   产品: %s - ¥%.2f\n", product2.getName(), product2.getPrice());
        
        // 获取热门产品列表
        System.out.println("🔥 获取热门产品列表:");
        java.util.List<Product> hotProducts = productService.getHotProducts();
        System.out.printf("   热门产品数量: %d\n", hotProducts.size());
        
        System.out.println();
    }
    
    /**
     * 演示订单缓存和清除
     */
    private void demonstrateOrderCache() {
        System.out.println("📦 === 订单缓存和清除演示 ===");
        
        Long orderId = 2001L;
        
        // 获取订单信息
        System.out.println("🔍 获取订单信息:");
        Order order = orderService.getOrderById(orderId);
        System.out.printf("   订单: %s - 状态: %s\n", order.getOrderNo(), order.getStatus());
        
        // 更新订单状态 - 会清除缓存
        System.out.println("📝 更新订单状态 (清除缓存):");
        orderService.updateOrderStatus(orderId, "SHIPPED");
        
        // 再次获取订单 - 从数据库获取新状态
        System.out.println("🔄 再次获取订单信息:");
        Order updatedOrder = orderService.getOrderById(orderId);
        System.out.printf("   订单: %s - 新状态: %s\n", updatedOrder.getOrderNo(), updatedOrder.getStatus());
        
        System.out.println();
    }
    
    /**
     * 演示缓存性能对比
     */
    private void demonstratePerformance() {
        System.out.println("⚡ === 缓存性能对比演示 ===");
        
        int testCount = 100;
        Long userId = 1002L;
        
        // 预热缓存
        userService.getUserById(userId);
        
        // 测试无缓存性能
        System.out.println("🐌 无缓存性能测试:");
        long start = System.currentTimeMillis();
        for (int i = 0; i < testCount; i++) {
            userService.getUserByIdNoCache(userId);
        }
        long noCacheTime = System.currentTimeMillis() - start;
        System.out.printf("   %d次调用耗时: %dms (平均: %.2fms/次)\n", 
                         testCount, noCacheTime, (double)noCacheTime/testCount);
        
        // 测试有缓存性能
        System.out.println("🚀 有缓存性能测试:");
        start = System.currentTimeMillis();
        for (int i = 0; i < testCount; i++) {
            userService.getUserById(userId);
        }
        long cacheTime = System.currentTimeMillis() - start;
        System.out.printf("   %d次调用耗时: %dms (平均: %.2fms/次)\n", 
                         testCount, cacheTime, (double)cacheTime/testCount);
        
        System.out.printf("   🎯 性能提升: %.1fx\n", (double)noCacheTime/cacheTime);
        
        System.out.println();
    }
    
    /**
     * 演示不同缓存策略
     */
    private void demonstrateCacheStrategies() {
        System.out.println("🎯 === 缓存策略对比演示 ===");
        
        String productId = "PROD-002";
        
        // LOCAL_FIRST 策略
        System.out.println("🏠 LOCAL_FIRST 策略:");
        Product product1 = productService.getProductLocalFirst(productId);
        System.out.printf("   产品: %s\n", product1.getName());
        
        // REMOTE_FIRST 策略
        System.out.println("🌐 REMOTE_FIRST 策略:");
        Product product2 = productService.getProductRemoteFirst(productId);
        System.out.printf("   产品: %s\n", product2.getName());
        
        // LOCAL_ONLY 策略
        System.out.println("💾 LOCAL_ONLY 策略:");
        Product product3 = productService.getProductLocalOnly(productId);
        System.out.printf("   产品: %s\n", product3.getName());
        
        // REMOTE_ONLY 策略
        System.out.println("☁️ REMOTE_ONLY 策略:");
        Product product4 = productService.getProductRemoteOnly(productId);
        System.out.printf("   产品: %s\n", product4.getName());
        
        System.out.println();
    }
}