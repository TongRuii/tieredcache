# TieredCache API 文档

## 1. 概述

本文档详细描述了 TieredCache 提供的所有公共 API，包括注解、配置类和核心组件。

## 2. 注解 API

### 2.1 @TieredCache

用于标记需要分层缓存的方法。

#### 属性

| 属性 | 类型 | 默认值 | 描述 |
|------|------|--------|------|
| key | String | "" | 缓存键表达式，支持 SpEL |
| keyGenerator | String | "" | 自定义键生成器名称 |
| condition | String | "" | 缓存条件表达式，支持 SpEL |
| strategy | CacheStrategy | LOCAL_FIRST | 缓存策略 |
| ttl | long | 0 | 缓存过期时间（秒），0 表示使用配置默认值 |
| mode | CacheMode | READ_WRITE | 缓存模式 |
| local | LocalCache | @LocalCache | 本地缓存配置 |
| remote | RemoteCache | @RemoteCache | 远程缓存配置 |

#### 示例

```java
@TieredCache(
    key = "#userId",
    strategy = CacheStrategy.LOCAL_FIRST,
    ttl = 3600
)
public User getUserById(Long userId) {
    return userRepository.findById(userId);
}
```

### 2.2 @LocalCache

用于标记只需要本地缓存的方法。

#### 属性

| 属性 | 类型 | 默认值 | 描述 |
|------|------|--------|------|
| key | String | "" | 缓存键表达式，支持 SpEL |
| keyGenerator | String | "" | 自定义键生成器名称 |
| condition | String | "" | 缓存条件表达式，支持 SpEL |
| maxSize | int | 1000 | 最大缓存条目数 |
| expireAfterWrite | long | 300 | 写入后过期时间（秒） |
| expireAfterAccess | long | 0 | 访问后过期时间（秒），0 表示禁用 |

#### 示例

```java
@LocalCache(
    key = "#username",
    maxSize = 5000,
    expireAfterWrite = 600
)
public UserProfile getUserProfile(String username) {
    return profileRepository.findByUsername(username);
}
```

### 2.3 @RemoteCache

用于标记只需要远程缓存的方法。

#### 属性

| 属性 | 类型 | 默认值 | 描述 |
|------|------|--------|------|
| key | String | "" | 缓存键表达式，支持 SpEL |
| keyGenerator | String | "" | 自定义键生成器名称 |
| condition | String | "" | 缓存条件表达式，支持 SpEL |
| ttl | long | 3600 | 缓存过期时间（秒） |

#### 示例

```java
@RemoteCache(
    key = "'user_permissions_' + #userId",
    ttl = 1800
)
public Set<String> getUserPermissions(Long userId) {
    return permissionRepository.findUserPermissions(userId);
}
```

### 2.4 @CacheEvict

用于清除指定的缓存项。

#### 属性

| 属性 | 类型 | 默认值 | 描述 |
|------|------|--------|------|
| key | String | "" | 要清除的缓存键表达式 |
| keyGenerator | String | "" | 自定义键生成器名称 |
| condition | String | "" | 清除条件表达式 |
| allEntries | boolean | false | 是否清除所有缓存项 |
| beforeInvocation | boolean | false | 是否在方法执行前清除缓存 |

#### 示例

```java
@CacheEvict(key = "#user.id")
public User updateUser(User user) {
    return userRepository.save(user);
}
```

## 3. 枚举 API

### 3.1 CacheStrategy

定义缓存策略。

| 值 | 描述 |
|----|------|
| LOCAL_FIRST | 本地优先策略 |
| REMOTE_FIRST | 远程优先策略 |
| LOCAL_ONLY | 仅本地缓存 |
| REMOTE_ONLY | 仅远程缓存 |

### 3.2 CacheMode

定义缓存模式。

| 值 | 描述 |
|----|------|
| READ_WRITE | 读写模式 |
| READ_ONLY | 只读模式 |
| WRITE_ONLY | 只写模式 |

## 4. 配置类 API

### 4.1 TieredCacheProperties

TieredCache 主配置类。

#### 属性

| 属性 | 类型 | 描述 |
|------|------|------|
| enabled | boolean | 是否启用缓存 |
| local | LocalCacheProperties | 本地缓存配置 |
| remote | RemoteCacheProperties | 远程缓存配置 |
| sync | CacheSyncProperties | 缓存同步配置 |
| security | CacheSecurityProperties | 安全配置 |
| monitoring | CacheMonitoringProperties | 监控配置 |

