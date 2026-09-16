package io.tcbs.template.messaging.publisher;

import io.tcbs.template.enums.OutboxStatus;
import io.tcbs.template.model.OutboxEvent;
import io.tcbs.template.repository.OutboxEventRepository;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxEventPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Async
    public void send(OutboxEvent event) {
        ProducerRecord<String, String> record = buildRecord(event);
        outboxEventRepository.markStatus(event.getCode(), OutboxStatus.SENDING);
        try {
            log.debug("[Outbox] Gửi Kafka requestId: {} thành công");
            kafkaTemplate.send(record).get(3000L, TimeUnit.MILLISECONDS);
            outboxEventRepository.markStatus(event.getCode(), OutboxStatus.PUBLISHED, Instant.now());
        } catch (Exception e) {
            log.error("[Outbox] Gửi Kafka requestId: {} thất bại: {}", event.getRequestId(), e.getMessage());
            outboxEventRepository.markStatus(event.getCode(), OutboxStatus.FAILED);
        }
    }

    private ProducerRecord<String, String> buildRecord(OutboxEvent event) {
        ProducerRecord<String, String> record = new ProducerRecord<>(event.getTopic(), null, event.getAggregateCode(), event.getPayload());
        record.headers().add(new RecordHeader("requestId", event.getRequestId().getBytes(StandardCharsets.UTF_8)));
        record.headers().add(new RecordHeader("code", event.getCode().getBytes(StandardCharsets.UTF_8)));
        record.headers().add(new RecordHeader("eventType", event.getEventType().name().getBytes(StandardCharsets.UTF_8)));
        return record;
    }
}
