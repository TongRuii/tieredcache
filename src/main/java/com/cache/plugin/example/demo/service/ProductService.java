package com.cache.plugin.example.demo.service;

import com.cache.plugin.annotation.TieredCache;
import com.cache.plugin.annotation.LocalCache;
import com.cache.plugin.annotation.RemoteCache;
import com.cache.plugin.annotation.CacheStrategy;
import com.cache.plugin.example.demo.model.Product;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 产品服务 - 演示不同缓存策略的使用
 */
@Service
public class ProductService {
    
    /**
     * 获取产品信息 - 远程优先策略
     */
    @TieredCache(
        local = @LocalCache(maxSize = 500, expireAfterWrite = 180),
        remote = @RemoteCache(ttl = 1800),
        key = "'product:' + #productId",
        strategy = CacheStrategy.REMOTE_FIRST
    )
    public Product getProductById(String productId) {
        // 模拟数据库查询延迟
        simulateDbDelay(120);
        
        return createMockProduct(productId);
    }
    
    /**
     * 获取产品信息 - 本地优先策略
     */
    @TieredCache(
        local = @LocalCache(maxSize = 200, expireAfterWrite = 120),
        remote = @RemoteCache(ttl = 600),
        key = "'product:local:' + #productId",
        strategy = CacheStrategy.LOCAL_FIRST
    )
    public Product getProductLocalFirst(String productId) {
        simulateDbDelay(100);
        return createMockProduct(productId);
    }
    
    /**
     * 获取产品信息 - 远程优先策略
     */
    @TieredCache(
        local = @LocalCache(maxSize = 200, expireAfterWrite = 120),
        remote = @RemoteCache(ttl = 600),
        key = "'product:remote:' + #productId",
        strategy = CacheStrategy.REMOTE_FIRST
    )
    public Product getProductRemoteFirst(String productId) {
        simulateDbDelay(100);
        return createMockProduct(productId);
    }
    
    /**
     * 获取产品信息 - 仅本地缓存
     */
    @LocalCache(
        key = "'product:local-only:' + #productId",
        maxSize = 100,
        expireAfterWrite = 300
    )
    public Product getProductLocalOnly(String productId) {
        simulateDbDelay(80);
        return createMockProduct(productId);
    }
    
    /**
     * 获取产品信息 - 仅远程缓存
     */
    @RemoteCache(
        key = "'product:remote-only:' + #productId",
        ttl = 900
    )
    public Product getProductRemoteOnly(String productId) {
        simulateDbDelay(80);
        return createMockProduct(productId);
    }
    
    /**
     * 获取热门产品列表 - 长时间缓存
     */
    @TieredCache(
        local = @LocalCache(maxSize = 10, expireAfterWrite = 300),
        remote = @RemoteCache(ttl = 3600),
        key = "'products:hot'",
        strategy = CacheStrategy.LOCAL_FIRST
    )
    public List<Product> getHotProducts() {
        // 模拟复杂查询
        simulateDbDelay(300);
        
        List<Product> hotProducts = new ArrayList<>();
        String[] hotIds = {"HOT-001", "HOT-002", "HOT-003", "HOT-004", "HOT-005"};
        
        for (String id : hotIds) {
            Product product = createMockProduct(id);
            product.setIsHot(true);
            hotProducts.add(product);
        }
        
        return hotProducts;
    }
    
    /**
     * 根据分类获取产品 - 条件缓存
     */
    @TieredCache(
        local = @LocalCache(maxSize = 50, expireAfterWrite = 240),
        remote = @RemoteCache(ttl = 1200),
        key = "'products:category:' + #category + ':page:' + #page",
        condition = "#category != null && #category.length() > 0"
    )
    public List<Product> getProductsByCategory(String category, int page) {
        simulateDbDelay(200);
        
        List<Product> products = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            String productId = category.toUpperCase() + "-" + String.format("%03d", page * 10 + i + 1);
            Product product = createMockProduct(productId);
            product.setCategory(category);
            products.add(product);
        }
        
        return products;
    }
    
    /**
     * 搜索产品 - 短时间缓存
     */
    @TieredCache(
        local = @LocalCache(maxSize = 100, expireAfterWrite = 60),
        remote = @RemoteCache(ttl = 300),
        key = "'products:search:' + #keyword + ':page:' + #page"
    )
    public List<Product> searchProducts(String keyword, int page) {
        simulateDbDelay(250);
        
        List<Product> results = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            String productId = "SEARCH-" + keyword.toUpperCase() + "-" + (page * 5 + i + 1);
            Product product = createMockProduct(productId);
            product.setName(keyword + " 相关产品 " + (i + 1));
            results.add(product);
        }
        
        return results;
    }
    
    /**
     * 更新产品信息 - 会更新缓存
     */
    @TieredCache(
        local = @LocalCache(maxSize = 500, expireAfterWrite = 180),
        remote = @RemoteCache(ttl = 1800),
        key = "'product:' + #product.id"
    )
    public Product updateProduct(Product product) {
        simulateDbDelay(150);
        
        product.setUpdatedAt(java.time.LocalDateTime.now());
        return product;
    }
    
    /**
     * 获取产品库存 - 高频更新，短时间缓存
     */
    @LocalCache(
        key = "'product:stock:' + #productId",
        maxSize = 1000,
        expireAfterWrite = 30 // 30秒过期，保证库存数据相对实时
    )
    public Integer getProductStock(String productId) {
        simulateDbDelay(50);
        
        // 模拟库存数据
        return (int)(Math.random() * 1000) + 1;
    }
    
    /**
     * 创建模拟产品数据
     */
    private Product createMockProduct(String productId) {
        String[] categories = {"电子产品", "服装", "家居", "图书", "运动", "美妆"};
        String[] brands = {"苹果", "华为", "小米", "三星", "索尼", "戴尔"};
        String[] names = {"智能手机", "笔记本电脑", "平板电脑", "智能手表", "耳机", "音响"};
        
        int hash = Math.abs(productId.hashCode());
        String category = categories[hash % categories.length];
        String brand = brands[hash % brands.length];
        String name = brand + " " + names[hash % names.length];
        
        BigDecimal price = BigDecimal.valueOf(100 + (hash % 9900));
        String description = "这是一款优质的" + name + "，来自" + brand + "品牌";
        
        Product product = new Product(productId, name, description, price, category);
        product.setBrand(brand);
        product.setStock(50 + (hash % 950));
        product.setIsHot(hash % 10 == 0); // 10%的产品是热门
        
        return product;
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