### 4.2 LocalCacheProperties

本地缓存配置。

#### 属性

| 属性 | 类型 | 默认值 | 描述 |
|------|------|--------|------|
| enabled | boolean | true | 是否启用本地缓存 |
| provider | String | "caffeine" | 本地缓存提供商 |
| maxSize | int | 10000 | 最大缓存条目数 |
| expireAfterWrite | Duration | PT5M | 写入后过期时间 |
| expireAfterAccess | Duration | PT10M | 访问后过期时间 |

### 4.3 RemoteCacheProperties

远程缓存配置。

#### 属性

| 属性 | 类型 | 默认值 | 描述 |
|------|------|--------|------|
| enabled | boolean | true | 是否启用远程缓存 |
| provider | String | "redis" | 远程缓存提供商 |
| host | String | "localhost" | Redis 主机地址 |
| port | int | 6379 | Redis 端口 |
| database | int | 0 | Redis 数据库索引 |
| password | String | "" | Redis 密码 |
| timeout | Duration | PT2S | 连接超时时间 |
| ttl | Duration | PT1H | 默认过期时间 |

## 5. 核心组件 API

### 5.1 TieredCacheManager

分层缓存管理器。

#### 方法

| 方法签名 | 描述 |
|----------|------|
| `<T> T get(String key, Class<T> type, CacheStrategy strategy)` | 根据策略获取缓存值 |
| `void put(String key, Object value, CacheStrategy strategy, Duration ttl)` | 根据策略放入缓存值 |
| `void evict(String key, CacheStrategy strategy)` | 根据策略清除缓存值 |
| `void clear(CacheStrategy strategy)` | 根据策略清除所有缓存值 |

### 5.2 LocalCache<K, V>

本地缓存接口。

#### 方法

| 方法签名 | 描述 |
|----------|------|
| `V get(K key)` | 获取缓存值 |
| `void put(K key, V value)` | 放入缓存值 |
| `void put(K key, V value, Duration ttl)` | 放入带过期时间的缓存值 |
| `void evict(K key)` | 清除缓存值 |
| `void clear()` | 清除所有缓存值 |

### 5.3 RemoteCache<K, V>

远程缓存接口。

#### 方法

| 方法签名 | 描述 |
|----------|------|
| `V get(K key)` | 获取缓存值 |
| `void put(K key, V value)` | 放入缓存值 |
| `void put(K key, V value, Duration ttl)` | 放入带过期时间的缓存值 |
| `void evict(K key)` | 清除缓存值 |
| `void clear()` | 清除所有缓存值 |

## 6. 监控 API

### 6.1 CacheMetrics

缓存指标收集器。

#### 方法

| 方法签名 | 描述 |
|----------|------|
| `void recordHit(CacheType type)` | 记录缓存命中 |
| `void recordMiss()` | 记录缓存未命中 |
| `void recordError(CacheOperation operation)` | 记录缓存操作错误 |
| `CacheMetricsSnapshot getSnapshot()` | 获取指标快照 |

## 7. 安全 API

### 7.1 DataEncryptionService

数据加密服务。

#### 方法

| 方法签名 | 描述 |
|----------|------|
| `String encrypt(String plaintext)` | 加密数据 |
| `String decrypt(String ciphertext)` | 解密数据 |
| `boolean isEnabled()` | 是否启用加密 |

## 8. 使用示例

### 8.1 基本用法

```java
@Service
public class UserService {
    
    @TieredCache(
        key = "#userId",
        strategy = CacheStrategy.LOCAL_FIRST,
        ttl = 3600
    )
    public User getUserById(Long userId) {
        return userRepository.findById(userId);
    }
    
    @CacheEvict(key = "#user.id")
    public User updateUser(User user) {
        return userRepository.save(user);
    }
}
```

### 8.2 条件缓存

```java
@Service
public class ProductService {
    
    @TieredCache(
        key = "#productId",
        condition = "#productId != null && #productId > 0"
    )
    public Product getProduct(Long productId) {
        return productRepository.findById(productId);
    }
}
```

### 8.3 复杂键表达式

```java
@Service
public class OrderService {
    
    @TieredCache(
        key = "#userId + '_' + #status",
        strategy = CacheStrategy.REMOTE_FIRST
    )
    public List<Order> getOrdersByUserAndStatus(Long userId, String status) {
        return orderRepository.findByUserIdAndStatus(userId, status);
    }
}
```