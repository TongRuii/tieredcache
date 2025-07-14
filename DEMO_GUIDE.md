# TieredCache 演示指南

这个演示应用展示了TieredCache分层缓存插件的各种使用场景和功能特性。

## 🚀 快速启动

### 方式一：命令行演示

```bash
# 编译项目
mvn clean compile

# 运行命令行演示
mvn exec:java -Dexec.mainClass="com.cache.plugin.example.demo.DemoApplication" -Dspring.profiles.active=demo
```

### 方式二：Web界面演示

```bash
# 启动Web应用
mvn spring-boot:run -Dspring.profiles.active=demo

# 访问演示页面
curl http://localhost:8080/api/demo/
```

## 📋 演示功能

### 1. 用户信息缓存 (分层缓存)

```bash
# 第一次调用 - 从数据库获取
curl http://localhost:8080/api/demo/user/1001

# 第二次调用 - 从本地缓存获取 (速度更快)
curl http://localhost:8080/api/demo/user/1001
```

**特点:**
- 使用 `LOCAL_FIRST` 策略
- 本地缓存300秒，远程缓存3600秒
- 第一次调用较慢，后续调用从缓存获取

### 2. 用户详情 (仅本地缓存)

```bash
# 获取用户详情
curl http://localhost:8080/api/demo/user/1001/profile
```

**特点:**
- 仅使用本地缓存
- 适合频繁访问的数据
- 缓存失效后需重新加载

### 3. 产品信息 (远程优先)

```bash
# 获取产品信息
curl http://localhost:8080/api/demo/product/PROD-001
```

**特点:**
- 使用 `REMOTE_FIRST` 策略
- 适合分布式环境下的数据一致性
- 远程缓存可在多个实例间共享

### 4. 热门产品 (长时间缓存)

```bash
# 获取热门产品列表
curl http://localhost:8080/api/demo/products/hot
```

**特点:**
- 长时间缓存 (3600秒)
- 适合变化较少的数据
- 减少数据库压力

### 5. 订单管理 (缓存清除)

```bash
# 获取订单信息
curl http://localhost:8080/api/demo/order/2001

# 更新订单状态 (会清除缓存)
curl -X PUT "http://localhost:8080/api/demo/order/2001/status?status=SHIPPED"

# 再次获取订单 (从数据库获取新状态)
curl http://localhost:8080/api/demo/order/2001
```

**特点:**
- 演示 `@CacheEvict` 注解的使用
- 数据更新时自动清除相关缓存
- 保证数据一致性

## ⚡ 性能测试

### 缓存性能对比

```bash
# 运行性能对比测试
curl http://localhost:8080/api/demo/performance
```

**测试内容:**
- 50次无缓存调用 vs 50次有缓存调用
- 展示缓存带来的性能提升
- 平均响应时间对比

### 缓存策略对比

```bash
# 对比不同缓存策略
curl http://localhost:8080/api/demo/strategies
```

**对比策略:**
- `LOCAL_FIRST`: 本地优先
- `REMOTE_FIRST`: 远程优先  
- `LOCAL_ONLY`: 仅本地
- `REMOTE_ONLY`: 仅远程

## 📊 监控和指标

### 健康检查

```bash
# 检查应用健康状态
curl http://localhost:8080/actuator/health
```

### 缓存指标

```bash
# 查看缓存相关指标
curl http://localhost:8080/actuator/metrics

# 查看缓存统计
curl http://localhost:8080/actuator/caches
```

## 🎯 使用场景演示

### 1. 电商场景

- **用户信息**: 分层缓存，平衡性能和一致性
- **产品目录**: 远程缓存，支持分布式部署
- **订单数据**: 缓存清除，保证数据准确性

### 2. 内容管理

- **文章内容**: 长时间缓存，减少数据库负载
- **用户评论**: 短时间缓存，保证实时性
- **热门内容**: 多级缓存，提升访问速度

### 3. 数据分析

- **统计报表**: 按时间维度缓存
- **实时指标**: 短时间本地缓存
- **历史数据**: 长时间远程缓存

## 🔧 配置说明

### 缓存配置

```yaml
tiered-cache:
  enabled: true
  local:
    provider: caffeine
    max-size: 10000
    expire-after-write: 300s
  remote:
    provider: redis
    ttl: 3600s
  strategy:
    default-strategy: local-first
```

### 监控配置

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,metrics,caches
```

## 📝 代码示例

### 基本用法

```java
@TieredCache(
    local = @LocalCache(maxSize = 1000, expireAfterWrite = 300),
    remote = @RemoteCache(ttl = 3600),
    key = "'user:' + #userId",
    strategy = CacheStrategy.LOCAL_FIRST
)
public User getUserById(Long userId) {
    return userRepository.findById(userId);
}
```

### 缓存清除

```java
@CacheEvict(key = "'user:' + #userId")
public void updateUser(Long userId, User user) {
    userRepository.save(user);
}
```

### 条件缓存

```java
@TieredCache(
    key = "'users:age:' + #minAge + '-' + #maxAge",
    condition = "#maxAge - #minAge <= 20"
)
public List<User> getUsersByAgeRange(int minAge, int maxAge) {
    return userRepository.findByAgeBetween(minAge, maxAge);
}
```

## 🎓 学习要点

1. **缓存策略选择**: 根据业务场景选择合适的缓存策略
2. **过期时间设置**: 平衡数据一致性和性能
3. **缓存键设计**: 使用SpEL表达式生成唯一键
4. **缓存清除**: 及时清除过期或无效的缓存
5. **监控指标**: 关注缓存命中率和性能指标

## 🔍 故障排查

### 常见问题

1. **缓存未生效**: 检查注解配置和方法调用方式
2. **性能无提升**: 确认缓存策略和过期时间设置
3. **数据不一致**: 检查缓存清除逻辑
4. **内存占用高**: 调整本地缓存大小限制

### 调试技巧

1. 开启DEBUG日志查看缓存操作
2. 使用监控端点检查缓存状态
3. 通过性能测试验证缓存效果
4. 分析缓存命中率和响应时间

## 📚 扩展阅读

- [TieredCache 用户指南](README.md)
- [API 文档](API.md)
- [配置参考](CONFIGURATION.md)
- [最佳实践](BEST_PRACTICES.md)