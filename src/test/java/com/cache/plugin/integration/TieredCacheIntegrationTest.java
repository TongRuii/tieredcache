package com.cache.plugin.integration;

import com.cache.plugin.config.TieredCacheAutoConfiguration;
import com.cache.plugin.example.User;
import com.cache.plugin.example.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 分层缓存集成测试
 */
@SpringBootTest(classes = {
    TieredCacheAutoConfiguration.class,
    UserService.class,
    IntegrationTestConfiguration.class
})
@ActiveProfiles("test")
public class TieredCacheIntegrationTest {
    
    @Autowired
    private UserService userService;
    
    @Test
    void testTieredCacheFlow() {
        Long userId = 1L;
        
        // 第一次调用 - 应该从数据库获取并缓存
        User user1 = userService.getUserById(userId);
        assertNotNull(user1);
        assertEquals("Alice", user1.getName());
        
        // 第二次调用 - 应该从本地缓存获取
        User user2 = userService.getUserById(userId);
        assertNotNull(user2);
        assertEquals("Alice", user2.getName());
        assertSame(user1, user2); // 应该是同一个对象实例（从缓存获取）
    }
    
    @Test
    void testLocalCacheOnly() {
        Long userId = 1L;
        
        // 测试本地缓存
        Object profile1 = userService.getUserProfile(userId);
        assertNotNull(profile1);
        
        Object profile2 = userService.getUserProfile(userId);
        assertNotNull(profile2);
    }
    
    @Test
    void testRemoteCacheOnly() {
        Long userId = 1L;
        
        // 测试远程缓存
        Object settings1 = userService.getUserSettings(userId);
        assertNotNull(settings1);
        
        Object settings2 = userService.getUserSettings(userId);
        assertNotNull(settings2);
    }
    
    @Test
    void testCacheEviction() {
        // 使用一个新用户进行测试，避免影响其他测试
        User newUser = userService.createUser("Test User For Eviction", 25, "test.eviction@example.com");
        assertNotNull(newUser);
        assertNotNull(newUser.getId());
        
        Long userId = newUser.getId();
        
        // 先获取用户信息，确保被缓存
        User originalUser = userService.getUserById(userId);
        assertNotNull(originalUser);
        
        String originalName = originalUser.getName();
        int originalAge = originalUser.getAge();
        
        // 更新用户信息，这应该清除缓存
        User updatedUser = new User(userId, originalName + " Updated", originalAge + 1, "test.updated@example.com");
        userService.updateUser(updatedUser);
        
        // 再次获取用户信息，应该从数据库获取新数据
        User newUserAfterUpdate = userService.getUserById(userId);
        assertNotNull(newUserAfterUpdate);
        assertEquals(originalName + " Updated", newUserAfterUpdate.getName());
        assertEquals(originalAge + 1, newUserAfterUpdate.getAge());
    }
    
    @Test
    void testCachePut() {
        // 创建新用户，结果应该被缓存
        User newUser = userService.createUser("David", 28, "david@example.com");
        assertNotNull(newUser);
        assertNotNull(newUser.getId());
        
        // 直接从缓存获取应该能找到
        User cachedUser = userService.getUserById(newUser.getId());
        assertNotNull(cachedUser);
        assertEquals("David", cachedUser.getName());
    }
    
    @Test
    void testCacheEvictWithNullId() {
        // 测试清除缓存时ID为null的情况
        User user = userService.getUserById(1L);
        assertNotNull(user);
        
        // 更新用户，但ID为null，不应该清除缓存
        User userWithNullId = new User(null, "Updated Name", 30, "updated@example.com");
        userService.updateUser(userWithNullId);
        
        // 应该仍然能从缓存中获取原用户
        User cachedUser = userService.getUserById(1L);
        assertSame(user, cachedUser);
    }
    
    @Test
    void testCacheWithInvalidId() {
        // 测试使用无效ID获取用户
        User user1 = userService.getUserById(-1L);
        User user2 = userService.getUserById(999L); // 不存在的用户
        
        assertNull(user1);
        assertNull(user2);
    }
    
    @Test
    void testGetAllUsers() {
        // 测试获取所有用户
        List<User> users1 = userService.getAllUsers(0, 2);
        List<User> users2 = userService.getAllUsers(0, 2);
        
        assertNotNull(users1);
        assertNotNull(users2);
        assertEquals(users1, users2);
    }
}