# API 文档

## 注解API

### @TieredCache

分层缓存注解，支持本地缓存和远程缓存的组合使用。

```java
@TieredCache(
    local = @LocalCache(maxSize = 1000, expireAfterWrite = 300),
    remote = @RemoteCache(ttl = 3600),
    key = "'user:' + #userId",
    strategy = CacheStrategy.LOCAL_FIRST,
    condition = "#userId != null && #userId > 0"
)
public User getUserById(Long userId) {
    // 方法实现
}
```

**参数说明:**
- `local`: 本地缓存配置
- `remote`: 远程缓存配置  
- `key`: 缓存键表达式（支持SpEL）
- `strategy`: 缓存策略
- `condition`: 缓存条件（支持SpEL）
- `mode`: 缓存模式（READ_ONLY, WRITE_ONLY, READ_WRITE）

### @LocalCache

本地缓存注解，仅使用本地缓存。

```java
@LocalCache(
    key = "'profile:' + #userId",
    maxSize = 500,
    expireAfterWrite = 600,
    condition = "#userId != null"
)
public UserProfile getUserProfile(Long userId) {
    // 方法实现
}
```

**参数说明:**
- `key`: 缓存键表达式
- `maxSize`: 最大缓存大小
- `expireAfterWrite`: 写入后过期时间（秒）
- `expireAfterAccess`: 访问后过期时间（秒）
- `condition`: 缓存条件

### @RemoteCache

远程缓存注解，仅使用远程缓存。

```java
@RemoteCache(
    key = "'settings:' + #userId",
    ttl = 7200,
    namespace = "user-settings"
)
public UserSettings getUserSettings(Long userId) {
    // 方法实现
}
```

**参数说明:**
- `key`: 缓存键表达式
- `ttl`: 过期时间（秒）
- `namespace`: 命名空间
- `sync`: 是否同步操作

### @CacheEvict

缓存清除注解。

```java
@CacheEvict(
    key = "'user:' + #user.id",
    condition = "#user != null",
    beforeInvocation = false
)
public User updateUser(User user) {
    // 方法实现
}
```

**参数说明:**
- `key`: 要清除的缓存键
- `allEntries`: 是否清除所有条目
- `beforeInvocation`: 是否在方法执行前清除
- `level`: 缓存层级（LOCAL, REMOTE, ALL）

### @CachePut

缓存更新注解。

```java
@CachePut(
    key = "'user:' + #result.id",
    level = CacheLevel.ALL,
    condition = "#result != null"
)
public User createUser(String name, Integer age) {
    // 方法实现
}
```

## 缓存策略

### CacheStrategy 枚举

- `LOCAL_FIRST`: 本地优先策略
- `REMOTE_FIRST`: 远程优先策略  
- `LOCAL_ONLY`: 仅本地缓存
- `REMOTE_ONLY`: 仅远程缓存
- `WRITE_THROUGH`: 写穿透模式
- `WRITE_BEHIND`: 写回模式

## SpEL表达式支持

### 可用变量

- `#p0, #p1, ...`: 方法参数（按位置）
- `#a0, #a1, ...`: 方法参数（按位置）
- `#参数名`: 方法参数（按名称）
- `#result`: 方法返回值（仅在@CachePut中可用）
- `#method`: 方法对象
- `#methodName`: 方法名称
- `#className`: 类名

### 可用函数

- `#isEmpty(obj)`: 检查对象是否为空
- `#isNotEmpty(obj)`: 检查对象是否不为空
- `#isNull(obj)`: 检查对象是否为null
- `#isNotNull(obj)`: 检查对象是否不为null

### 表达式示例

```java
// 简单键
key = "'user:' + #userId"

// 复合键
key = "#className + '.' + #methodName + ':' + #userId"

// 条件表达式
condition = "#userId != null && #userId > 0"

// 复杂条件
condition = "#isNotNull(#user) && #user.age >= 18"

// 结果相关
key = "'result:' + #result.id"  // 在@CachePut中使用
```

## 配置API

### 完整配置示例

```yaml
two-level-cache:
  enabled: true
  
  # 本地缓存配置
  local:
    provider: caffeine
    max-size: 10000
    expire-after-write: 300s
    expire-after-access: 600s
    initial-capacity: 100
    record-stats: true
  
  # 远程缓存配置  
  remote:
    provider: redis
    ttl: 3600s
    cluster-nodes: localhost:6379
    password: ${REDIS_PASSWORD:}
    database: 0
    timeout: 5s
    pool:
      max-active: 8
      max-idle: 8
      min-idle: 0
  
  # 策略配置
  strategy:
    default-strategy: local-first
    write-mode: write-through
    read-mode: read-through
    consistency: eventual
  
  # 同步配置
  sync:
    enabled: true
    channel: cache-sync
    batch-size: 100
    flush-interval: 1s
  
  # 安全配置
  security:
    encryption:
      enabled: false
      algorithm: AES-256-GCM
      key: ${CACHE_ENCRYPTION_KEY:}
    access-control:
      enabled: false
      default-policy: deny
      rules:
        "user:*": "allow:USER,ADMIN:read,write"
        "admin:*": "allow:ADMIN:read,write"
  
  # 监控配置
  monitoring:
    metrics:
      enabled: true
      export-interval: 30s
    health-check:
      enabled: true
      interval: 10s
```

## 编程API

### TwoLevelCacheManager

```java
@Autowired
private TwoLevelCacheManager cacheManager;

// 获取缓存值
Object value = cacheManager.get("key", String.class, CacheStrategy.LOCAL_FIRST);

// 存储缓存值
cacheManager.put("key", "value", CacheStrategy.WRITE_THROUGH, Duration.ofHours(1));

// 删除缓存
cacheManager.evict("key", CacheStrategy.LOCAL_FIRST);

// 清空缓存
cacheManager.clear();

// 批量操作
Map<String, Object> values = cacheManager.multiGet(keys, CacheStrategy.LOCAL_FIRST);
cacheManager.multiPut(keyValues, CacheStrategy.WRITE_THROUGH, ttl);
```

### 缓存指标

```java
@Autowired
private CacheMetrics metrics;

// 获取统计摘要
CacheMetrics.CacheMetricsSummary summary = metrics.getSummary();
System.out.println("命中率: " + summary.getHitRate());
System.out.println("本地命中数: " + summary.getLocalHits());
System.out.println("远程命中数: " + summary.getRemoteHits());
```

## 最佳实践

### 1. 缓存键设计

```java
// 好的做法：使用有意义的前缀和层次结构
key = "'user:profile:' + #userId"
key = "'product:detail:' + #productId + ':' + #version"

// 避免：过于简单或可能冲突的键
key = "#userId"  // 太简单
key = "'data:' + #id"  // 可能冲突
```

### 2. 缓存策略选择

```java
// 频繁访问的数据：本地优先
@TwoLevelCache(strategy = CacheStrategy.LOCAL_FIRST)

// 大型数据或共享数据：远程优先  
@TwoLevelCache(strategy = CacheStrategy.REMOTE_FIRST)

// 临时数据：仅本地
@LocalCache(expireAfterWrite = 60)

// 持久化数据：仅远程
@RemoteCache(ttl = 86400)
```

### 3. 条件缓存

```java
// 只缓存有效数据
condition = "#result != null && #result.isValid()"

// 根据参数决定是否缓存
condition = "#userId > 0 && #includeCache == true"

// 避免缓存大对象
condition = "#result != null && #result.size() < 1000"
```