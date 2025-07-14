package com.cache.plugin.example.demo.controller;

import com.cache.plugin.example.demo.model.User;
import com.cache.plugin.example.demo.model.Product;
import com.cache.plugin.example.demo.model.Order;
import com.cache.plugin.example.demo.service.UserService;
import com.cache.plugin.example.demo.service.ProductService;
import com.cache.plugin.example.demo.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 演示控制器 - 提供Web接口展示缓存功能
 */
@RestController
@RequestMapping("/api/demo")
public class DemoController {
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private ProductService productService;
    
    @Autowired
    private OrderService orderService;
    
    /**
     * 首页 - 展示演示功能列表
     */
    @GetMapping("/")
    public Map<String, Object> home() {
        Map<String, Object> response = new HashMap<>();
        response.put("title", "TieredCache 演示应用");
        response.put("description", "展示分层缓存的各种使用场景");
        
        Map<String, String> endpoints = new HashMap<>();
        endpoints.put("GET /api/demo/user/{id}", "获取用户信息 (分层缓存)");
        endpoints.put("GET /api/demo/user/{id}/profile", "获取用户详情 (本地缓存)");
        endpoints.put("GET /api/demo/product/{id}", "获取产品信息 (远程优先)");
        endpoints.put("GET /api/demo/products/hot", "获取热门产品 (长时间缓存)");
        endpoints.put("GET /api/demo/order/{id}", "获取订单信息");
        endpoints.put("PUT /api/demo/order/{id}/status", "更新订单状态 (清除缓存)");
        endpoints.put("GET /api/demo/performance", "性能对比测试");
        endpoints.put("GET /api/demo/strategies", "缓存策略对比");
        
        response.put("endpoints", endpoints);
        return response;
    }
    
    /**
     * 获取用户信息 - 演示分层缓存
     */
    @GetMapping("/user/{id}")
    public Map<String, Object> getUser(@PathVariable Long id) {
        long start = System.currentTimeMillis();
        User user = userService.getUserById(id);
        long duration = System.currentTimeMillis() - start;
        
        Map<String, Object> response = new HashMap<>();
        response.put("user", user);
        response.put("duration_ms", duration);
        response.put("cache_strategy", "LOCAL_FIRST");
        response.put("note", "第一次调用较慢，后续调用会从缓存获取");
        
        return response;
    }
    
    /**
     * 获取用户详情 - 演示本地缓存
     */
    @GetMapping("/user/{id}/profile")
    public Map<String, Object> getUserProfile(@PathVariable Long id) {
        long start = System.currentTimeMillis();
        com.cache.plugin.example.demo.model.UserProfile profile = userService.getUserProfile(id);
        long duration = System.currentTimeMillis() - start;
        
        Map<String, Object> response = new HashMap<>();
        response.put("profile", profile);
        response.put("duration_ms", duration);
        response.put("cache_type", "LOCAL_ONLY");
        
        return response;
    }
    
    /**
     * 获取产品信息 - 演示远程优先策略
     */
    @GetMapping("/product/{id}")
    public Map<String, Object> getProduct(@PathVariable String id) {
        long start = System.currentTimeMillis();
        Product product = productService.getProductById(id);
        long duration = System.currentTimeMillis() - start;
        
        Map<String, Object> response = new HashMap<>();
        response.put("product", product);
        response.put("duration_ms", duration);
        response.put("cache_strategy", "REMOTE_FIRST");
        
        return response;
    }
    
    /**
     * 获取热门产品 - 演示长时间缓存
     */
    @GetMapping("/products/hot")
    public Map<String, Object> getHotProducts() {
        long start = System.currentTimeMillis();
        List<Product> products = productService.getHotProducts();
        long duration = System.currentTimeMillis() - start;
        
        Map<String, Object> response = new HashMap<>();
        response.put("products", products);
        response.put("count", products.size());
        response.put("duration_ms", duration);
        response.put("cache_ttl", "3600s");
        
        return response;
    }
    
    /**
     * 获取订单信息
     */
    @GetMapping("/order/{id}")
    public Map<String, Object> getOrder(@PathVariable Long id) {
        long start = System.currentTimeMillis();
        Order order = orderService.getOrderById(id);
        long duration = System.currentTimeMillis() - start;
        
        Map<String, Object> response = new HashMap<>();
        response.put("order", order);
        response.put("duration_ms", duration);
        
        return response;
    }
    
