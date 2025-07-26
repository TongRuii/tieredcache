# TieredCache 配置示例

本文档提供了多种场景下的配置示例，帮助用户快速正确地配置和使用TieredCache。

## 1. 仅本地缓存配置

当您只需要本地缓存，不需要远程缓存时，可以使用以下配置：

```yaml
tiered-cache:
  enabled: true
  local:
    provider: caffeine
    max-size: 10000
    expire-after-write: 300s  # 5分钟
    expire-after-access: 600s  # 10分钟
    initial-capacity: 100
    record-stats: true
  remote:
    enabled: false  # 禁用远程缓存
  sync:
    enabled: false  # 禁用同步功能
  monitoring:
    metrics:
      enabled: true
```

这种配置适用于：
- 单实例应用
- 不需要跨实例缓存共享的场景
- 没有Redis等远程缓存服务的环境

## 2. 生产环境配置

生产环境推荐使用以下配置：

```yaml
tiered-cache:
  enabled: true
  local:
    provider: caffeine
    max-size: 50000
    expire-after-write: 1800s  # 30分钟
    expire-after-access: 3600s  # 1小时
    initial-capacity: 1000
    record-stats: true
  remote:
    enabled: true
    provider: redis
    ttl: 7200s  # 2小时
    cluster-nodes: redis-host1:6379,redis-host2:6379,redis-host3:6379
    password: ${REDIS_PASSWORD}
    database: 0
    timeout: 5s
    pool:
      max-active: 20
      max-idle: 10
      min-idle: 2
      max-wait: 2s
  strategy:
    default-strategy: local-first
    write-mode: write-through
    read-mode: read-through
    consistency: eventual
  sync:
    enabled: true
    channel: cache-sync
    batch-size: 50
    flush-interval: 2s
  security:
    encryption:
      enabled: true
      algorithm: AES-256-GCM
      key: ${CACHE_ENCRYPTION_KEY}
    access-control:
      enabled: true
      default-policy: deny
      rules:
        "user:*": "allow:USER,ADMIN:read,write"
        "admin:*": "allow:ADMIN:read,write"
  monitoring:
    metrics:
      enabled: true
      export-interval: 30s
    health-check:
      enabled: true
      interval: 10s
```

## 3. 开发环境配置

开发环境可以使用简化的配置：

```yaml
tiered-cache:
  enabled: true
  local:
    provider: caffeine
    max-size: 1000
    expire-after-write: 300s  # 5分钟
    expire-after-access: 600s  # 10分钟
  remote:
    enabled: true
    provider: redis
    ttl: 3600s  # 1小时
    cluster-nodes: localhost:6379
  sync:
    enabled: false  # 开发环境通常不需要同步
  monitoring:
    metrics:
      enabled: true
```

## 4. 无Redis依赖配置

如果您想在没有Redis依赖的环境中使用TieredCache，可以使用以下配置：

```yaml
tiered-cache:
  enabled: true
  local:
    provider: caffeine
    max-size: 5000
    expire-after-write: 600s  # 10分钟
  remote:
    enabled: false  # 完全禁用远程缓存
  sync:
    enabled: false  # 禁用同步功能
  monitoring:
    metrics:
      enabled: true
```

## 5. 高性能读取配置

如果您需要优化读取性能，可以使用以下配置：

```yaml
tiered-cache:
  enabled: true
  local:
    provider: caffeine
    max-size: 100000
    expire-after-write: 1800s  # 30分钟
    initial-capacity: 5000
    record-stats: true
  remote:
    enabled: true
    provider: redis
    ttl: 10800s  # 3小时
    cluster-nodes: redis-host:6379
    timeout: 2s
    pool:
      max-active: 50
      max-idle: 20
      min-idle: 5
  strategy:
    default-strategy: local-first  # 本地优先策略
  sync:
    enabled: true
    channel: cache-sync
    batch-size: 100
    flush-interval: 1s
```

## 6. 强一致性配置

如果您需要强一致性，可以使用以下配置：

```yaml
tiered-cache:
  enabled: true
  local:
    provider: caffeine
    max-size: 10000
    expire-after-write: 600s  # 10分钟
  remote:
    enabled: true
    provider: redis
    ttl: 3600s  # 1小时
    cluster-nodes: redis-host:6379
  strategy:
    default-strategy: remote-first  # 远程优先策略
    write-mode: write-through  # 直写模式
  sync:
    enabled: true
    channel: cache-sync
```

## 配置说明

### 核心配置项

- `tiered-cache.enabled`: 是否启用分层缓存
- `tiered-cache.local`: 本地缓存配置
- `tiered-cache.remote`: 远程缓存配置
- `tiered-cache.strategy`: 缓存策略配置
- `tiered-cache.sync`: 缓存同步配置
- `tiered-cache.security`: 安全配置
- `tiered-cache.monitoring`: 监控配置

### 本地缓存配置

- `provider`: 本地缓存提供者（目前只支持caffeine）
- `max-size`: 最大缓存条目数
- `expire-after-write`: 写入后过期时间
- `expire-after-access`: 访问后过期时间
- `initial-capacity`: 初始容量
- `record-stats`: 是否记录统计信息

### 远程缓存配置

- `enabled`: 是否启用远程缓存
- `provider`: 远程缓存提供者（目前只支持redis）
- `ttl`: 默认过期时间
- `cluster-nodes`: Redis集群节点
- `password`: Redis密码
- `database`: Redis数据库索引
- `timeout`: 连接超时时间
- `pool`: 连接池配置

### 同步配置

- `enabled`: 是否启用缓存同步
- `channel`: 同步频道名称
- `batch-size`: 批量同步大小
- `flush-interval`: 刷新间隔

通过合理配置这些参数，您可以根据具体的应用场景和性能要求来优化TieredCache的行为。