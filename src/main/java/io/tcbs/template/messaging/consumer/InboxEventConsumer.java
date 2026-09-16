package io.tcbs.template.messaging.consumer;

import io.tcbs.template.constants.Topic;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class InboxEventConsumer {

    @KafkaListener(topics = Topic.TEMPLATE_EVENTS, containerFactory = "kafkaListenerContainerFactory")
    public void onEvent(ConsumerRecord<String, String> record, Acknowledgment ack) {
        log.info("[onEvent] Đã nhận event từ topic: {}", record.topic());

        var requestId = header(record, "requestId");
        var code = header(record, "code");
        var eventType = header(record, "eventType");

        var timestamp = record.timestamp();
        var topic = record.topic();
        var partition = record.partition();
        var offset = record.offset();
        var key = record.key();
        var value = record.value();

        log.info("[onEvent] requestId: {}", requestId);
        log.info("[onEvent] code: {}", code);
        log.info("[onEvent] eventType: {}", eventType);
        log.info("[onEvent] timestamp: {}", timestamp);
        log.info("[onEvent] topic: {}", topic);
        log.info("[onEvent] partition: {}", partition);
        log.info("[onEvent] offset: {}", offset);
        log.info("[onEvent] key: {}", key);
        log.info("[onEvent] value: {}", value);

        ack.acknowledge();
    }

    private String header(ConsumerRecord<String, String> record, String key) {
        Header h = record.headers().lastHeader(key);
        return h != null ? new String(h.value(), StandardCharsets.UTF_8) : null;
    }
}