    /**
     * 更新订单状态 - 演示缓存清除
     */
    @PutMapping("/order/{id}/status")
    public Map<String, Object> updateOrderStatus(@PathVariable Long id, @RequestParam String status) {
        long start = System.currentTimeMillis();
        orderService.updateOrderStatus(id, status);
        long duration = System.currentTimeMillis() - start;
        
        Map<String, Object> response = new HashMap<>();
        response.put("orderId", id);
        response.put("newStatus", status);
        response.put("duration_ms", duration);
        response.put("action", "缓存已清除，下次获取将从数据库查询");
        
        return response;
    }
    
    /**
     * 性能对比测试
     */
    @GetMapping("/performance")
    public Map<String, Object> performanceTest() {
        Long userId = 1001L;
        int testCount = 50;
        
        // 预热缓存
        userService.getUserById(userId);
        
        // 测试无缓存性能
        long start = System.currentTimeMillis();
        for (int i = 0; i < testCount; i++) {
            userService.getUserByIdNoCache(userId);
        }
        long noCacheTime = System.currentTimeMillis() - start;
        
        // 测试有缓存性能
        start = System.currentTimeMillis();
        for (int i = 0; i < testCount; i++) {
            userService.getUserById(userId);
        }
        long cacheTime = System.currentTimeMillis() - start;
        
        Map<String, Object> response = new HashMap<>();
        response.put("test_count", testCount);
        response.put("no_cache_time_ms", noCacheTime);
        response.put("cache_time_ms", cacheTime);
        response.put("performance_improvement", String.format("%.1fx", (double)noCacheTime/cacheTime));
        response.put("avg_no_cache_ms", String.format("%.2f", (double)noCacheTime/testCount));
        response.put("avg_cache_ms", String.format("%.2f", (double)cacheTime/testCount));
        
        return response;
    }
    
    /**
     * 缓存策略对比
     */
    @GetMapping("/strategies")
    public Map<String, Object> cacheStrategies() {
        String productId = "STRATEGY-TEST";
        Map<String, Object> results = new HashMap<>();
        
        // LOCAL_FIRST 策略
        long start = System.currentTimeMillis();
        Product localFirst = productService.getProductLocalFirst(productId);
        long localFirstTime = System.currentTimeMillis() - start;
        
        // REMOTE_FIRST 策略
        start = System.currentTimeMillis();
        Product remoteFirst = productService.getProductRemoteFirst(productId);
        long remoteFirstTime = System.currentTimeMillis() - start;
        
        // LOCAL_ONLY 策略
        start = System.currentTimeMillis();
        Product localOnly = productService.getProductLocalOnly(productId);
        long localOnlyTime = System.currentTimeMillis() - start;
        
        // REMOTE_ONLY 策略
        start = System.currentTimeMillis();
        Product remoteOnly = productService.getProductRemoteOnly(productId);
        long remoteOnlyTime = System.currentTimeMillis() - start;
        
        Map<String, Object> strategies = new HashMap<>();
        
        Map<String, Object> localFirstMap = new HashMap<>();
        localFirstMap.put("duration_ms", localFirstTime);
        localFirstMap.put("description", "本地优先，快速访问");
        strategies.put("LOCAL_FIRST", localFirstMap);
        
        Map<String, Object> remoteFirstMap = new HashMap<>();
        remoteFirstMap.put("duration_ms", remoteFirstTime);
        remoteFirstMap.put("description", "远程优先，分布式一致");
        strategies.put("REMOTE_FIRST", remoteFirstMap);
        
        Map<String, Object> localOnlyMap = new HashMap<>();
        localOnlyMap.put("duration_ms", localOnlyTime);
        localOnlyMap.put("description", "仅本地，最快访问");
        strategies.put("LOCAL_ONLY", localOnlyMap);
        
        Map<String, Object> remoteOnlyMap = new HashMap<>();
        remoteOnlyMap.put("duration_ms", remoteOnlyTime);
        remoteOnlyMap.put("description", "仅远程，持久化存储");
        strategies.put("REMOTE_ONLY", remoteOnlyMap);
        
        results.put("product_id", productId);
        results.put("strategies", strategies);
        results.put("note", "首次调用时间，后续调用会从对应缓存获取");
        
        return results;
    }
    
    /**
     * 缓存统计信息
     */
    @GetMapping("/cache/stats")
    public Map<String, Object> cacheStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("message", "缓存统计功能");
        stats.put("note", "可以通过 /actuator/metrics 端点查看详细指标");
        Map<String, String> endpoints = new HashMap<>();
        endpoints.put("health", "/actuator/health");
        endpoints.put("metrics", "/actuator/metrics");
        endpoints.put("caches", "/actuator/caches");
        stats.put("endpoints", endpoints);
        
        return stats;
    }
}