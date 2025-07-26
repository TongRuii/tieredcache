# TieredCache 用户指南

## 1. 简介

TieredCache 是一个功能强大且易于使用的分层缓存解决方案，专为 Spring Boot 应用设计。它结合了本地缓存的高速度和远程缓存的一致性，提供了高性能、高可用的缓存服务。

## 2. 快速开始

### 2.1 环境要求

- Java 8 或更高版本
- Spring Boot 2.7.x 或更高版本
- Maven 或 Gradle 构建工具

### 2.2 添加依赖

#### Maven

在 `pom.xml` 中添加以下依赖：

```xml
<dependency>
    <groupId>com.cache.plugin</groupId>
    <artifactId>tieredcache-spring-boot-starter</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

#### Gradle

在 `build.gradle` 中添加以下依赖：

```gradle
implementation 'com.cache.plugin:tieredcache-spring-boot-starter:1.0.0-SNAPSHOT'
```

### 2.3 基本配置

在 `application.yml` 中添加基本配置：

```yaml
tiered-cache:
  enabled: true
  local:
    provider: caffeine
    max-size: 10000
    expire-after-write: 300s
  remote:
    provider: redis
    host: localhost
    port: 6379
    ttl: 3600s
```

### 2.4 使用缓存

在服务类的方法上添加缓存注解：

```java
@Service
public class UserService {
    
    @TieredCache(key = "#userId")
    public User getUserById(Long userId) {
        return userRepository.findById(userId);
    }
}
```

## 3. 详细配置

### 3.1 本地缓存配置

#### Caffeine 配置示例

```yaml
tiered-cache:
  local:
    provider: caffeine
    max-size: 10000
    expire-after-write: 300s
    expire-after-access: 600s
```

#### 属性说明

| 属性 | 默认值 | 描述 |
|------|--------|------|
| provider | caffeine | 本地缓存提供商 |
| max-size | 10000 | 最大缓存条目数 |
| expire-after-write | 300s | 写入后过期时间 |
| expire-after-access | 600s | 访问后过期时间 |

### 3.2 远程缓存配置

#### Redis 配置示例

```yaml
tiered-cache:
  remote:
    provider: redis
    enabled: true
    host: localhost
    port: 6379
    database: 0
    password: your_password
    timeout: 2s
    ttl: 3600s
```

#### 属性说明

| 属性 | 默认值 | 描述 |
|------|--------|------|
| provider | redis | 远程缓存提供商 |
| enabled | true | 是否启用远程缓存 |
| host | localhost | Redis 主机地址 |
| port | 6379 | Redis 端口 |
| database | 0 | Redis 数据库索引 |
| password | "" | Redis 密码 |
| timeout | 2s | 连接超时时间 |
| ttl | 3600s | 默认过期时间 |

### 3.3 缓存同步配置

```yaml
tiered-cache:
  sync:
    enabled: true
    mode: redis-pubsub
    channel: tiered-cache-sync
```

### 3.4 安全配置

```yaml
tiered-cache:
  security:
    encryption:
      enabled: true
      algorithm: AES-256-GCM
      key: your-encryption-key
    access-control:
      enabled: true
      roles:
        - ADMIN
        - USER
```

### 3.5 监控配置

```yaml
tiered-cache:
  monitoring:
    metrics:
      enabled: true
    health:
      enabled: true
    logging:
      level: INFO
```

## 4. 缓存注解详解

### 4.1 @TieredCache

最常用的缓存注解，支持完整的分层缓存功能。

```java
@Service
public class ExampleService {
    
    @TieredCache(
        key = "#id",
        strategy = CacheStrategy.LOCAL_FIRST,
        ttl = 1800,
        condition = "#id != null"
    )
    public String getData(Long id) {
        // 方法实现
        return "data";
    }
}
```

### 4.2 @LocalCache

仅使用本地缓存的注解。

```java
@Service
public class ExampleService {
    
    @LocalCache(
        key = "#name",
        maxSize = 5000,
        expireAfterWrite = 600
    )
    public UserProfile getProfile(String name) {
        // 方法实现
        return new UserProfile();
    }
}
```

### 4.3 @RemoteCache

仅使用远程缓存的注解。

```java
@Service
public class ExampleService {
    
    @RemoteCache(
        key = "'config_' + #key",
        ttl = 3600
    )
    public String getConfig(String key) {
        // 方法实现
        return "config";
    }
}
```

### 4.4 @CacheEvict

用于清除缓存的注解。

```java
@Service
public class ExampleService {
    
