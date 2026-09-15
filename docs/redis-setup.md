# Hướng dẫn setup Redis từ A → Z (Spring Boot + Redisson + Hibernate L2 Cache)

Tài liệu này gồm 2 phần:

- **Phần 1** — Mô tả chính xác project `template` này đang setup Redis như thế nào (để tham chiếu).
- **Phần 2** — Hướng dẫn từng bước dựng lại toàn bộ setup đó cho **một project Spring Boot trắng**, không phụ thuộc template generator.

Stack tham chiếu: Spring Boot `4.1.1`, Java `21`, Redisson `4.7.0`, Redis image `redis:8.10.1`, Maven.

---

# PHẦN 1 — Project này đang setup Redis ra sao

## 1.1. Vai trò của Redis trong project

Redis ở đây **không** dùng như một `RedisTemplate` / data store thông thường. Nó đóng 2 vai trò, cả hai đều đi qua chuẩn **JCache (JSR-107)**:

| Vai trò | Cơ chế | File liên quan |
|---|---|---|
| Spring Cache (`@Cacheable`, `@CacheEvict`…) | `CacheManager` của JCache, backend = Redisson | `config/CacheConfiguration.java` |
| Hibernate Second-Level Cache | `hibernate-jcache` trỏ vào cùng `CacheManager` đó | `CacheConfiguration.java` + `application.yml` |

Chuỗi phụ thuộc:

```
Spring Cache  ─┐
               ├─► javax.cache.CacheManager (JSR-107)
Hibernate L2  ─┘            │
                            ▼
                   Redisson JCache provider
                            │
                            ▼
                      Redis server
```

## 1.2. Các thành phần đang có trong repo

| Đường dẫn | Nội dung |
|---|---|
| `pom.xml` | `org.redisson:redisson:4.7.0`, `org.hibernate.orm:hibernate-jcache` |
| `src/main/java/io/tcbs/template/config/CacheConfiguration.java` | Bean `javax.cache.configuration.Configuration`, `HibernatePropertiesCustomizer`, `JCacheManagerCustomizer` |
| `src/main/resources/config/application.yml` | `hibernate.cache.use_second_level_cache: true`, `use_query_cache: false` |
| `src/main/resources/config/application-dev.yml` | `template.cache.redis.*` (server, expiration, cluster) |
| `src/main/resources/config/application-prod.yml` | tương tự dev |
| `src/main/docker/redis.yml` | Redis single node cho dev |
| `src/main/docker/redis-cluster.yml` + `src/main/docker/redis/` | Redis cluster 6 node + script tự `cluster create` |
| `src/main/docker/app.yml`, `services.yml` | Nhúng redis vào compose stack chung, truyền `JHIPSTER_CACHE_REDIS_SERVER` |
| `src/test/java/.../config/RedisTestContainer.java` | Testcontainers Redis cho integration test |
| `src/main/resources/logback-spring.xml` | `<logger name="org.redisson" level="WARN" />` |

## 1.3. Điểm mấu chốt trong `CacheConfiguration.java`

```java
config.setCodec(new org.redisson.codec.SerializationCodec());
```

