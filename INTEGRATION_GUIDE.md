
# TieredCache 接入指南

## 1. 简介

TieredCache 是一个为 Spring Boot 应用设计的声明式、可插拔的分层缓存库。它通过整合本地缓存（一级缓存，L1）和远程缓存（二级缓存，L2），旨在提供高性能、高可用的缓存解决方案，同时显著降低对后端数据源的访问压力。

### 核心特性

- **分层缓存**：默认集成 [Caffeine](https://github.com/ben-manes/caffeine) 作为本地缓存和 [Redis](https://redis.io/) 作为远程缓存。
- **声明式接入**：通过注解（如 `@TieredCache`, `@CacheEvict`）即可为方法添加缓存逻辑，对业务代码无侵入。
- **灵活的缓存策略**：支持本地优先、远程优先、仅本地、仅远程、直写、后写等多种策略。
- **强大的配置能力**：所有缓存行为均可通过 `application.yml` 进行详细配置。
- **多实例同步**：通过 Redis Pub/Sub 机制，自动处理多服务实例间的本地缓存同步问题。
- **监控与健康检查**：与 Spring Boot Actuator 集成，提供详细的缓存指标和健康状态检查。
- **安全性**：提供可选的数据加密和访问控制功能。

## 2. 快速入门

本章节将引导您快速地将 TieredCache 集成到您的 Spring Boot 项目中。

### 2.1. 添加依赖

首先，在您的 `pom.xml` 文件中添加 TieredCache 的 Maven 依赖。

```xml
<dependency>
    <groupId>com.cache.plugin</groupId>
    <artifactId>tiered-cache</artifactId>
    <version>1.0.0</version> <!-- 请替换为最新版本 -->
</dependency>
```

### 2.2. 添加配置

在您的 `application.yml` 文件中添加最基础的配置。您需要一个正在运行的 Redis 实例。

```yaml
# Spring Boot Redis 配置
spring:
  redis:
    host: localhost
    port: 6379
    # password: your-redis-password

# TieredCache 基础配置
tiered-cache:
  enabled: true # 启用分层缓存
  remote:
    enabled: true # 启用远程缓存
  local:
    # 本地缓存使用默认配置
  sync:
    enabled: true # 启用多实例同步
```

### 2.3. 使用注解

现在，您可以在任何 Spring Bean 的方法上使用 `@TieredCache` 注解。

```java
import com.cache.plugin.annotation.TieredCache;
import org.springframework.stereotype.Service;

@Service
public class ProductService {

    @TieredCache(
        key = "'product:' + #productId", // SpEL 表达式生成缓存键
        local = @LocalCache(expireAfterWrite = 300), // 本地缓存有效期 300s
        remote = @RemoteCache(ttl = 3600) // 远程缓存有效期 3600s
    )
    public Product getProductById(String productId) {
        // 模拟从数据库获取数据
        System.out.println("正在从数据库查询产品: " + productId);
        return new Product(productId, "My Awesome Product");
    }
}
```

**工作原理**：
- 当 `getProductById` 方法第一次被调用时，它会执行方法体（查询数据库），然后将返回值同时存入本地缓存和 Redis 缓存。
- 后续的调用将直接从本地缓存（Caffeine）获取数据，响应时间极快。
- 如果本地缓存过期或被清除，它会尝试从远程缓存（Redis）获取。如果命中，数据会返回并被重新加载到本地缓存。
- 只有当两级缓存都未命中时，才会再次执行方法体。

## 3. 详细配置

TieredCache 提供了丰富的配置项，允许您精细化地控制缓存行为。

### 3.1. 全局配置

```yaml
tiered-cache:
  enabled: true # 全局开关，默认为 true
```

### 3.2. 本地缓存 (L1)

基于 Caffeine 实现。

```yaml
tiered-cache:
  local:
    provider: "caffeine" # 目前仅支持 caffeine
    initial-capacity: 100 # 初始容量
    max-size: 10000 # 最大条目数
    expire-after-write: 5m # 写入后 5 分钟过期
    expire-after-access: 10m # 访问后 10 分钟过期
    record-stats: true # 是否记录统计信息，用于监控
```

### 3.3. 远程缓存 (L2)

基于 Redis 实现。

```yaml
tiered-cache:
  remote:
    enabled: true # 是否启用远程缓存
    provider: "redis" # 目前仅支持 redis
    ttl: 1h # 默认的全局 TTL (Time-To-Live)
    # Redis 连接信息通常由 spring.redis.* 配置提供
    # 但也可以在这里覆盖
    cluster-nodes: "localhost:6379"
    password:
    database: 0
    timeout: 5s
    pool:
      max-active: 8
      max-idle: 8
      min-idle: 0
      max-wait: -1ms
```

### 3.4. 缓存同步

当您部署多个服务实例时，缓存同步至关重要。它能确保当一个实例更新或清除了某个缓存后，其他实例的本地缓存也能被同步清除，避免数据不一致。

```yaml
tiered-cache:
  sync:
    enabled: true # 启用同步机制
    channel: "cache-sync" # Redis Pub/Sub 使用的频道名称
    batch-size: 100 # 批量处理同步消息的大小
    flush-interval: 1s # 定期同步任务的间隔
```

### 3.5. 缓存策略

```yaml
tiered-cache:
  strategy:
    default-strategy: "local-first" # 默认的读策略
    write-mode: "write-through" # 默认的写策略
    read-mode: "read-through" # 默认的读模式
    consistency: "eventual" # 一致性模型
```

- **读策略 (`default-strategy`)**:
  - `local-first`: 优先读本地，本地没有再读远程。
  - `remote-first`: 优先读远程，远程没有再读本地。
  - `local-only`: 只读本地。
  - `remote-only`: 只读远程。
- **写策略 (`write-mode`)**:
  - `write-through` (直写): 同步写入本地和远程缓存。
  - `write-behind` (后写): 先写入本地，然后异步写入远程。

## 4. 使用指南：注解详解

### 4.1. `@TieredCache`

最核心的注解，用于为一个方法启动分层缓存。

```java
@TieredCache(
    // 缓存键，支持 SpEL。#result 可以引用方法返回值
    key = "'user:' + #userId", 
    
    // 缓存条件，支持 SpEL。只有当表达式为 true 时才缓存
    condition = "#userId > 0", 
    
    // 缓存策略
    strategy = CacheStrategy.LOCAL_FIRST, 
    
    // 缓存读写模式
    mode = CacheMode.READ_WRITE, 
    
    // 单独配置 L1 缓存
    local = @LocalCache(expireAfterWrite = 600), 
    
    // 单独配置 L2 缓存
    remote = @RemoteCache(ttl = 7200) 
)
public User getUser(long userId) { ... }
```

### 4.2. `@LocalCache` 和 `@RemoteCache`

如果只想使用单层缓存，可以直接使用这两个注解。

```java
// 仅使用本地缓存
@LocalCache(key = "'profile:' + #userId", expireAfterAccess = 1800)
public UserProfile getProfile(long userId) { ... }

// 仅使用远程缓存
@RemoteCache(key = "'settings:' + #userId", ttl = 86400)
public UserSettings getSettings(long userId) { ... }
```

### 4.3. `@CacheEvict`

用于从缓存中移除数据，通常用在更新或删除操作的方法上。

```java
@CacheEvict(
    // 要清除的缓存键，必须和 @TieredCache 中定义的 key 匹配
    key = "'user:' + #user.id", 
    
    // 缓存层级，指定要从哪一层清除
    level = CacheLevel.ALL, // ALL, LOCAL, or REMOTE
    
    // 是否清除所有条目，设为 true 则忽略 key
    allEntries = false, 
    
    // true: 方法执行前清除; false: 方法成功执行后清除
    beforeInvocation = false 
)
public void updateUser(User user) { ... }
```

### 4.4. `@CachePut`

确保方法总是被执行，并且其结果被放入缓存。适用于希望更新缓存内容但又不影响方法正常调用的场景。

```java
@CachePut(
    key = "'user:' + #result.id", // key 通常依赖于返回值
    level = CacheLevel.ALL
)
public User createUser(String name) {
    // ... 创建用户的逻辑 ...
    return newUser; // 返回值 newUser 将被缓存
}
```

## 5. 高级主题

### 5.1. 动态缓存键 (SpEL)

所有缓存注解的 `key` 和 `condition` 属性都支持 [Spring Expression Language (SpEL)](https://docs.spring.io/spring-framework/docs/current/reference/html/core.html#expressions)。

- **引用参数**:
  - 按索引: `#a0`, `#p0`
  - 按名称: `#paramName` (需要 Java 8+ 的 `-parameters` 编译参数)
- **引用返回值**: `#result` (仅用于 `@CachePut` 和部分场景的 `@CacheEvict`)
- **调用方法**: `#user.getId()`
- **访问 Bean**: `@beanName.method()`

### 5.2. 监控与健康检查

如果您的项目中包含了 `spring-boot-starter-actuator`，TieredCache 会自动集成。

- **健康检查**: 访问 `/actuator/health` 查看缓存健康状态。
  ```json
  "cache": {
    "status": "UP",
    "details": {
      "local-cache": "UP",
      "remote-cache": "UP",
      ...
    }
  }
  ```

- **缓存指标**: 访问 `/actuator/metrics` 查看详细指标。
  - `cache.local.hit`, `cache.remote.hit`: 命中次数
  - `cache.miss`: 未命中次数
  - `cache.put`: 存放次数
  - `cache.hit.rate`: 命中率
  - ...等等

### 5.3. 安全

- **数据加密**:
  ```yaml
  tiered-cache:
    security:
      encryption:
        enabled: true
        algorithm: "AES-256-GCM"
        key: "your-base64-encoded-secret-key" # 推荐从配置中心或环境变量读取
  ```
  启用后，存入远程缓存的数据会被自动加密。

- **访问控制**:
  ```yaml
  tiered-cache:
    security:
      access-control:
        enabled: true
        default-policy: "deny" # 默认拒绝
        rules:
          "user:*": "role:ADMIN:read,write" # 拥有 ADMIN 角色的用户可读写 user:* 的缓存
          "product:*": "allow" # 允许所有用户访问 product:* 的缓存
  ```
  此功能依赖 Spring Security 的 `Authentication` 上下文。

## 6. 示例项目

本项目自带一个完整的演示模块 `com.cache.plugin.example.demo`，其中包含了各种缓存使用场景的示例代码，是学习和理解本库用法的最佳参考。
