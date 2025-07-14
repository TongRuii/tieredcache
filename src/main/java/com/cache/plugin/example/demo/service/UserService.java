package com.cache.plugin.example.demo.service;

import com.cache.plugin.annotation.TieredCache;
import com.cache.plugin.annotation.LocalCache;
import com.cache.plugin.annotation.RemoteCache;
import com.cache.plugin.annotation.CacheStrategy;
import com.cache.plugin.example.demo.model.User;
import com.cache.plugin.example.demo.model.UserProfile;
import org.springframework.stereotype.Service;

/**
 * 用户服务 - 演示分层缓存的使用
 */
@Service
public class UserService {
    
    /**
     * 获取用户信息 - 使用分层缓存 (LOCAL_FIRST策略)
     */
    @TieredCache(
        local = @LocalCache(maxSize = 1000, expireAfterWrite = 300),
        remote = @RemoteCache(ttl = 3600),
        key = "'user:' + #userId",
        strategy = CacheStrategy.LOCAL_FIRST
    )
    public User getUserById(Long userId) {
        // 模拟数据库查询延迟
        simulateDbDelay(100);
        
        return createMockUser(userId);
    }
    
    /**
     * 获取用户信息 - 无缓存版本 (用于性能对比)
     */
    public User getUserByIdNoCache(Long userId) {
        // 模拟数据库查询延迟
        simulateDbDelay(50);
        
        return createMockUser(userId);
    }
    
    /**
     * 获取用户详情 - 仅使用本地缓存
     */
    @LocalCache(
        key = "'user:profile:' + #userId",
        maxSize = 500,
        expireAfterWrite = 600
    )
    public UserProfile getUserProfile(Long userId) {
        // 模拟数据库查询
        simulateDbDelay(80);
        
        User user = createMockUser(userId);
        return new UserProfile(userId, user.getName() + " (详情)");
    }
    
    /**
     * 获取用户设置 - 仅使用远程缓存
     */
    @RemoteCache(
        key = "'user:settings:' + #userId",
        ttl = 1800
    )
    public UserSettings getUserSettings(Long userId) {
        // 模拟数据库查询
        simulateDbDelay(60);
        
        return new UserSettings(userId, "zh-CN", "dark", true);
    }
    
    /**
     * 根据年龄范围获取用户列表 - 条件缓存
     */
    @TieredCache(
        local = @LocalCache(maxSize = 100, expireAfterWrite = 180),
        remote = @RemoteCache(ttl = 900),
        key = "'users:age:' + #minAge + '-' + #maxAge",
        condition = "#maxAge - #minAge <= 20" // 只有年龄范围<=20才缓存
    )
    public java.util.List<User> getUsersByAgeRange(int minAge, int maxAge) {
        // 模拟数据库查询
        simulateDbDelay(200);
        
        java.util.List<User> users = new java.util.ArrayList<>();
        for (int i = 0; i < 5; i++) {
            users.add(createMockUser(1000L + i));
        }
        return users;
    }
    
    /**
     * 创建用户 - 结果会被缓存
     */
    @TieredCache(
        local = @LocalCache(maxSize = 1000, expireAfterWrite = 300),
        remote = @RemoteCache(ttl = 3600),
        key = "'user:' + #result.id"
    )
    public User createUser(String name, Integer age, String email) {
        // 模拟数据库插入
        simulateDbDelay(150);
        
        Long newId = System.currentTimeMillis() % 10000;
        return new User(newId, name, email, age, "IT部门");
    }
    
    /**
     * 更新用户信息 - 清除相关缓存
     */
    @TieredCache(
        local = @LocalCache(maxSize = 1000, expireAfterWrite = 300),
        remote = @RemoteCache(ttl = 3600),
        key = "'user:' + #user.id"
    )
    public User updateUser(User user) {
        // 模拟数据库更新
        simulateDbDelay(120);
        
        user.setUpdatedAt(java.time.LocalDateTime.now());
        return user;
    }
    
    /**
     * 获取所有用户 - 分页缓存
     */
    @TieredCache(
        local = @LocalCache(maxSize = 50, expireAfterWrite = 120),
        remote = @RemoteCache(ttl = 600),
        key = "'users:page:' + #page + ':size:' + #size"
    )
    public java.util.List<User> getAllUsers(int page, int size) {
        // 模拟数据库分页查询
        simulateDbDelay(180);
        
        java.util.List<User> users = new java.util.ArrayList<>();
        for (int i = 0; i < size; i++) {
            users.add(createMockUser((long)(page * size + i + 1)));
        }
        return users;
    }
    
    /**
     * 清除所有用户缓存
     */
    public void clearAllUserCache() {
        // 这里可以通过缓存管理器清除特定前缀的缓存
        System.out.println("清除所有用户相关缓存");
    }
    
    /**
     * 创建模拟用户数据
     */
    private User createMockUser(Long userId) {
        String[] names = {"张三", "李四", "王五", "赵六", "钱七", "孙八", "周九", "吴十"};
        String[] departments = {"技术部", "产品部", "运营部", "市场部", "人事部"};
        
        String name = names[(int)(userId % names.length)];
        String department = departments[(int)(userId % departments.length)];
        String email = name.toLowerCase() + userId + "@example.com";
        Integer age = 20 + (int)(userId % 40);
        
        return new User(userId, name, email, age, department);
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
 * 用户设置类
 */
class UserSettings {
    private Long userId;
    private String language;
    private String theme;
    private Boolean emailNotification;
    
    public UserSettings(Long userId, String language, String theme, Boolean emailNotification) {
        this.userId = userId;
        this.language = language;
        this.theme = theme;
        this.emailNotification = emailNotification;
    }
    
    // Getters
    public Long getUserId() { return userId; }
    public String getLanguage() { return language; }
    public String getTheme() { return theme; }
    public Boolean getEmailNotification() { return emailNotification; }
}