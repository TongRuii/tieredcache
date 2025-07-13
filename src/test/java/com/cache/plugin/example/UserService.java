package com.cache.plugin.example;

import com.cache.plugin.annotation.*;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 用户服务示例 - 展示缓存注解的使用
 */
@Service
public class UserService {
    
    // 模拟数据库
    private final Map<Long, User> userDatabase = new HashMap<>();
    
    public UserService() {
        // 初始化一些测试数据
        userDatabase.put(1L, new User(1L, "Alice", 25, "alice@example.com"));
        userDatabase.put(2L, new User(2L, "Bob", 30, "bob@example.com"));
        userDatabase.put(3L, new User(3L, "Charlie", 35, "charlie@example.com"));
    }
    
    /**
     * 使用分层缓存获取用户信息
     * 本地缓存优先，远程缓存作为备份
     */
    @TieredCache(
        local = @LocalCache(
            maxSize = 1000, 
            expireAfterWrite = 300,  // 5分钟
            expireAfterAccess = 600  // 10分钟
        ),
        remote = @RemoteCache(
            ttl = 3600,  // 1小时
            namespace = "user"
        ),
        key = "'user:' + #userId",
        strategy = CacheStrategy.LOCAL_FIRST,
        condition = "#userId != null && #userId > 0"
    )
    public User getUserById(Long userId) {
        System.out.println("Fetching user from database: " + userId);
        return userDatabase.get(userId);
    }
    
    /**
     * 仅使用本地缓存的用户配置
     */
    @LocalCache(
        key = "'user:profile:' + #userId",
        maxSize = 500,
        expireAfterWrite = 600,
        condition = "#userId != null"
    )
    public UserProfile getUserProfile(Long userId) {
        System.out.println("Fetching user profile from database: " + userId);
        User user = userDatabase.get(userId);
        if (user != null) {
            return new UserProfile(user.getId(), user.getName(), "Profile for " + user.getName());
        }
        return null;
    }
    
    /**
     * 仅使用远程缓存的用户设置
     */
    @RemoteCache(
        key = "'user:settings:' + #userId",
        ttl = 7200,  // 2小时
        namespace = "settings",
        condition = "#userId != null"
    )
    public UserSettings getUserSettings(Long userId) {
        System.out.println("Fetching user settings from database: " + userId);
        return new UserSettings(userId, "theme", "dark", true);
    }
    
    /**
     * 更新用户信息并清除相关缓存
     */
    @CacheEvict(
        key = "'user:' + #user.id",
        condition = "#user != null && #user.id != null"
    )
    public User updateUser(User user) {
        System.out.println("Updating user in database: " + user.getId());
        userDatabase.put(user.getId(), user);
        return user;
    }
    
    /**
     * 创建用户并缓存结果
     */
    @CachePut(
        key = "'user:' + #result.id",
        level = CacheLevel.ALL,
        ttl = 3600,
        condition = "#result != null"
    )
    public User createUser(String name, Integer age, String email) {
        System.out.println("Creating new user in database: " + name);
        Long newId = (long) (userDatabase.size() + 1);
        User newUser = new User(newId, name, age, email);
        userDatabase.put(newId, newUser);
        return newUser;
    }
    
    /**
     * 删除用户并清除所有相关缓存
     */
    @CacheEvict(
        key = "'user:' + #userId",
        beforeInvocation = false
    )
    public boolean deleteUser(Long userId) {
        System.out.println("Deleting user from database: " + userId);
        return userDatabase.remove(userId) != null;
    }
    
    /**
     * 获取所有用户 - 使用复杂的缓存键
     */
    @TieredCache(
        key = "'users:all:' + #page + ':' + #size",
        strategy = CacheStrategy.REMOTE_FIRST,
        local = @LocalCache(maxSize = 10, expireAfterWrite = 120),
        remote = @RemoteCache(ttl = 600)
    )
    public List<User> getAllUsers(int page, int size) {
        System.out.println("Fetching all users from database, page: " + page + ", size: " + size);
        return userDatabase.values().stream()
                .skip((long) page * size)
                .limit(size)
                .collect(Collectors.toList());
    }
    
    /**
     * 根据年龄范围查询用户 - 条件缓存
     */
    @TieredCache(
        key = "'users:age:' + #minAge + '-' + #maxAge",
        condition = "#minAge >= 0 && #maxAge > #minAge && (#maxAge - #minAge) <= 50",
        strategy = CacheStrategy.LOCAL_FIRST
    )
    public List<User> getUsersByAgeRange(int minAge, int maxAge) {
        System.out.println("Fetching users by age range from database: " + minAge + "-" + maxAge);
        return userDatabase.values().stream()
                .filter(user -> user.getAge() >= minAge && user.getAge() <= maxAge)
                .collect(Collectors.toList());
    }
    
    /**
     * 清除所有用户相关缓存
     */
    @CacheEvict(allEntries = true)
    public void clearAllUserCache() {
        System.out.println("Clearing all user cache");
    }
    
    /**
     * 批量更新用户 - 仅写入缓存
     */
    @TieredCache(
        mode = CacheMode.WRITE_ONLY,
        strategy = CacheStrategy.WRITE_THROUGH
    )
    public void batchUpdateUsers(List<User> users) {
        System.out.println("Batch updating users in database: " + users.size());
        for (User user : users) {
            userDatabase.put(user.getId(), user);
        }
    }
}