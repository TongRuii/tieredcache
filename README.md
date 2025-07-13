# TieredCache Spring Boot Starter

一个安全、高效、灵活的分层缓存插件，适配SpringBoot应用。

## 特性

- **分层缓存架构**: 本地缓存(Local) + 远程缓存(Remote)
- **多种缓存策略**: 本地优先、远程优先、仅本地、仅远程等
- **注解驱动**: 简单易用的注解配置
- **安全性**: 数据加密、访问控制、缓存穿透防护
- **高性能**: 异步处理、批量操作、连接池
- **监控运维**: 指标统计、健康检查、性能监控
- **灵活配置**: 支持多种缓存提供商，可插拔架构

## 快速开始

### 1. 添加依赖

```xml
<dependency>
    <groupId>com.cache.plugin</groupId>
    <artifactId>tieredcache-spring-boot-starter</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### 2. 配置文件

```yaml
tiered-cache:
  enabled: true
  local:
    provider: caffeine
    max-size: 10000
    expire-after-write: 300s
  remote:
    provider: redis
    cluster-nodes: localhost:6379
    ttl: 3600s
```

### 3. 使用注解

```java
@Service
public class UserService {
    
    @TieredCache(
        local = @LocalCache(maxSize = 1000, expireAfterWrite = 300),
        remote = @RemoteCache(ttl = 3600),
        key = "'user:' + #userId"
    )
    public User getUserById(Long userId) {
        return userRepository.findById(userId);
    }
    
    @CacheEvict(key = "'user:' + #user.id")
    public User updateUser(User user) {
        return userRepository.save(user);
    }
}
```

## 架构设计

```
┌─────────────────────────────────────────────────────────────┐
│                    Application Layer                        │
├─────────────────────────────────────────────────────────────┤
│                Cache Annotation Layer                       │
│  @LocalCache, @RemoteCache, @CacheEvict, @CachePut        │
├─────────────────────────────────────────────────────────────┤
│                Cache Manager Layer                          │
│  TwoLevelCacheManager, CacheConfiguration                  │
├─────────────────────────────────────────────────────────────┤
│                Cache Strategy Layer                         │
│  LocalCache ←→ RemoteCache ←→ DataSource                   │
├─────────────────────────────────────────────────────────────┤
│                Infrastructure Layer                         │
│  Caffeine/Guava    Redis/Hazelcast    Database            │
└─────────────────────────────────────────────────────────────┘
```

## 支持的缓存提供商

### 本地缓存
- Caffeine (推荐)
- Guava Cache
- EhCache

### 远程缓存
- Redis (推荐)
- Hazelcast
- Apache Ignite

## 缓存策略

- **LOCAL_FIRST**: 本地优先，先查本地缓存，未命中再查远程缓存
- **REMOTE_FIRST**: 远程优先，先查远程缓存，未命中再查本地缓存
- **LOCAL_ONLY**: 仅使用本地缓存
- **REMOTE_ONLY**: 仅使用远程缓存
- **WRITE_THROUGH**: 写穿透，同时写入本地和远程缓存
- **WRITE_BEHIND**: 写回，先写本地缓存，异步写入远程缓存

## 安全特性

- **数据加密**: 支持AES-256-GCM加密算法
- **访问控制**: 基于角色的访问控制
- **缓存穿透防护**: 布隆过滤器 + 限流

## 监控运维

- **指标统计**: 命中率、响应时间、错误率等
- **健康检查**: 缓存连接状态检查
- **性能监控**: 集成Micrometer指标

## 开发进度

- [x] 基础框架搭建
- [x] 核心缓存管理器实现
- [x] 本地缓存实现 (Caffeine)
- [x] 远程缓存实现 (Redis)
- [x] 注解处理器和AOP切面
- [x] 缓存同步机制
- [x] 安全模块
- [x] 监控模块
- [x] 测试用例
- [x] 文档完善

## 项目统计

- **总代码文件**: 47个Java类
- **核心模块**: 8个主要模块
- **测试覆盖**: 单元测试 + 集成测试 + 示例代码
- **配置支持**: 完整的SpringBoot自动配置
- **文档**: README + CHANGELOG + API文档

## 快速体验

### 1. 克隆项目
```bash
git clone <repository-url>
cd tieredcache-spring-boot-starter
```

### 2. 运行示例
```bash
mvn clean compile
mvn exec:java -Dexec.mainClass="com.cache.plugin.example.CacheUsageExample"
```

### 3. 运行测试
```bash
mvn test
```

## 贡献

欢迎提交Issue和Pull Request来帮助改进这个项目。

## 许可证

MIT License