# Hướng dẫn setup Kafka (spring-kafka) cho project Spring Boot mới

> Viết cho một project Spring Boot **trắng, chưa có gì**, dùng **spring-kafka thuần** (`KafkaTemplate` + `@KafkaListener`) — không dùng Spring Cloud Stream.
>
> Phiên bản tham chiếu: **Spring Boot 4.1.1 / spring-kafka 4.1.1 / kafka-clients 4.2.1 / Java 21**. Mọi API trong tài liệu đã được kiểm chứng trên đúng các version này. Thay `com.example.app` bằng package thật của bạn.

**Mục lục**

1. [Dependencies](#1-dependencies)
2. [Dựng Kafka bằng Docker (KRaft, không Zookeeper)](#2-dựng-kafka-bằng-docker-kraft-không-zookeeper)
3. [Cấu hình application-kafka.yml](#3-cấu-hình-application-kafkayml)
4. [Tạo topic từ code](#4-tạo-topic-từ-code)
5. [Producer — KafkaTemplate](#5-producer--kafkatemplate)
6. [Consumer — @KafkaListener](#6-consumer--kafkalistener)
7. [Gửi/nhận object JSON — phần khác nhiều nhất ở Spring Boot 4](#7-gửinhận-object-json--phần-khác-nhiều-nhất-ở-spring-boot-4)
8. [Error handling, retry và DLQ](#8-error-handling-retry-và-dlq)
9. [Commit offset: auto hay manual](#9-commit-offset-auto-hay-manual)
10. [REST endpoint để bắn message thủ công](#10-rest-endpoint-để-bắn-message-thủ-công)
11. [Chạy thử end-to-end](#11-chạy-thử-end-to-end)
12. [Test](#12-test)
13. [Chuẩn bị cho production](#13-chuẩn-bị-cho-production)
14. [Xử lý sự cố thường gặp](#14-xử-lý-sự-cố-thường-gặp)
15. [Nếu bạn khởi tạo từ project Template này](#15-nếu-bạn-khởi-tạo-từ-project-template-này)
16. [Checklist](#16-checklist)

---

## 1. Dependencies

**Spring Boot 4 đã có starter riêng cho Kafka.** Trước đây (Boot 3.x) phải khai `org.springframework.kafka:spring-kafka` trực tiếp; giờ dùng starter, version do BOM của `spring-boot-starter-parent` quản lý nên **không khai version**:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-kafka</artifactId>
</dependency>
```

Starter này kéo theo `spring-boot-kafka` (auto-configuration) → `spring-kafka` → `kafka-clients`.

Cho test:

```xml
<!-- Gồm spring-boot-starter-test + spring-kafka-test (EmbeddedKafka) -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-kafka-test</artifactId>
    <scope>test</scope>
</dependency>
```

Chỉ thêm khi muốn test bằng Kafka thật trong Docker (xem mục 12.2):

```xml
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>testcontainers-kafka</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-testcontainers</artifactId>
    <scope>test</scope>
</dependency>
```

> **Không cần Spring Cloud.** Đây là điểm khác lớn nhất so với cách Spring Cloud Stream: không BOM `spring-cloud-dependencies`, không phải canh bảng tương thích Spring Cloud ↔ Spring Boot, không có lớp trừu tượng binder ở giữa.
>
> Ở Spring Boot 3.x, thay starter bằng:
> ```xml
> <dependency>
>     <groupId>org.springframework.kafka</groupId>
>     <artifactId>spring-kafka</artifactId>
> </dependency>
> ```

---

## 2. Dựng Kafka bằng Docker (KRaft, không Zookeeper)

Từ Kafka 4.x, Zookeeper **đã bị gỡ hoàn toàn**. Broker tự chạy controller qua KRaft. Mọi hướng dẫn trên mạng còn service `zookeeper` đều đã lỗi thời — bỏ qua.

Tạo `src/main/docker/kafka.yml`:

```yaml
# Cấu hình cho môi trường DEV. Lên production phải harden lại.
name: myapp
services:
  kafka:
    image: apache/kafka-native:4.3.1
    # Bỏ tiền tố "127.0.0.1:" nếu muốn máy khác trong LAN truy cập được
    ports:
      - 127.0.0.1:9092:9092
    environment:
      KAFKA_NODE_ID: 1
      KAFKA_PROCESS_ROLES: broker,controller
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092
      KAFKA_LISTENERS: PLAINTEXT://:9092,CONTROLLER://localhost:9093
      KAFKA_CONTROLLER_LISTENER_NAMES: CONTROLLER
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT
      KAFKA_CONTROLLER_QUORUM_VOTERS: 1@localhost:9093
      KAFKA_GROUP_INITIAL_REBALANCE_DELAY_MS: 0
    labels:
      # Ngăn Spring Boot Docker Compose tự động start container này
      org.springframework.boot.ignore: true
```

Vài điểm cần hiểu, vì đây là chỗ 90% lỗi "connect được rồi mà vẫn timeout":

- **`KAFKA_ADVERTISED_LISTENERS` là địa chỉ broker trả về cho client**, không phải địa chỉ client dùng để kết nối lần đầu. Client bootstrap tới `localhost:9092`, broker trả lời "hãy nói chuyện với tôi ở `localhost:9092`". Nếu advertised sai (ví dụ để `kafka:9092` trong khi app chạy ngoài Docker), client connect được lần đầu rồi treo.
- **App chạy ngoài Docker** → advertised = `localhost:9092`. **App chạy trong cùng Docker network** → advertised phải là `kafka:9092` (tên service).
- Muốn hỗ trợ cả hai cùng lúc thì khai hai listener:
  ```yaml
  KAFKA_LISTENERS: INTERNAL://:29092,EXTERNAL://:9092,CONTROLLER://localhost:9093
  KAFKA_ADVERTISED_LISTENERS: INTERNAL://kafka:29092,EXTERNAL://localhost:9092
  KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: CONTROLLER:PLAINTEXT,INTERNAL:PLAINTEXT,EXTERNAL:PLAINTEXT
  KAFKA_INTER_BROKER_LISTENER_NAME: INTERNAL
  ```
- `GROUP_INITIAL_REBALANCE_DELAY_MS: 0` cho dev để consumer group join ngay; production để mặc định (3000).
- `apache/kafka-native` là GraalVM native image — start dưới 1 giây, rất hợp cho dev/test. Production dùng `apache/kafka` (JVM) hoặc managed service.

Start:

```bash
docker compose -f src/main/docker/kafka.yml up -d
```

**Tùy chọn — để Spring Boot tự start Kafka khi chạy app.** Bỏ label `org.springframework.boot.ignore`, rồi trong `application.yml`:

```yaml
spring:
  docker:
    compose:
      enabled: true
      lifecycle-management: start-only   # start khi run, KHÔNG stop khi tắt app
      file: src/main/docker/kafka.yml
```

Spring Boot nhận diện service Kafka và tự set `spring.kafka.bootstrap-servers` — không cần khai tay khi dev.

---

## 3. Cấu hình application-kafka.yml

Tách Kafka ra file profile riêng để bật/tắt được và local dev không lỗi khi chưa có broker.

`src/main/resources/config/application-kafka.yml`:

```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9092

    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.apache.kafka.common.serialization.StringSerializer
      acks: all                 # chờ mọi replica in-sync ack
      retries: 3
      properties:
        enable.idempotence: true
        delivery.timeout.ms: 120000

    consumer:
      group-id: myapp           # BẮT BUỘC — xem giải thích bên dưới
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      auto-offset-reset: earliest
      enable-auto-commit: false # để container commit, không để client tự commit theo thời gian
      max-poll-records: 500

    listener:
      ack-mode: batch           # RECORD | BATCH | TIME | COUNT | COUNT_TIME | MANUAL | MANUAL_IMMEDIATE
      concurrency: 3            # số thread consume song song, <= số partition
      observation-enabled: true # metric + trace qua Micrometer

    template:
      observation-enabled: true
```

Kích hoạt profile. Gọn nhất là gắn vào profile group trong `application.yml`:

```yaml
spring:
  profiles:
    active: '@spring.profiles.active@'
    group:
      dev:
        - kafka
      prod:
        - kafka
```

Hoặc chạy trực tiếp: `./mvnw spring-boot:run -Dspring-boot.run.profiles=dev,kafka`

Ba property đáng dừng lại:

**`consumer.group-id`** — không có nó thì `@KafkaListener` không khai `groupId` sẽ fail lúc start. Consumer group là đơn vị chia partition và lưu offset: cùng group = chia việc; khác group = mỗi group nhận đủ mọi message.

**`auto-offset-reset`** — chỉ áp dụng khi group **chưa có offset** (lần đầu chạy, hoặc offset đã hết hạn). `earliest` = đọc từ đầu topic, `latest` (mặc định) = chỉ đọc message mới. Dev nên để `earliest`; production tùy nghiệp vụ, nhưng phải chọn có ý thức — đây là lý do phổ biến nhất của "app chạy mà không thấy message nào".

**`enable-auto-commit: false`** — với spring-kafka, luôn để `false` và giao việc commit cho listener container (`listener.ack-mode`). Auto-commit theo timer commit cả những message chưa xử lý xong → mất message khi app crash.

**Override broker khi deploy** bằng biến môi trường, không sửa file:

```
SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka:9092
```

---

## 4. Tạo topic từ code

Ở dev, broker tự tạo topic khi có message đầu tiên — nhưng topic đó sẽ có 1 partition, replication 1. Khai báo tường minh bằng bean `NewTopic`, `KafkaAdmin` sẽ tạo lúc app start:

```java
package com.example.app.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    public static final String ORDER_EVENTS = "order-events";
    public static final String ORDER_EVENTS_DLT = "order-events.DLT";

    @Bean
    NewTopic orderEventsTopic() {
        return TopicBuilder.name(ORDER_EVENTS)
            .partitions(6)
            .replicas(1)                       // production: 3
            .config("retention.ms", "604800000")   // 7 ngày
            .build();
    }

    @Bean
    NewTopic orderEventsDlt() {
        return TopicBuilder.name(ORDER_EVENTS_DLT).partitions(1).replicas(1).build();
    }
}
```

Hai lưu ý:

- `KafkaAdmin` chỉ **tạo mới**, **không sửa** topic đã tồn tại. Đổi `partitions` từ 6 lên 12 trong code sẽ không có tác dụng (tăng partition phải làm bằng `kafka-topics.sh --alter`). Giảm partition thì Kafka không cho — phải tạo topic mới.
- Ở production, nhiều team không cho app quyền tạo topic. Khi đó bỏ các bean này, tạo topic bằng IaC/Terraform, và app chỉ đọc/ghi.

---

## 5. Producer — KafkaTemplate

Spring Boot tự tạo bean `KafkaTemplate<Object, Object>`; inject theo kiểu cụ thể của bạn:

```java
package com.example.app.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class OrderEventPublisher {

    private static final String TOPIC = "order-events";

    private static final Logger LOG = LoggerFactory.getLogger(OrderEventPublisher.class);

    private final KafkaTemplate<String, String> kafkaTemplate;

    public OrderEventPublisher(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(String key, String payload) {
        kafkaTemplate
            .send(TOPIC, key, payload)
            .whenComplete((result, ex) -> {
                if (ex != null) {
                    LOG.error("Failed to send message key={}", key, ex);
                } else {
                    LOG.debug("Sent key={} to partition={} offset={}",
                        key,
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
                }
            });
    }
}
```

**`send()` là bất đồng bộ** và trả về `CompletableFuture<SendResult<K, V>>`. Bỏ qua future nghĩa là bỏ qua lỗi gửi — message rớt mà code vẫn chạy tiếp bình thường. Tối thiểu phải log như trên.

Cần chắc chắn đã gửi xong mới đi tiếp (hiếm khi cần, và đánh đổi throughput rất lớn):

```java
SendResult<String, String> result = kafkaTemplate.send(TOPIC, key, payload)
    .get(10, TimeUnit.SECONDS);
```

### Message key quyết định thứ tự

**Cùng một key luôn vào cùng một partition**, mà Kafka chỉ đảm bảo thứ tự *trong một partition*. Muốn các event của cùng một `orderId` được xử lý đúng thứ tự thì key phải là `orderId`. Không set key → round-robin → mất thứ tự.

Đây là lỗi thiết kế phổ biến nhất khi dùng Kafka, và nó chỉ lộ ra khi tải cao.

### Gửi kèm header

```java
ProducerRecord<String, String> record = new ProducerRecord<>(TOPIC, key, payload);
record.headers().add("event-type", "ORDER_CREATED".getBytes(StandardCharsets.UTF_8));
kafkaTemplate.send(record);
```

### Chỉ gửi sau khi transaction DB commit

Sai lầm kinh điển: gửi event trong `@Transactional`, transaction rollback nhưng message đã bay đi. Dùng `@TransactionalEventListener`:

```java
@Component
public class OrderEventForwarder {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public OrderEventForwarder(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(OrderCreatedEvent event) {
        kafkaTemplate.send("order-events", event.orderId(), event.toJson());
    }
}
```

Service publish bằng `ApplicationEventPublisher` bên trong transaction; Kafka chỉ nhận message khi commit thành công. Cần đảm bảo chắc chắn hơn nữa (không mất message kể cả khi app chết ngay sau commit) thì phải dùng **transactional outbox** — ghi event vào bảng DB cùng transaction, một job riêng đọc bảng đó đẩy sang Kafka.

---

## 6. Consumer — @KafkaListener

```java
package com.example.app.consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class OrderEventConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(OrderEventConsumer.class);

    @KafkaListener(topics = "order-events", groupId = "myapp")
    public void consume(String payload) {
        LOG.debug("Received: {}", payload);
        // xử lý nghiệp vụ
    }
}
```

`@EnableKafka` **không cần** khai — auto-configuration của Spring Boot đã bật sẵn.

Cần metadata (key, partition, offset, header) thì khai thêm tham số, spring-kafka tự resolve:

```java
@KafkaListener(topics = "order-events", groupId = "myapp")
public void consume(
    @Payload String payload,
    @Header(KafkaHeaders.RECEIVED_KEY) String key,
    @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
    @Header(KafkaHeaders.OFFSET) long offset
) {
    LOG.debug("key={} partition={} offset={} payload={}", key, partition, offset, payload);
}
```

Hoặc nhận thẳng `ConsumerRecord` khi cần tất cả:

```java
@KafkaListener(topics = "order-events", groupId = "myapp")
public void consume(ConsumerRecord<String, String> record) {
    record.headers().forEach(h -> LOG.debug("{}={}", h.key(), new String(h.value())));
}
```

### Batch listener

Xử lý theo lô hiệu quả hơn nhiều khi phải ghi DB (một `INSERT` nhiều dòng thay vì N lần):

```yaml
spring:
  kafka:
    listener:
      type: batch
```

```java
@KafkaListener(topics = "order-events", groupId = "myapp", batch = "true")
public void consume(List<String> payloads) {
    repository.saveAll(parse(payloads));
}
```

Đánh đổi: một record lỗi làm cả lô lỗi. Cần xử lý riêng từng record trong lô thì dùng `BatchListenerFailedHandler` — hoặc đơn giản hơn là đừng dùng batch.

### Vài điều về `@KafkaListener`

- **Listener chạy trên thread riêng của container**, không phải thread HTTP. Đừng giả định có `SecurityContext`, `RequestContext`, hay transaction sẵn.
- **`concurrency` không vượt quá số partition.** Thừa thread thì thread đó idle vĩnh viễn.
- **Xử lý lâu quá `max.poll.interval.ms` (mặc định 5 phút) → broker coi consumer đã chết → rebalance → message bị xử lý lại.** Việc nặng phải đẩy sang thread pool khác hoặc tăng `max.poll.interval.ms`.
- Đặt `id` cho listener nếu muốn start/stop nó lúc runtime qua `KafkaListenerEndpointRegistry`.

---

## 7. Gửi/nhận object JSON — phần khác nhiều nhất ở Spring Boot 4

**Đây là chỗ mọi hướng dẫn cũ trên mạng sẽ làm bạn mất thời gian.**

Spring Boot 4 dùng **Jackson 3** (package `tools.jackson`), không phải Jackson 2 (`com.fasterxml.jackson`). spring-kafka 4.x vì vậy có **hai bộ serializer song song**:

| Class | Jackson | Dùng ở |
|---|---|---|
| `JsonSerializer` / `JsonDeserializer` | 2 (`com.fasterxml.jackson`) | Spring Boot 3.x |
| `JacksonJsonSerializer` / `JacksonJsonDeserializer` | 3 (`tools.jackson`) | **Spring Boot 4.x** |

Ở Spring Boot 4, Jackson 2 **không có trên classpath**. Copy cấu hình `JsonSerializer` từ blog cũ sẽ ra `ClassNotFoundException: com.fasterxml.jackson.databind.ObjectMapper` lúc runtime — không phải lỗi compile, nên nó chỉ nổ khi gửi message đầu tiên.

### Cấu hình

```yaml
spring:
  kafka:
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JacksonJsonSerializer
    consumer:
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      # Bọc bằng ErrorHandlingDeserializer — xem giải thích bên dưới
      value-deserializer: org.springframework.kafka.support.serializer.ErrorHandlingDeserializer
      properties:
        spring.deserializer.value.delegate.class: org.springframework.kafka.support.serializer.JacksonJsonDeserializer
        spring.json.trusted.packages: com.example.app.event
        spring.json.value.default.type: com.example.app.event.OrderEvent
        spring.json.use.type.headers: false
```

Payload dùng record:

```java
package com.example.app.event;

import java.time.Instant;

public record OrderEvent(String orderId, String status, Instant occurredAt) {}
```

Producer và consumer giờ làm việc với object:

```java
private final KafkaTemplate<String, OrderEvent> kafkaTemplate;

kafkaTemplate.send("order-events", event.orderId(), event);
```

```java
@KafkaListener(topics = "order-events", groupId = "myapp")
public void consume(OrderEvent event) { /* ... */ }
```

### Bốn property phải hiểu, không hiểu là mất buổi chiều

**`spring.deserializer.value.delegate.class`** — `ErrorHandlingDeserializer` bọc ngoài deserializer thật. **Bắt buộc phải có.** Không có nó, một message JSON hỏng (poison pill) sẽ ném exception *trước khi* listener được gọi, error handler không bắt được, offset không commit → consumer đọc lại chính message đó **vô hạn**, log ngập và không bao giờ tiến lên. Có `ErrorHandlingDeserializer` thì lỗi được chuyển thành `null` payload kèm exception header, error handler xử lý được và đẩy sang DLQ.

**`spring.json.trusted.packages`** — deserializer chỉ tạo object thuộc package trong danh sách này. Để `*` là lỗ hổng deserialization (attacker gửi message chỉ định class tùy ý). **Luôn liệt kê package cụ thể.**

**`spring.json.use.type.headers: false` + `spring.json.value.default.type`** — mặc định `JacksonJsonSerializer` nhét tên class đầy đủ (`__TypeId__`) vào header, và deserializer dùng nó để chọn class. Điều này buộc producer và consumer phải **cùng tên package** — đổi package một bên là hỏng. Tắt type header và khai `value.default.type` ở consumer là cách bền vững hơn.

Nếu một topic chở nhiều loại event, dùng type mapping thay vì tên class đầy đủ:

```yaml
# Producer
spring.json.type.mapping: order:com.example.app.event.OrderEvent,payment:com.example.app.event.PaymentEvent
# Consumer — cùng alias, khác package cũng không sao
spring.json.type.mapping: order:com.example.other.OrderEvent,payment:com.example.other.PaymentEvent
```

### Tương thích schema

- Chỉ **thêm field optional**. Đừng xóa/đổi tên/đổi kiểu field cũ — consumer version cũ vẫn đang chạy và đang đọc message version mới.
- Field lạ làm deserialize ném exception. Với Jackson 3 trong Boot 4: `spring.jackson.deserialization.fail-on-unknown-properties: false`.
- Contract chặt chẽ giữa nhiều team → cân nhắc Avro/Protobuf + Schema Registry. Hệ thống nhỏ thì JSON là đủ.

---

## 8. Error handling, retry và DLQ

**Mặc định của spring-kafka:** listener ném exception → `DefaultErrorHandler` retry **9 lần, cách nhau 0ms** → vẫn lỗi thì **log rồi bỏ qua**, commit offset. Message biến mất, chỉ còn một dòng ERROR trong log.

Đây là mặc định không dùng được ở production. Có hai cách sửa, chọn một.

### 8.1. Blocking retry + DLQ — đơn giản, đủ cho phần lớn trường hợp

Retry ngay tại chỗ, hết lượt thì đẩy sang topic `.DLT`:

```java
package com.example.app.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.ExponentialBackOff;

@Configuration
public class KafkaErrorConfig {

    @Bean
    DefaultErrorHandler errorHandler(KafkaOperations<Object, Object> template) {
        // Mặc định đẩy sang "<topic gốc>.DLT", cùng partition với record gốc
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(template);

        ExponentialBackOff backOff = new ExponentialBackOff(1000L, 2.0);
        backOff.setMaxInterval(10_000L);
        backOff.setMaxAttempts(4);

        DefaultErrorHandler handler = new DefaultErrorHandler(recoverer, backOff);

        // Lỗi không bao giờ tự hết thì đừng retry — cho sang DLT luôn
        handler.addNotRetryableExceptions(IllegalArgumentException.class, ValidationException.class);

        return handler;
    }
}
```

Bean `DefaultErrorHandler` được auto-configuration nhặt và gắn vào mọi listener container.

DLT phải có sẵn partition tương ứng với topic gốc, nếu không recoverer sẽ lỗi. Cách an toàn: cho recoverer luôn ghi vào partition 0.

```java
DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
    template,
    (record, ex) -> new TopicPartition(record.topic() + ".DLT", 0)
);
```

**Điểm yếu của cách này:** trong lúc retry, **cả partition bị chặn**. Retry 4 lần cách nhau tới 10 giây nghĩa là 30+ giây không message nào khác trong partition đó được xử lý. Backoff dài thì phải dùng cách 8.2.

### 8.2. Non-blocking retry (`@RetryableTopic`) — không chặn partition

Spring-kafka tự tạo chuỗi topic `order-events-retry-0`, `-retry-1`, ... và `order-events-dlt`. Message lỗi được **chuyển sang topic retry** rồi commit offset ngay, partition gốc chạy tiếp.

Bật global cho mọi listener:

```yaml
spring:
  kafka:
    retry:
      topic:
        enabled: true
        attempts: 4
        backoff:
          delay: 1s
          multiplier: 2
          max-delay: 30s
          jitter: 500ms
```

Hoặc khai từng listener (chi tiết hơn):

```java
import org.springframework.kafka.annotation.BackOff;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.RetryableTopic;

@Component
public class OrderEventConsumer {

    @RetryableTopic(
        attempts = "4",
        backOff = @BackOff(delay = 1000, multiplier = 2.0, maxDelay = 30000),
        exclude = { IllegalArgumentException.class },
        dltTopicSuffix = "-dlt",
        autoCreateTopics = "true"
    )
    @KafkaListener(topics = "order-events", groupId = "myapp")
    public void consume(OrderEvent event) {
        // ...
    }

    @DltHandler
    public void handleDlt(OrderEvent event,
                          @Header(KafkaHeaders.ORIGINAL_TOPIC) String topic,
                          @Header(KafkaHeaders.DLT_EXCEPTION_MESSAGE) String errorMessage) {
        LOG.error("Message dead-lettered from {}: {} — {}", topic, event, errorMessage);
        // ghi DB, bắn alert...
    }
}
```

> Lưu ý version: từ spring-kafka 3.2 trở đi, thuộc tính là **`backOff = @BackOff(...)`** (`org.springframework.kafka.annotation.BackOff`). Cú pháp cũ `backoff = @Backoff(...)` (dùng `org.springframework.retry.annotation.Backoff`) đã bị thay — code mẫu cũ trên mạng sẽ không compile.

**Đánh đổi:** thứ tự message không còn được đảm bảo (message lỗi đi vòng qua topic retry rồi quay lại sau). Nếu thứ tự quan trọng, dùng 8.1.

### 8.3. Ba việc phải làm cùng lúc, bất kể chọn cách nào

1. Bật DLQ/DLT.
2. **Có alert trên DLT.** DLT không ai nhìn = thùng rác có nhãn đẹp.
3. Có cách **replay** message từ DLT về topic gốc sau khi fix bug. Viết sẵn một script hoặc một admin endpoint — đừng đợi đến lúc sự cố mới nghĩ.

Header `kafka_dlt-exception-message`, `kafka_dlt-original-topic`, `kafka_dlt-original-offset`, `kafka_dlt-original-consumer-group` được recoverer gắn sẵn vào record trong DLT, đủ để chẩn đoán và replay.

---

## 9. Commit offset: auto hay manual

`spring.kafka.listener.ack-mode` quyết định khi nào offset được commit:

| AckMode | Commit khi nào | Dùng khi |
|---|---|---|
| `RECORD` | Sau mỗi record xử lý xong | Cần chính xác nhất, chậm hơn |
| `BATCH` (mặc định) | Sau khi xử lý xong cả batch của một lần `poll()` | Mặc định tốt cho hầu hết trường hợp |
| `TIME` / `COUNT` / `COUNT_TIME` | Theo chu kỳ / số lượng | Throughput cao, chấp nhận xử lý lại nhiều hơn |
| `MANUAL` | Bạn gọi `ack.acknowledge()`, commit ở cuối batch | Cần kiểm soát |
| `MANUAL_IMMEDIATE` | Bạn gọi `ack.acknowledge()`, commit ngay | Cần kiểm soát chặt |

Manual ack:

```yaml
spring:
  kafka:
    listener:
      ack-mode: manual
```

```java
@KafkaListener(topics = "order-events", groupId = "myapp")
public void consume(OrderEvent event, Acknowledgment ack) {
    try {
        process(event);
        ack.acknowledge();
    } catch (RecoverableException e) {
        // KHÔNG ack => message sẽ được đọc lại
        throw e;
    }
}
```

> **Đừng dùng manual ack nếu không có lý do cụ thể.** Quên gọi `acknowledge()` ở một nhánh code là consumer đứng im, lag tăng dần, và không có log lỗi nào cả. `BATCH` + `DefaultErrorHandler` xử lý đúng cho gần như mọi trường hợp.

**Consumer phải idempotent.** Kafka đảm bảo **at-least-once**, không phải exactly-once. Rebalance, retry, hay app crash sau khi xử lý nhưng trước khi commit offset đều dẫn tới xử lý lại cùng một message. Cách thường dùng: mỗi event có `eventId`, có bảng `processed_event(event_id PRIMARY KEY)`, insert trước khi xử lý, trùng khóa thì bỏ qua. Đây là yêu cầu thiết kế, không config được.

---

## 10. REST endpoint để bắn message thủ công

Rất đáng có lúc dev:

```java
package com.example.app.web.rest;

import com.example.app.service.OrderEventPublisher;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/kafka")
@Profile("dev")
public class KafkaResource {

    private final OrderEventPublisher publisher;

    public KafkaResource(OrderEventPublisher publisher) {
        this.publisher = publisher;
    }

    @PostMapping("/publish")
    public void publish(@RequestParam("key") String key, @RequestParam("message") String message) {
        publisher.publish(key, message);
    }
}
```

`@Profile("dev")` không phải trang trí — endpoint này bắn message tùy ý vào hệ thống, để lọt ra production là lỗ hổng thật.

---

## 11. Chạy thử end-to-end

```bash
# 1. Start Kafka
docker compose -f src/main/docker/kafka.yml up -d

# 2. Start app
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev,kafka
```

Liệt kê topic:

```bash
docker compose -f src/main/docker/kafka.yml exec kafka \
  /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --list
```

Nghe topic:

```bash
docker compose -f src/main/docker/kafka.yml exec kafka \
  /opt/kafka/bin/kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 --topic order-events --from-beginning \
  --property print.key=true --property print.headers=true
```

Bắn message qua REST:

```bash
curl -X POST "http://localhost:8080/api/kafka/publish?key=ORD-1&message=hello"
```

Bắn thẳng vào topic để test consumer:

```bash
docker compose -f src/main/docker/kafka.yml exec -it kafka \
  /opt/kafka/bin/kafka-console-producer.sh \
  --bootstrap-server localhost:9092 --topic order-events \
  --property parse.key=true --property key.separator=:
# gõ:  ORD-1:{"orderId":"ORD-1","status":"CREATED","occurredAt":"2026-01-01T00:00:00Z"}
```

**Kiểm tra consumer group và lag** — đây là chỉ báo sức khỏe quan trọng nhất:

```bash
docker compose -f src/main/docker/kafka.yml exec kafka \
  /opt/kafka/bin/kafka-consumer-groups.sh \
  --bootstrap-server localhost:9092 --describe --group myapp
```

Cột `LAG` tăng dần = consumer xử lý không kịp. `CONSUMER-ID` trống = không có consumer nào đang chạy.

Health check qua Actuator (cần `spring-boot-starter-actuator`):

```bash
curl http://localhost:8080/actuator/health | jq
```

---

## 12. Test

### 12.1. `@EmbeddedKafka` — dùng cho hầu hết test

`spring-kafka-test` nhúng broker Kafka chạy in-process, **không cần Docker**. Từ spring-kafka 4.x, broker nhúng luôn là KRaft (không còn tham số `kraft`).

```java
package com.example.app.consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("kafka")
@EmbeddedKafka(partitions = 1, topics = { "order-events" })
class OrderEventConsumerIT {

    @Autowired
    private KafkaTemplate<String, OrderEvent> kafkaTemplate;

    @Autowired
    private OrderRepository repository;   // side effect của consumer

    @Test
    void consumesOrderEvent() {
        kafkaTemplate.send("order-events", "ORD-1",
            new OrderEvent("ORD-1", "CREATED", Instant.now()));

        await().atMost(Duration.ofSeconds(10))
            .untilAsserted(() -> assertThat(repository.findById("ORD-1")).isPresent());
    }
}
```

`@EmbeddedKafka` tự set property `spring.embedded.kafka.brokers`. Để Spring Boot dùng nó thay cho `localhost:9092`, thêm vào `src/test/resources/config/application.yml`:

```yaml
spring:
  kafka:
    bootstrap-servers: ${spring.embedded.kafka.brokers}
    consumer:
      auto-offset-reset: earliest
```

Ba nguyên tắc để test Kafka không flaky:

1. **Đừng dùng `Thread.sleep`.** Dùng `Awaitility` (`await().atMost(...).untilAsserted(...)`) hoặc `CountDownLatch` trong listener test. Kafka bất đồng bộ, sleep cố định sẽ pass trên máy bạn và fail trên CI.
2. **`auto-offset-reset: earliest` trong test.** Nếu không, producer gửi trước khi consumer join group xong là message mất luôn, test fail ngẫu nhiên.
3. **Mỗi test class dùng `groupId` riêng** nếu chạy song song, tránh tranh partition với nhau.

Muốn assert message được **gửi ra** thay vì được xử lý, tạo consumer test thủ công:

```java
Map<String, Object> props = KafkaTestUtils.consumerProps("test-group", "true", embeddedKafka);
try (Consumer<String, String> consumer = new DefaultKafkaConsumerFactory<String, String>(props)
        .createConsumer()) {
    embeddedKafka.consumeFromAnEmbeddedTopic(consumer, "order-events");
    publisher.publish("ORD-1", "payload");
    ConsumerRecord<String, String> record =
        KafkaTestUtils.getSingleRecord(consumer, "order-events", Duration.ofSeconds(10));
    assertThat(record.value()).isEqualTo("payload");
}
```

### 12.2. Testcontainers — khi cần broker thật

Chỉ dùng cho vài test verify hành vi thật (rebalance, cấu hình broker cụ thể, DLT). Chậm hơn `@EmbeddedKafka` đáng kể.

```java
package com.example.app.config;

import java.time.Duration;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.kafka.KafkaContainer;

@TestConfiguration(proxyBeanMethods = false)
public class KafkaTestContainer {

    // static => container dùng chung cho mọi test class, chỉ start một lần
    private static final KafkaContainer KAFKA_CONTAINER = new KafkaContainer("apache/kafka-native:4.3.1")
        .withStartupTimeout(Duration.ofMinutes(2))
        .withStartupAttempts(3)
        .withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger(KafkaTestContainer.class)));

    @Bean
    @ServiceConnection
    KafkaContainer kafkaContainer() {
        return KAFKA_CONTAINER;
    }
}
```

Dùng: `@SpringBootTest(classes = { MyApp.class, KafkaTestContainer.class })`

`@ServiceConnection` tự set `spring.kafka.bootstrap-servers` trỏ vào container — **không cần** `@DynamicPropertySource`. Đây là một lợi thế thật của spring-kafka thuần so với Spring Cloud Stream, nơi binder đọc property riêng và phải nối tay.

---

## 13. Chuẩn bị cho production

Cấu hình ở mục 3 là **cấu hình dev**. Đây là những gì phải đổi.

### 13.1. Không mất message ở phía producer

```yaml
spring:
  kafka:
    producer:
      acks: all
      retries: 2147483647
      properties:
        enable.idempotence: true
        max.in.flight.requests.per.connection: 5
        delivery.timeout.ms: 120000
        compression.type: lz4
```

`acks: all` + `enable.idempotence: true` = không mất, không trùng (trong phạm vi một producer session). Đánh đổi là latency cao hơn. Với event quan trọng (thanh toán, đơn hàng) thì bắt buộc.

Phía broker/topic, đặt `min.insync.replicas=2` với `replication.factor=3` — thiếu cái này thì `acks: all` không có ý nghĩa thật khi chỉ còn 1 replica sống.

### 13.2. Topic

- `replication.factor: 3`, `min.insync.replicas: 2`
- Tắt auto-create topic ở broker (`auto.create.topics.enable=false`). Bật ở production nghĩa là một typo trong tên topic sẽ tạo topic rác 1 partition/1 replica, và bạn chỉ phát hiện khi broker đó chết.
- Số partition: ước lượng theo throughput mục tiêu và số consumer tối đa. **Tăng được, giảm không được** — và tăng partition sẽ phá thứ tự theo key với dữ liệu cũ.

### 13.3. Consumer

```yaml
spring:
  kafka:
    consumer:
      enable-auto-commit: false
      max-poll-records: 100          # giảm nếu xử lý mỗi record nặng
      properties:
        max.poll.interval.ms: 300000
        session.timeout.ms: 45000
        heartbeat.interval.ms: 3000
    listener:
      ack-mode: batch
      concurrency: 3
```

`max-poll-records` × thời gian xử lý mỗi record phải **nhỏ hơn** `max.poll.interval.ms`. Vi phạm điều này là nguyên nhân của rebalance loop — consumer bị đá ra khỏi group, message bị xử lý lại, lag tăng vô hạn.

### 13.4. Bảo mật (SASL/SSL)

Managed Kafka (Confluent Cloud, MSK, Aiven) gần như luôn dùng SASL_SSL:

```yaml
spring:
  kafka:
    bootstrap-servers: ${KAFKA_BROKERS}
    security:
      protocol: SASL_SSL
    properties:
      sasl.mechanism: SCRAM-SHA-512
      sasl.jaas.config: >
        org.apache.kafka.common.security.scram.ScramLoginModule required
        username="${KAFKA_USERNAME}" password="${KAFKA_PASSWORD}";
```

Credentials **luôn qua biến môi trường / secret manager**, không commit vào repo.

### 13.5. Observability

```yaml
spring:
  kafka:
    listener:
      observation-enabled: true
    template:
      observation-enabled: true

management:
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus
```

Bật `observation-enabled` cho phép Micrometer sinh metric và **propagate trace context qua Kafka header** — trace một request đi từ HTTP → Kafka → consumer liền mạch. Không bật thì trace đứt ở ranh giới Kafka.

Cần theo dõi: **consumer lag** (quan trọng nhất), số message vào DLT, thời gian xử lý mỗi record, số lần rebalance, `record-error-rate` phía producer.

---

## 14. Xử lý sự cố thường gặp

**Consumer không nhận được message nào, log không báo lỗi**

Theo thứ tự này:
1. `kafka-console-consumer.sh` có thấy message trong topic không? Không thấy → lỗi ở producer, không phải consumer.
2. `kafka-consumer-groups.sh --describe --group <group>` — group có consumer đang active không? `CURRENT-OFFSET` có bằng `LOG-END-OFFSET` không? Nếu bằng nghĩa là group đã "đọc" hết rồi.
3. `auto-offset-reset` là `latest` (mặc định) và group mới tạo → message cũ bị bỏ qua. Đổi sang `earliest` hoặc reset offset.
4. Tên topic có typo không? `@KafkaListener(topics = "order-event")` vs topic thật `order-events` — không lỗi, chỉ là im lặng.
5. Class listener có `@Component` không, có nằm trong package được scan không?

**`TimeoutException: Topic ... not present in metadata after 60000 ms`**

Gần như luôn là `KAFKA_ADVERTISED_LISTENERS` sai — đọc lại mục 2. Kiểm chứng: `docker compose exec kafka /opt/kafka/bin/kafka-broker-api-versions.sh --bootstrap-server localhost:9092` in ra chính địa chỉ advertised, so với địa chỉ app đang dùng.

**Log ngập `Connection to node -1 could not be established`**

Broker chưa lên hoặc sai port. `-1` là node bootstrap — chưa kết nối được lần đầu. `docker compose ps` xem container còn sống không.

**`ClassNotFoundException: com.fasterxml.jackson.databind.ObjectMapper`**

Bạn đang dùng `JsonSerializer`/`JsonDeserializer` (Jackson 2) trên Spring Boot 4 (Jackson 3). Đổi sang `JacksonJsonSerializer`/`JacksonJsonDeserializer`. Xem mục 7.

**Consumer đọc đi đọc lại cùng một message, vô hạn**

Poison pill: message không deserialize được, exception ném ra *trước* listener nên error handler không bắt được, offset không commit. Bọc bằng `ErrorHandlingDeserializer` (mục 7). Đây là lý do property đó là bắt buộc, không phải tùy chọn.

**`Deserialization error` nhưng payload nhìn vẫn đúng JSON**

Header `__TypeId__` chỉ tên class mà consumer không có (hoặc khác package). Tắt `spring.json.use.type.headers` và dùng `spring.json.value.default.type`, hoặc dùng `spring.json.type.mapping`.

**Rebalance liên tục, lag tăng dần**

Xử lý một batch lâu hơn `max.poll.interval.ms`. Giảm `max-poll-records`, hoặc tăng `max.poll.interval.ms`, hoặc đẩy việc nặng sang thread pool riêng.

**`@RetryableTopic` không compile — `backoff` không tồn tại**

Từ spring-kafka 3.2, thuộc tính là `backOff = @BackOff(...)` với `org.springframework.kafka.annotation.BackOff`, không phải `backoff = @Backoff(...)` của `spring-retry`.

**Message xử lý sai thứ tự**

Không set message key, hoặc đang dùng `@RetryableTopic` (non-blocking retry phá thứ tự theo thiết kế), hoặc `concurrency > 1` với key phân bố không đều. Xem mục 5.

**Test khi pass khi fail**

`Thread.sleep` thay vì `Awaitility`, hoặc thiếu `auto-offset-reset: earliest` trong config test. Xem mục 12.1.

**`KafkaAdmin` không tăng số partition dù đã sửa `NewTopic`**

Đúng như thiết kế — `KafkaAdmin` chỉ tạo mới, không sửa topic đã tồn tại. Dùng `kafka-topics.sh --alter --partitions N`.

---

## 15. Nếu bạn khởi tạo từ project Template này

Project `template` trong repo được sinh với `messageBroker: kafka`, nhưng nó dùng **Spring Cloud Stream**, không phải spring-kafka thuần. Để chuyển sang cách trong tài liệu này, **xóa** những thứ sau:

**Trong `pom.xml`:**
- `spring-cloud-starter-stream-kafka` (`pom.xml:347-350`)
- `spring-cloud-stream` (`pom.xml:351-354`)
- `spring-cloud-stream-test-binder` (`pom.xml:355-359`)
- BOM `spring-cloud-dependencies` (`pom.xml:74-80`) — chỉ khi không còn dùng Spring Cloud cho việc khác. Project này còn dùng `spring-cloud-starter-openfeign` và `spring-cloud-starter-circuitbreaker-resilience4j`, nên **giữ lại BOM**.

Thay bằng `spring-boot-starter-kafka` + `spring-boot-starter-kafka-test`.

**Xóa code:**
- `src/main/java/io/tcbs/broker/KafkaProducer.java` — bean `Supplier<String>` này bị Spring Cloud Stream poll **mỗi giây** và bắn chuỗi hằng `"kafka_producer"` ra topic. Chỉ là demo, ở project thật là traffic rác.
- `src/main/java/io/tcbs/broker/KafkaConsumer.java` — `Consumer<String>` giữ `HashMap<String, SseEmitter>` để đẩy message ra SSE. Ngoài việc phụ thuộc API của Spring Cloud Stream, nó còn có hai lỗi thật: `HashMap` không thread-safe nhưng bị ghi từ HTTP thread và đọc từ Kafka listener thread; và emitter nằm trong bộ nhớ một instance nên scale nhiều pod là user nối vào pod A không nhận được message do pod B xử lý.
- `src/main/java/io/tcbs/web/rest/JavaJhipsterKafkaResource.java` — dùng `StreamBridge`, thay bằng `KafkaTemplate` (mục 10).
- `src/test/java/io/tcbs/web/rest/JavaJhipsterKafkaResourceIT.java` — dựa trên `TestChannelBinderConfiguration` của Spring Cloud Stream, không còn dùng được.

**Thay cấu hình:** toàn bộ `src/main/resources/config/application-kafka.yml` (`spring.cloud.function.definition` + `spring.cloud.stream.*`) → thay bằng khối `spring.kafka.*` ở mục 3.

**Giữ lại:**
- `src/main/docker/kafka.yml` — dùng nguyên, không phụ thuộc cách tích hợp.
- `src/test/java/io/tcbs/config/KafkaTestContainer.java` — dùng được, thậm chí sạch hơn vì `@ServiceConnection` set đúng `spring.kafka.bootstrap-servers` mà không cần `@DynamicPropertySource`. Có thể bỏ dòng `.withEnv("KAFKA_LISTENERS", ...)`.
- Profile group `kafka` trong `application.yml:114-122`.
- Biến `SPRING_CLOUD_STREAM_KAFKA_BINDER_BROKERS` trong `src/main/docker/app.yml:20` → đổi thành `SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka:9092`.

---

## 16. Checklist

Setup cơ bản:

- [ ] `spring-boot-starter-kafka` (Boot 4) hoặc `spring-kafka` (Boot 3.x)
- [ ] `spring-boot-starter-kafka-test` scope `test`
- [ ] `src/main/docker/kafka.yml` (KRaft, không Zookeeper)
- [ ] `KAFKA_ADVERTISED_LISTENERS` khớp với nơi app chạy
- [ ] `application-kafka.yml` với `spring.kafka.bootstrap-servers`, `consumer.group-id`
- [ ] `auto-offset-reset` chọn có ý thức (`earliest` cho dev)
- [ ] `enable-auto-commit: false`, dùng `listener.ack-mode`
- [ ] Bean `NewTopic` cho mỗi topic (hoặc tạo bằng IaC)
- [ ] Producer log kết quả của `CompletableFuture` trả về từ `send()`
- [ ] Message key được set khi thứ tự có ý nghĩa
- [ ] Verify end-to-end bằng `kafka-console-consumer.sh`

JSON:

- [ ] Dùng `JacksonJsonSerializer`/`JacksonJsonDeserializer` (Boot 4), **không** `JsonSerializer`/`JsonDeserializer`
- [ ] Bọc bằng `ErrorHandlingDeserializer` — chống poison pill
- [ ] `spring.json.trusted.packages` liệt kê cụ thể, **không** để `*`
- [ ] `spring.json.use.type.headers: false` + `value.default.type`, hoặc `type.mapping`

Trước khi lên production:

- [ ] `acks: all` + `enable.idempotence: true`
- [ ] `replication.factor: 3` + `min.insync.replicas: 2`
- [ ] Error handler + DLT (blocking hoặc `@RetryableTopic`)
- [ ] Alert trên DLT + quy trình replay đã viết sẵn
- [ ] Consumer idempotent (bảng dedupe theo `eventId`)
- [ ] `max-poll-records` × thời gian xử lý < `max.poll.interval.ms`
- [ ] Event chỉ gửi sau khi transaction DB commit
- [ ] SASL/SSL + credentials từ secret manager
- [ ] `observation-enabled: true` cho listener và template
- [ ] Monitoring consumer lag
- [ ] Endpoint `/api/kafka/publish` đã `@Profile("dev")` hoặc gỡ bỏ

---

## Tham khảo

| Nội dung | Nguồn |
|---|---|
| Docker Compose Kafka KRaft | `src/main/docker/kafka.yml` trong repo này |
| Testcontainers config | `src/test/java/io/tcbs/config/KafkaTestContainer.java` |
| Profile group `kafka` | `src/main/resources/config/application.yml:114-122` |

**Tài liệu ngoài**

- Spring for Apache Kafka reference: https://docs.spring.io/spring-kafka/reference/
- Non-blocking retry / `@RetryableTopic`: https://docs.spring.io/spring-kafka/reference/retrytopic.html
- Spring Boot — Messaging with Kafka: https://docs.spring.io/spring-boot/reference/messaging/kafka.html
- Toàn bộ property `spring.kafka.*`: https://docs.spring.io/spring-boot/appendix/application-properties/#appendix.application-properties.integration
- Apache Kafka docs: https://kafka.apache.org/documentation/