    @CacheEvict(key = "#id")
    public void updateData(Long id, String data) {
        // 更新数据
    }
    
    @CacheEvict(allEntries = true)
    public void clearAllCache() {
        // 清除所有缓存
    }
}
```

## 5. 缓存策略

### 5.1 LOCAL_FIRST（默认）

本地优先策略，先查询本地缓存，再查询远程缓存。

```java
@TieredCache(strategy = CacheStrategy.LOCAL_FIRST)
public User getUser(Long id) {
    return userRepository.findById(id);
}
```

### 5.2 REMOTE_FIRST

远程优先策略，先查询远程缓存，再查询本地缓存。

```java
@TieredCache(strategy = CacheStrategy.REMOTE_FIRST)
public User getUser(Long id) {
    return userRepository.findById(id);
}
```

### 5.3 LOCAL_ONLY

仅使用本地缓存。

```java
@TieredCache(strategy = CacheStrategy.LOCAL_ONLY)
public User getUser(Long id) {
    return userRepository.findById(id);
}
```

### 5.4 REMOTE_ONLY

仅使用远程缓存。

```java
@TieredCache(strategy = CacheStrategy.REMOTE_ONLY)
public User getUser(Long id) {
    return userRepository.findById(id);
}
```

## 6. 高级用法

### 6.1 SpEL 表达式

支持使用 Spring Expression Language (SpEL) 编写复杂的键表达式和条件。

```java
@Service
public class OrderService {
    
    @TieredCache(
        key = "#user.id + '_' + #status",
        condition = "#user != null and #status != null"
    )
    public List<Order> getOrders(User user, String status) {
        return orderRepository.findByUserAndStatus(user, status);
    }
}
```

### 6.2 自定义键生成

可以通过实现自定义键生成逻辑来满足特殊需求。

```java
@TieredCache(keyGenerator = "customKeyGenerator")
public ComplexObject getComplexData(Criteria criteria) {
    return dataService.getComplexData(criteria);
}
```

### 6.3 异常处理

所有缓存相关的异常都会被捕获，不会影响主业务流程。

```java
@Service
public class SafeService {
    
    @TieredCache(key = "#id")
    public User getUser(Long id) {
        // 即使缓存服务不可用，此方法仍会正常执行
        return userRepository.findById(id);
    }
}
```

## 7. 监控和运维

### 7.1 指标监控

通过 Micrometer 提供缓存性能指标：

- 缓存命中率
- 请求总数
- 平均响应时间
- 错误率

### 7.2 健康检查

提供 Spring Boot Actuator 健康检查端点：

```
GET /actuator/health/tieredCache
```

### 7.3 日志记录

详细的日志记录帮助诊断问题：

```yaml
logging:
  level:
    com.cache.plugin: DEBUG
```

## 8. 故障排除

### 8.1 常见问题

#### 缓存未生效

检查以下几点：
1. 确保在 `@Service` 或 `@Component` 注解的类中使用缓存注解
2. 确保在 Spring 管理的 bean 之间进行方法调用
3. 检查配置是否正确启用缓存

#### 远程缓存连接失败

1. 检查 Redis 服务是否正常运行
2. 验证配置中的主机地址和端口是否正确
3. 检查网络连接和防火墙设置

#### 性能问题

1. 检查缓存键是否过于复杂
2. 调整本地缓存大小和过期策略
3. 监控远程缓存的性能指标

### 8.2 日志分析

启用 DEBUG 级别日志可以帮助诊断问题：

```yaml
logging:
  level:
    com.cache.plugin: DEBUG
```

## 9. 最佳实践

### 9.1 缓存键设计

1. 使用简单且唯一的键
2. 避免使用过于复杂的 SpEL 表达式
3. 考虑键的长度和可读性

### 9.2 过期策略

1. 根据数据变化频率设置合适的过期时间
2. 对于静态数据可以设置较长的过期时间
3. 对于频繁变化的数据设置较短的过期时间

### 9.3 容量规划

1. 根据应用内存情况合理设置本地缓存大小
2. 监控缓存命中率，调整缓存策略
3. 考虑使用分布式缓存应对大数据量场景

### 9.4 安全考虑

1. 对敏感数据启用加密功能
2. 配置适当的访问控制策略
3. 定期轮换加密密钥

## 10. 版本升级指南

### 10.1 从旧版本升级

在升级到新版本时，请注意以下变化：

1. 检查配置属性是否有变化
2. 验证注解用法是否兼容
3. 更新依赖版本

### 10.2 兼容性说明

TieredCache 保证主版本号不变的情况下向后兼容。