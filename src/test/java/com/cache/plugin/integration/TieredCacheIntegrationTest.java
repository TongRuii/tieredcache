package com.cache.plugin.integration;

import com.cache.plugin.config.TieredCacheAutoConfiguration;
import com.cache.plugin.example.User;
import com.cache.plugin.example.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

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
        Long userId = 1L;
        
        // 先获取用户信息，确保被缓存
        User originalUser = userService.getUserById(userId);
        assertNotNull(originalUser);
        
        // 更新用户信息，这应该清除缓存
        User updatedUser = new User(userId, "Alice Updated", 26, "alice.updated@example.com");
        userService.updateUser(updatedUser);
        
        // 再次获取用户信息，应该从数据库获取新数据
        User newUser = userService.getUserById(userId);
        assertNotNull(newUser);
        assertEquals("Alice Updated", newUser.getName());
        assertEquals(26, newUser.getAge());
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
    void testConditionalCaching() {
        // 测试条件缓存 - 有效条件
        java.util.List<User> users1 = userService.getUsersByAgeRange(20, 35);
        assertNotNull(users1);
        
        java.util.List<User> users2 = userService.getUsersByAgeRange(20, 35);
        assertNotNull(users2);
        assertEquals(users1.size(), users2.size());
        
        // 测试条件缓存 - 无效条件（年龄范围太大）
        java.util.List<User> users3 = userService.getUsersByAgeRange(0, 100);
        assertNotNull(users3);
        // 这个调用不应该被缓存，因为不满足条件
    }
    
    @Test
    void testClearAllCache() {
        // 先缓存一些数据
        userService.getUserById(1L);
        userService.getUserById(2L);
        
        // 清除所有缓存
        userService.clearAllUserCache();
        
        // 验证缓存已被清除（这里主要验证不抛异常）
        User user = userService.getUserById(1L);
        assertNotNull(user);
    }
    
    @Test
    void testPaginatedResults() {
        // 测试分页结果缓存
        java.util.List<User> page1 = userService.getAllUsers(0, 2);
        assertNotNull(page1);
        assertTrue(page1.size() <= 2);
        
        java.util.List<User> page1Again = userService.getAllUsers(0, 2);
        assertNotNull(page1Again);
        assertEquals(page1.size(), page1Again.size());
        
        java.util.List<User> page2 = userService.getAllUsers(1, 2);
        assertNotNull(page2);
        // 不同的分页参数应该有不同的缓存键
    }
    
    @Test
    void testNullAndInvalidInputs() {
        // 测试null输入
        User nullUser = userService.getUserById(null);
        assertNull(nullUser);
        
        // 测试无效ID
        User invalidUser = userService.getUserById(-1L);
        assertNull(invalidUser);
        
        // 测试不存在的用户
        User nonExistentUser = userService.getUserById(999L);
        assertNull(nonExistentUser);
    }
}