Đây là điểm **quan trọng nhất và dễ bị bỏ sót**. Mặc định Redisson dùng codec Kryo/FST — codec này phá vỡ Hibernate lazy proxy khi deserialize, gây lỗi lazy-initialization ngẫu nhiên
(xem [template#22889](https://github.com/template/generator-template/issues/22889)).
`SerializationCodec` = Java serialization ⇒ **mọi entity và mọi field được cache bắt buộc `implements Serializable`**.

Ngoài ra:

- `URI.create(server[0]).getUserInfo()` được parse để lấy password từ URL dạng `redis://user:password@host:6379` — không cần property password riêng.
- Nhánh `cluster: true` dùng `useClusterServers()` + `addNodeAddress(...)` với **toàn bộ** mảng server; nhánh single dùng `useSingleServer()` + `server[0]`.
- `template-needle-redis-add-entry` là marker để generator chèn `createCache(cm, "<EntityName>", jcacheConfiguration)` mỗi khi bạn tạo entity mới. Hiện repo chưa có entity nào nên `cacheManagerCustomizer` còn rỗng và `createCache(...)` chưa được gọi (nên compiler báo unused — đó là bình thường với code generated).

## 1.4. Cấu hình hiện tại

`application-dev.yml` / `application-prod.yml`:

```yaml
template:
  cache:
    redis:
      expiration: 3600          # giây — TTL mặc định cho mọi entry
      server: redis://localhost:6379
      cluster: false
      # server: redis://localhost:6379,redis://localhost:16379,redis://localhost:26379
      # cluster: true
```

Các key `connectionPoolSize`, `connectionMinimumIdleSize`, `subscriptionConnectionPoolSize` không khai báo trong yml → lấy **default của `JHipsterProperties`** (lần lượt `64`, `24`, `50`).

`application.yml`:

```yaml
spring:
  jpa:
    properties:
      hibernate.cache.use_second_level_cache: true
      hibernate.cache.use_query_cache: false
```

## 1.5. Chạy Redis cho project này

```bash
# Chỉ Redis
docker compose -f src/main/docker/redis.yml up -d

# Toàn bộ service phụ trợ (postgres, elasticsearch, keycloak, redis, kafka)
docker compose -f src/main/docker/services.yml up -d

# Redis cluster (6 node, tự động cluster create)
docker compose -f src/main/docker/redis-cluster.yml up -d
```

Lưu ý `redis.yml` bind `127.0.0.1:6379:6379` — chỉ truy cập được từ chính máy dev. Muốn expose ra LAN thì bỏ tiền tố `127.0.0.1:` (và nhớ bật password trước).

---

# PHẦN 2 — Setup từ đầu cho một project Spring Boot trắng

## Bước 0 — Chọn hướng đi

Có 2 hướng, chọn **một**:

| | **Hướng A — Redisson + JCache** (giống project này) | **Hướng B — Spring Data Redis + Lettuce** |
|---|---|---|
| Hibernate L2 cache | ✅ Có | ❌ Không (Hibernate không dùng được `RedisCacheManager`) |
| Spring `@Cacheable` | ✅ | ✅ |
| Distributed lock, rate limiter, queue… | ✅ Redisson cung cấp sẵn | ❌ Phải tự viết |
| `RedisTemplate` / thao tác Redis thủ công | ⚠️ Có nhưng ít tự nhiên | ✅ |
| Độ phức tạp | Cao hơn | Thấp |

👉 **Cần Hibernate L2 cache** → Hướng A. **Chỉ cần cache HTTP/service layer hoặc lưu session/token** → Hướng B (xem [Phụ lục A](#phụ-lục-a--hướng-b-spring-data-redis--lettuce)).

Phần 2 dưới đây đi theo **Hướng A**.

---

## Bước 1 — Khởi động Redis bằng Docker

Tạo `src/main/docker/redis.yml`:

```yaml
name: myapp
services:
  redis:
    image: redis:8.10.1
    # Bỏ tiền tố 127.0.0.1: nếu cần truy cập từ ngoài máy dev
    ports:
      - 127.0.0.1:6379:6379
    healthcheck:
      test: ['CMD', 'redis-cli', 'ping']
      interval: 5s
      timeout: 3s
      retries: 20
```

```bash
docker compose -f src/main/docker/redis.yml up -d
docker compose -f src/main/docker/redis.yml exec redis redis-cli ping   # → PONG
```

> Project mẫu không có `healthcheck` trong `redis.yml`; thêm vào là tốt hơn nếu bạn dùng `depends_on: condition: service_healthy`.

## Bước 2 — Thêm dependency

`pom.xml`:

```xml
<properties>
    <redisson.version>4.7.0</redisson.version>
</properties>

<dependencies>
    <!-- Redisson: JCache provider + Redis client -->
    <dependency>
        <groupId>org.redisson</groupId>
        <artifactId>redisson</artifactId>
        <version>${redisson.version}</version>
    </dependency>

    <!-- Spring Cache abstraction -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-cache</artifactId>
    </dependency>

    <!-- Cầu nối Hibernate ↔ JCache (bỏ qua nếu không dùng JPA) -->
    <dependency>
        <groupId>org.hibernate.orm</groupId>
        <artifactId>hibernate-jcache</artifactId>
    </dependency>
</dependencies>
```

Gradle tương đương:

```groovy
implementation "org.redisson:redisson:4.7.0"
implementation "org.springframework.boot:spring-boot-starter-cache"
implementation "org.hibernate.orm:hibernate-jcache"
```

> `javax.cache:cache-api` (JSR-107) đã đi kèm transitively qua Redisson — **không** khai báo lại để tránh xung đột version.

## Bước 3 — Khai báo properties

`src/main/resources/application.yml`:

```yaml
app:
  cache:
    redis:
      server: redis://localhost:6379   # dạng list nếu cluster
      cluster: false
      expiration: 3600                 # giây
      connection-pool-size: 64
      connection-minimum-idle-size: 24
      subscription-connection-pool-size: 50

spring:
  jpa:
    properties:
      hibernate.cache.use_second_level_cache: true
      hibernate.cache.use_query_cache: false
```

> Dùng prefix riêng (`app.cache.redis`) thay vì `template.cache.redis` vì project trắng không có `template-framework`. Nếu bạn **có** thêm `tech.template:template-framework` thì dùng luôn `JHipsterProperties` và bỏ qua Bước 4.

## Bước 4 — Properties class

`src/main/java/com/example/myapp/config/RedisCacheProperties.java`:

```java
package com.example.myapp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.cache.redis")
public class RedisCacheProperties {

    private String[] server = { "redis://localhost:6379" };
    private boolean cluster = false;
    private int expiration = 3600;
    private int connectionPoolSize = 64;
    private int connectionMinimumIdleSize = 24;
    private int subscriptionConnectionPoolSize = 50;

    public String[] getServer() { return server; }
    public void setServer(String[] server) { this.server = server; }

    public boolean isCluster() { return cluster; }
    public void setCluster(boolean cluster) { this.cluster = cluster; }

    public int getExpiration() { return expiration; }
    public void setExpiration(int expiration) { this.expiration = expiration; }

    public int getConnectionPoolSize() { return connectionPoolSize; }
    public void setConnectionPoolSize(int v) { this.connectionPoolSize = v; }

    public int getConnectionMinimumIdleSize() { return connectionMinimumIdleSize; }
    public void setConnectionMinimumIdleSize(int v) { this.connectionMinimumIdleSize = v; }

    public int getSubscriptionConnectionPoolSize() { return subscriptionConnectionPoolSize; }
    public void setSubscriptionConnectionPoolSize(int v) { this.subscriptionConnectionPoolSize = v; }
}
```

Bật binding trong class `@SpringBootApplication`:

```java
@EnableConfigurationProperties(RedisCacheProperties.class)
```

## Bước 5 — `CacheConfiguration`

`src/main/java/com/example/myapp/config/CacheConfiguration.java`:

```java
package com.example.myapp.config;

import java.net.URI;
import java.util.concurrent.TimeUnit;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.expiry.CreatedExpiryPolicy;
import javax.cache.expiry.Duration;
import org.hibernate.cache.jcache.ConfigSettings;
import org.redisson.Redisson;
import org.redisson.codec.SerializationCodec;
import org.redisson.config.ClusterServersConfig;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;
import org.redisson.jcache.configuration.RedissonConfiguration;
import org.springframework.boot.cache.autoconfigure.JCacheManagerCustomizer;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class CacheConfiguration {

    @Bean
    public javax.cache.configuration.Configuration<Object, Object> jcacheConfiguration(RedisCacheProperties props) {
        MutableConfiguration<Object, Object> jcacheConfig = new MutableConfiguration<>();

        URI redisUri = URI.create(props.getServer()[0]);

        Config config = new Config();
        // BẮT BUỘC: codec mặc định của Redisson phá vỡ Hibernate lazy proxy.
        // https://github.com/template/generator-template/issues/22889
        config.setCodec(new SerializationCodec());

        if (props.isCluster()) {
            ClusterServersConfig cluster = config
                .useClusterServers()
                .setMasterConnectionPoolSize(props.getConnectionPoolSize())
                .setMasterConnectionMinimumIdleSize(props.getConnectionMinimumIdleSize())
                .setSubscriptionConnectionPoolSize(props.getSubscriptionConnectionPoolSize())
                .addNodeAddress(props.getServer());

            if (redisUri.getUserInfo() != null) {
                cluster.setPassword(redisUri.getUserInfo().substring(redisUri.getUserInfo().indexOf(':') + 1));
            }
        } else {
            SingleServerConfig single = config
                .useSingleServer()
                .setConnectionPoolSize(props.getConnectionPoolSize())
                .setConnectionMinimumIdleSize(props.getConnectionMinimumIdleSize())
                .setSubscriptionConnectionPoolSize(props.getSubscriptionConnectionPoolSize())
                .setAddress(props.getServer()[0]);

            if (redisUri.getUserInfo() != null) {
                single.setPassword(redisUri.getUserInfo().substring(redisUri.getUserInfo().indexOf(':') + 1));
            }
        }

        jcacheConfig.setStatisticsEnabled(true);
        jcacheConfig.setExpiryPolicyFactory(
            CreatedExpiryPolicy.factoryOf(new Duration(TimeUnit.SECONDS, props.getExpiration()))
        );

        return RedissonConfiguration.fromInstance(Redisson.create(config), jcacheConfig);
    }

    /** Trỏ Hibernate second-level cache vào chính CacheManager ở trên. */
    @Bean
    public HibernatePropertiesCustomizer hibernatePropertiesCustomizer(javax.cache.CacheManager cm) {
        return hibernateProperties -> hibernateProperties.put(ConfigSettings.CACHE_MANAGER, cm);
    }

    /** Đăng ký sẵn các cache region. Mỗi entity / collection cần 1 dòng createCache. */
    @Bean
    public JCacheManagerCustomizer cacheManagerCustomizer(
        javax.cache.configuration.Configuration<Object, Object> jcacheConfiguration
    ) {
        return cm -> {
            createCache(cm, "com.example.myapp.domain.User", jcacheConfiguration);
            createCache(cm, "com.example.myapp.domain.User.authorities", jcacheConfiguration);
            createCache(cm, "usersByLogin", jcacheConfiguration);   // cache thủ công cho @Cacheable
        };
    }

    private void createCache(
        javax.cache.CacheManager cm,
        String cacheName,
        javax.cache.configuration.Configuration<Object, Object> jcacheConfiguration
    ) {
        javax.cache.Cache<Object, Object> cache = cm.getCache(cacheName);
        if (cache != null) {
            cache.clear();
        } else {
            cm.createCache(cacheName, jcacheConfiguration);
        }
    }
}
```

> ⚠️ **Import path Spring Boot 4**: `JCacheManagerCustomizer` nằm ở `org.springframework.boot.cache.autoconfigure`, `HibernatePropertiesCustomizer` ở `org.springframework.boot.hibernate.autoconfigure`. Trên Spring Boot 3.x cả hai đều ở `org.springframework.boot.autoconfigure.cache` / `...autoconfigure.orm.jpa`.

## Bước 6 — Bật cache trên entity (Hibernate L2)

```java
package com.example.myapp.domain;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

@Entity
@Table(name = "jhi_user")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class User implements Serializable {   // ← BẮT BUỘC Serializable

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    private String login;

    @ManyToMany
    @Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)   // cache riêng cho collection
    private Set<Authority> authorities = new HashSet<>();

    // getters / setters …
}
```

**Quy tắc bắt buộc:**

1. Mỗi `@Cache` trên entity ⇒ 1 dòng `createCache(cm, "<FQCN>", …)`.
2. Mỗi `@Cache` trên collection ⇒ 1 dòng `createCache(cm, "<FQCN>.<fieldName>", …)`.
3. Mọi class đi vào cache (entity, embeddable, enum wrapper…) phải `implements Serializable`. Đây là hệ quả trực tiếp của `SerializationCodec`.

Chọn `CacheConcurrencyStrategy`:

| Strategy | Dùng khi |
|---|---|
| `READ_ONLY` | Dữ liệu không bao giờ đổi sau khi tạo (bảng danh mục) |
| `NONSTRICT_READ_WRITE` | Mặc định an toàn cho hầu hết trường hợp — ghi hiếm, chấp nhận stale rất ngắn |
| `READ_WRITE` | Cần đọc-sau-ghi nhất quán hơn; tốn kém hơn |
| `TRANSACTIONAL` | Chỉ với JTA — Redisson JCache **không** hỗ trợ đầy đủ, tránh dùng |

## Bước 7 — Dùng Spring Cache ở service layer

```java
@Service
public class UserService {

    public static final String USERS_BY_LOGIN_CACHE = "usersByLogin";

    @Cacheable(cacheNames = USERS_BY_LOGIN_CACHE)
    public Optional<User> findByLogin(String login) {
        return userRepository.findOneByLogin(login);
    }

    @CacheEvict(cacheNames = USERS_BY_LOGIN_CACHE, key = "#user.login")
    public void updateUser(User user) {
        userRepository.save(user);
    }
}
```

Cache name `"usersByLogin"` phải đã được `createCache(...)` ở Bước 5, nếu không JCache sẽ ném lỗi khi lần đầu truy cập.

## Bước 8 — Testcontainers cho integration test

`pom.xml`:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-testcontainers</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>testcontainers</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>testcontainers-junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>
```

`src/test/java/com/example/myapp/config/RedisTestContainer.java`:

```java
package com.example.myapp.config;

import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;

@TestConfiguration(proxyBeanMethods = false)
public class RedisTestContainer {

    private static final GenericContainer<?> REDIS_CONTAINER = new GenericContainer<>("redis:8.10.1")
        .withExposedPorts(6379)
        .withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger(RedisTestContainer.class)))
        .withReuse(true);

    @Bean
    GenericContainer<?> redisContainer() {
        return REDIS_CONTAINER;
    }

    @Bean
    DynamicPropertyRegistrar redisProperties(GenericContainer<?> redisContainer) {
        return registry -> registry.add(
            "app.cache.redis.server",
            () -> "redis://" + redisContainer.getHost() + ":" + redisContainer.getMappedPort(6379)
        );
    }
}
```

Dùng trong annotation tổng hợp:

```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest(classes = { MyApp.class, RedisTestContainer.class })
public @interface IntegrationTest {}
```

> `withReuse(true)` chỉ có tác dụng khi bật `testcontainers.reuse.enable=true` trong `~/.testcontainers.properties`. Không bật thì container vẫn chạy bình thường, chỉ là mỗi test run tạo mới.
>
> `static` + `DynamicPropertyRegistrar` là cách chuẩn: Spring Boot `@ServiceConnection` **chưa** hỗ trợ Redisson-JCache nên phải set property thủ công.

## Bước 9 — Giảm log rác

`src/main/resources/logback-spring.xml`:

```xml
<logger name="org.redisson" level="WARN" />
```

Redisson log khá nhiều ở level INFO (connection pool, reconnect…), set WARN là hợp lý cho môi trường thường.

## Bước 10 — Docker Compose tổng & biến môi trường

Trong compose của app, override bằng env var — Spring Boot relaxed binding map `APP_CACHE_REDIS_SERVER` → `app.cache.redis.server`:

```yaml
name: myapp
services:
  app:
    image: myapp
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - APP_CACHE_REDIS_SERVER=redis://redis:6379
      - APP_CACHE_REDIS_CLUSTER=false
    depends_on:
      redis:
        condition: service_healthy
  redis:
    extends:
      file: ./redis.yml
      service: redis
```

Có password:

```yaml
- APP_CACHE_REDIS_SERVER=redis://default:${REDIS_PASSWORD}@redis:6379
```

## Bước 11 — Redis Cluster (tuỳ chọn)

`src/main/docker/redis-cluster.yml` — 6 node (3 master + 3 replica) cùng 1 container builder tự chạy `cluster create`:

```yaml
name: myapp
services:
  redis:
    image: redis:8.10.1
    command:
      - 'redis-server'
      - '--port 6379'
      - '--cluster-enabled yes'
      - '--cluster-config-file nodes.conf'
      - '--cluster-node-timeout 5000'
      - '--appendonly yes'
    ports: ['6379:6379']
  redis-1:
    image: redis:8.10.1
    command: [ 'redis-server', '--port 6379', '--cluster-enabled yes',
               '--cluster-config-file nodes.conf', '--cluster-node-timeout 5000', '--appendonly yes' ]
    ports: ['16379:6379']
  # … redis-2 → redis-5 tương tự, port 26379 / 36379 / 46379 / 56379
  redis-cluster-builder:
    build:
      context: .
      dockerfile: redis/Redis-Cluster.Dockerfile
```

`src/main/docker/redis/Redis-Cluster.Dockerfile`:

```dockerfile
FROM redis:8.10.1
RUN apt update && apt install dnsutils -y
ADD redis/connectRedisCluster.sh /usr/local/bin/connectRedisCluster
RUN chmod 755 /usr/local/bin/connectRedisCluster
ENTRYPOINT ["connectRedisCluster"]
```

`src/main/docker/redis/connectRedisCluster.sh`:

```bash
#!/usr/bin/env bash
set -euo pipefail

log() { echo "[$(date)]: $*"; }

log "Start Redis Cluster builder"
sleep 5

log "Connect all Redis containers"
redis-cli \
    --cluster-replicas 1 \
    --cluster-yes \
    --cluster create \
        $(host myapp-redis   | awk '{print $4}'):6379 \
        $(host myapp-redis-1 | awk '{print $4}'):6379 \
        $(host myapp-redis-2 | awk '{print $4}'):6379 \
        $(host myapp-redis-3 | awk '{print $4}'):6379 \
        $(host myapp-redis-4 | awk '{print $4}'):6379 \
        $(host myapp-redis-5 | awk '{print $4}'):6379
```

> Tên host `myapp-redis-N` = `<compose project name>-<service name>`. Đổi `myapp` cho khớp `name:` trong compose file, nếu không script sẽ fail ở `host`.

Cấu hình app phía client:

```yaml
app:
  cache:
    redis:
      cluster: true
      server:
        - redis://localhost:6379
        - redis://localhost:16379
        - redis://localhost:26379
```

## Bước 12 — Kiểm chứng setup hoạt động

```bash
# 1. App khởi động không lỗi
./mvnw spring-boot:run

# 2. Kiểm tra key sinh ra trong Redis
docker compose -f src/main/docker/redis.yml exec redis redis-cli
> KEYS *
> TTL "<tên key>"

# 3. Bật log SQL để chứng minh L2 cache có ăn
#    Gọi cùng 1 endpoint 2 lần — lần 2 không được có SELECT
```

`application-dev.yml` khi cần debug:

```yaml
logging:
  level:
    org.hibernate.SQL: DEBUG
    org.hibernate.cache: DEBUG
    org.redisson: INFO
spring:
  jpa:
    properties:
      hibernate.generate_statistics: true
```

## Bước 13 — Hardening cho production

| Hạng mục | Việc cần làm |
|---|---|
| **Password** | `requirepass` trên server; app dùng `redis://default:<pass>@host:6379`. **Không** hardcode — lấy từ env/secret manager |
| **Không expose public** | Redis nằm trong private network/VPC; nếu buộc phải mở, dùng firewall + TLS |
| **TLS** | Dùng scheme `rediss://` (2 chữ `s`), cấu hình truststore cho Redisson |
| **`maxmemory` + eviction** | `maxmemory 2gb`, `maxmemory-policy allkeys-lru` — nếu không, Redis đầy RAM sẽ bắt đầu từ chối ghi |
| **Persistence** | Cache thuần ⇒ có thể tắt RDB/AOF cho nhẹ. Nếu dùng chung cho dữ liệu khác thì bật AOF |
| **TTL** | `expiration: 3600` là mặc định chung. Muốn TTL riêng từng region thì tạo `MutableConfiguration` riêng và truyền vào `createCache` tương ứng |
| **HA** | Redis Sentinel (`useSentinelServers()`) hoặc Cluster. Single node = single point of failure |
| **Monitoring** | `jcacheConfig.setStatisticsEnabled(true)` đã bật ⇒ metric xuất qua Micrometer/Actuator; theo dõi thêm `redis_memory_used_bytes`, hit ratio, evicted keys |
| **Version pin** | Pin cụ thể `redis:8.10.1`, không dùng `latest` |

## Bước 14 — Checklist đối chiếu

- [ ] `redisson`, `spring-boot-starter-cache`, `hibernate-jcache` đã có trong `pom.xml`
- [ ] `CacheConfiguration` có `@EnableCaching`
- [ ] **`config.setCodec(new SerializationCodec())`** — không được quên
- [ ] Mọi entity/field được cache `implements Serializable`
- [ ] `HibernatePropertiesCustomizer` trỏ `ConfigSettings.CACHE_MANAGER`
- [ ] `hibernate.cache.use_second_level_cache: true` trong yml
- [ ] Mỗi `@Cache` (entity + collection) có một `createCache(...)` tương ứng
- [ ] Mỗi cache name dùng trong `@Cacheable` có một `createCache(...)`
- [ ] `RedisTestContainer` + `DynamicPropertyRegistrar` cho test
- [ ] `org.redisson` set level WARN trong logback
- [ ] Prod: password, TLS/private network, `maxmemory-policy`, HA

---

# PHỤ LỤC A — Hướng B: Spring Data Redis + Lettuce

Khi **không** cần Hibernate L2 cache, setup nhẹ hơn nhiều:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-cache</artifactId>
</dependency>
```

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      password: ${REDIS_PASSWORD:}
      timeout: 2s
      lettuce:
        pool:
          max-active: 16
          max-idle: 8
          min-idle: 2
  cache:
    type: redis
    redis:
      time-to-live: 1h
      cache-null-values: false
      key-prefix: "myapp:"
      use-key-prefix: true
```

```java
@Configuration
@EnableCaching
public class RedisCacheConfig {

    @Bean
    public RedisCacheConfiguration cacheConfiguration() {
        return RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofHours(1))
            .disableCachingNullValues()
            .serializeValuesWith(
                SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer())
            );
    }

    /** TTL riêng cho từng cache. */
    @Bean
    public RedisCacheManagerBuilderCustomizer cacheManagerCustomizer() {
        return builder -> builder
            .withCacheConfiguration("shortLived",
                RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofMinutes(5)));
    }
}
```

Test với Testcontainers ở hướng này đơn giản hơn vì có `@ServiceConnection`:

```java
@TestConfiguration(proxyBeanMethods = false)
public class RedisTestContainer {

    @Bean
    @ServiceConnection(name = "redis")
    GenericContainer<?> redisContainer() {
        return new GenericContainer<>("redis:8.10.1").withExposedPorts(6379);
    }
}
```

Không cần `DynamicPropertyRegistrar` — Boot tự bind `spring.data.redis.*`.

---

# PHỤ LỤC B — Lỗi thường gặp

| Triệu chứng | Nguyên nhân | Cách xử lý |
|---|---|---|
| `LazyInitializationException` ngẫu nhiên khi đọc từ cache | Codec mặc định của Redisson không bảo toàn Hibernate proxy | `config.setCodec(new SerializationCodec())` |
| `NotSerializableException` | Entity hoặc một field của nó thiếu `Serializable` | Cho class đó `implements Serializable`; field không cần cache thì đánh `transient` |
| `javax.cache.CacheException: Cache <name> not found` | Thiếu `createCache(...)` cho cache name đó | Thêm dòng `createCache` trong `cacheManagerCustomizer` |
| App treo khi khởi động, log Redisson retry | Redis chưa chạy / sai host / firewall | `redis-cli -h <host> -p 6379 ping` |
| `NOAUTH Authentication required` | Redis bật `requirepass` nhưng URL không có password | `redis://default:<pass>@host:6379` |
| `CROSSSLOT Keys ... don't hash to the same slot` | Chạy cluster nhưng `cluster: false` (hoặc ngược lại) | Đồng bộ `cluster` flag với deployment thực tế |
| L2 cache "không ăn", vẫn thấy SELECT | Thiếu `@Cache` trên entity, hoặc `use_second_level_cache: false`, hoặc cache region chưa `createCache` | Kiểm tra đủ 3 điểm |
| `ClassNotFoundException` sau khi đổi package/class | Cache cũ trong Redis chứa object serialize theo class cũ | `redis-cli FLUSHALL` (chỉ trên dev!) |
| Redis đầy RAM, ghi bị từ chối | Không set `maxmemory-policy` | `maxmemory-policy allkeys-lru` |

---

# Tham khảo

- Redisson JCache: https://github.com/redisson/redisson/wiki/14.-Integration-with-frameworks#144-jcache-api-jsr-107
- Redisson Hibernate: https://github.com/redisson/redisson/wiki/14.-Integration-with-frameworks#142-hibernate
- Spring Boot Caching: https://docs.spring.io/spring-boot/reference/io/caching.html
- Hibernate L2 Cache: https://docs.jboss.org/hibernate/orm/current/userguide/html_single/Hibernate_User_Guide.html#caching
- template cache options: https://www.template.tech/using-cache/
- Issue về codec/lazy-init: https://github.com/template/generator-template/issues/22889
