# 🚀 TieredCache Spring Boot Starter

<div align="center">

![Java](https://img.shields.io/badge/Java-8+-orange.svg)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.7+-green.svg)
![License](https://img.shields.io/badge/License-MIT-blue.svg)
![Build Status](https://img.shields.io/badge/Build-Passing-brightgreen.svg)
![Coverage](https://img.shields.io/badge/Coverage-80%25+-brightgreen.svg)

**A secure, efficient, and flexible tiered cache plugin for Spring Boot applications**

[中文](README.md) | [English](README_EN.md)

</div>

---

## ✨ Features

- 🏗️ **Tiered Cache Architecture**: Local Cache (L1) + Remote Cache (L2)
- 🎯 **Multiple Cache Strategies**: Local-first, Remote-first, Local-only, Remote-only
- 📝 **Annotation Driven**: Simple and intuitive annotation configuration
- 🔒 **Security**: Data encryption, access control, cache penetration protection
- ⚡ **High Performance**: Async processing, batch operations, connection pooling
- 📊 **Monitoring**: Metrics, health checks, performance monitoring
- 🔧 **Flexible Configuration**: Multiple cache providers, pluggable architecture

## 🚀 Quick Start

### 📦 Installation

Add the dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>com.cache.plugin</groupId>
    <artifactId>tieredcache-spring-boot-starter</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

<details>
<summary>Gradle Users</summary>

```gradle
implementation 'com.cache.plugin:tieredcache-spring-boot-starter:1.0.0-SNAPSHOT'
```
</details>

### ⚙️ Configuration

Configure in your `application.yml`:

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

### 💡 Usage

Simply add annotations to your service methods:

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

## 🏗️ Architecture

<div align="center">

```mermaid
graph TB
    A[Application Layer] --> B[Cache Annotation Layer]
    B --> C[Cache Manager Layer]
    C --> D[Cache Strategy Layer]
    D --> E[Infrastructure Layer]
    
    B1[@TieredCache<br/>@LocalCache<br/>@RemoteCache<br/>@CacheEvict] --> B
    C1[TieredCacheManager<br/>CacheConfiguration] --> C
    D1[Local Cache] --> D
    D2[Remote Cache] --> D
    D3[Data Source] --> D
    E1[Caffeine] --> E
    E2[Redis] --> E
    E3[Database] --> E
```

</div>

## 🔧 Supported Providers

<table>
<tr>
<td>

### 🏠 Local Cache
- ✅ **Caffeine** (Recommended)
- ✅ Guava Cache
- ✅ EhCache

</td>
<td>

### 🌐 Remote Cache
- ✅ **Redis** (Recommended)
- ✅ Hazelcast
- ✅ Apache Ignite

</td>
</tr>
</table>

## 📋 Cache Strategies

| Strategy | Description | Use Case |
|----------|-------------|----------|
| `LOCAL_FIRST` | Check local cache first, then remote | 🚀 High performance reads |
| `REMOTE_FIRST` | Check remote cache first, then local | 🔄 Multi-instance consistency |
| `LOCAL_ONLY` | Use only local cache | ⚡ Ultra-fast single instance |
| `REMOTE_ONLY` | Use only remote cache | 🌐 Distributed applications |
| `WRITE_THROUGH` | Write to both caches simultaneously | 🔒 Strong consistency |
| `WRITE_BEHIND` | Write to local first, async to remote | 📈 High write performance |

## 🔒 Security Features

- 🔐 **Data Encryption**: AES-256-GCM encryption algorithm
- 👥 **Access Control**: Role-based access control (RBAC)
- 🛡️ **Cache Penetration Protection**: Bloom filter + Rate limiting

## 📊 Monitoring & Operations

- 📈 **Metrics**: Hit rate, response time, error rate
- ❤️ **Health Checks**: Cache connection status monitoring
- 🔍 **Performance Monitoring**: Micrometer integration

## 🚧 Development Status

<div align="center">

![Progress](https://progress-bar.dev/100/?title=Overall%20Progress&width=400&color=babaca)

</div>

- ✅ **Framework Setup** - Core infrastructure and dependencies
- ✅ **Cache Manager** - TieredCacheManager implementation
- ✅ **Local Cache** - Caffeine integration
- ✅ **Remote Cache** - Redis integration  
- ✅ **AOP & Annotations** - Annotation processors and aspects
- ✅ **Cache Synchronization** - Multi-node consistency
- ✅ **Security Module** - Encryption and access control
- ✅ **Monitoring** - Metrics and health checks
- ✅ **Testing** - Unit and integration tests
- ✅ **Documentation** - Complete API docs

## 📊 Project Stats

<div align="center">

| Metric | Value |
|--------|-------|
| 📁 **Java Classes** | 47 |
| 🧩 **Core Modules** | 8 |
| 🧪 **Test Coverage** | 80%+ |
| 📚 **Documentation** | Complete |
| 🏗️ **Architecture** | Modular |

</div>

## 🎯 Getting Started

### Clone and Run

```bash
# Clone the repository
git clone <repository-url>
cd tieredcache-spring-boot-starter

# Run example
mvn clean compile
mvn exec:java -Dexec.mainClass="com.cache.plugin.example.CacheUsageExample"

# Run tests
mvn test
```

## 🤝 Contributing

We welcome contributions! Please see our [Contributing Guide](CONTRIBUTING.md) for details.

- 🐛 **Bug Reports**: [Create an issue](../../issues/new?template=bug_report.md)
- 💡 **Feature Requests**: [Create an issue](../../issues/new?template=feature_request.md)
- 🔀 **Pull Requests**: [Submit a PR](../../pulls)

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

<div align="center">

**⭐ Star this repo if you find it helpful!**

Made with ❤️ by the TieredCache team

</div>