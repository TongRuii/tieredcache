package com.cache.plugin.example;

import com.cache.plugin.config.TieredCacheAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * 缓存使用示例
 * 
 * 这个示例展示了如何在Spring Boot应用中使用分层缓存插件
 */
@SpringBootApplication(scanBasePackages = "com.cache.plugin")
public class CacheUsageExample {
    
    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(CacheUsageExample.class, args);
        
        // 获取用户服务
        UserService userService = context.getBean(UserService.class);
        
        System.out.println("=== 分层缓存使用示例 ===");
        
        // 示例1：基本的分层缓存使用
        demonstrateBasicCaching(userService);
        
        // 示例2：本地缓存使用
        demonstrateLocalCaching(userService);
        
        // 示例3：远程缓存使用
        demonstrateRemoteCaching(userService);
        
        // 示例4：缓存清除
        demonstrateCacheEviction(userService);
        
        // 示例5：条件缓存
        demonstrateConditionalCaching(userService);
        
        // 示例6：缓存更新
        demonstrateCachePut(userService);
        
        context.close();
    }
    
    /**
     * 演示基本的分层缓存使用
     */
    private static void demonstrateBasicCaching(UserService userService) {
        System.out.println("\n--- 基本分层缓存演示 ---");
        
        Long userId = 1L;
        
        // 第一次调用 - 从数据库获取
        System.out.println("第一次调用 getUserById:");
        User user1 = userService.getUserById(userId);
        System.out.println("结果: " + user1);
        
        // 第二次调用 - 从本地缓存获取
        System.out.println("\n第二次调用 getUserById:");
        User user2 = userService.getUserById(userId);
        System.out.println("结果: " + user2);
        System.out.println("是否为同一对象: " + (user1 == user2));
    }
    
    /**
     * 演示本地缓存使用
     */
    private static void demonstrateLocalCaching(UserService userService) {
        System.out.println("\n--- 本地缓存演示 ---");
        
        Long userId = 1L;
        
        System.out.println("第一次调用 getUserProfile:");
        UserProfile profile1 = userService.getUserProfile(userId);
        System.out.println("结果: " + profile1);
        
        System.out.println("\n第二次调用 getUserProfile:");
        UserProfile profile2 = userService.getUserProfile(userId);
        System.out.println("结果: " + profile2);
    }
    
    /**
     * 演示远程缓存使用
     */
    private static void demonstrateRemoteCaching(UserService userService) {
        System.out.println("\n--- 远程缓存演示 ---");
        
        Long userId = 1L;
        
        System.out.println("第一次调用 getUserSettings:");
        UserSettings settings1 = userService.getUserSettings(userId);
        System.out.println("结果: " + settings1);
        
        System.out.println("\n第二次调用 getUserSettings:");
        UserSettings settings2 = userService.getUserSettings(userId);
        System.out.println("结果: " + settings2);
    }
    
    /**
     * 演示缓存清除
     */
    private static void demonstrateCacheEviction(UserService userService) {
        System.out.println("\n--- 缓存清除演示 ---");
        
        Long userId = 2L;
        
        // 先获取用户信息
        System.out.println("获取原始用户信息:");
        User originalUser = userService.getUserById(userId);
        System.out.println("结果: " + originalUser);
        
        // 更新用户信息（会清除缓存）
        System.out.println("\n更新用户信息:");
        User updatedUser = new User(userId, "Bob Updated", 31, "bob.updated@example.com");
        userService.updateUser(updatedUser);
        
        // 再次获取用户信息
        System.out.println("\n再次获取用户信息:");
        User newUser = userService.getUserById(userId);
        System.out.println("结果: " + newUser);
    }
    
    /**
     * 演示条件缓存
     */
    private static void demonstrateConditionalCaching(UserService userService) {
        System.out.println("\n--- 条件缓存演示 ---");
        
        // 满足条件的查询 - 会被缓存
        System.out.println("满足条件的年龄范围查询 (25-35):");
        var users1 = userService.getUsersByAgeRange(25, 35);
        System.out.println("结果数量: " + users1.size());
        
        System.out.println("\n再次执行相同查询:");
        var users2 = userService.getUsersByAgeRange(25, 35);
        System.out.println("结果数量: " + users2.size());
        
        // 不满足条件的查询 - 不会被缓存
        System.out.println("\n不满足条件的年龄范围查询 (0-100):");
        var users3 = userService.getUsersByAgeRange(0, 100);
        System.out.println("结果数量: " + users3.size());
    }
    
    /**
     * 演示缓存更新
     */
    private static void demonstrateCachePut(UserService userService) {
        System.out.println("\n--- 缓存更新演示 ---");
        
        // 创建新用户（结果会被缓存）
        System.out.println("创建新用户:");
        User newUser = userService.createUser("Eve", 27, "eve@example.com");
        System.out.println("创建的用户: " + newUser);
        
        // 直接从缓存获取
        System.out.println("\n从缓存获取新创建的用户:");
        User cachedUser = userService.getUserById(newUser.getId());
        System.out.println("缓存中的用户: " + cachedUser);
        System.out.println("是否为同一对象: " + (newUser == cachedUser));
    }
}