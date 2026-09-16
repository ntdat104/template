package io.tcbs.template.service.impl;

import io.tcbs.template.enums.EventTypeEnum;
import io.tcbs.template.enums.OutboxStatus;
import io.tcbs.template.messaging.publisher.OutboxEventPublisher;
import io.tcbs.template.model.OutboxEvent;
import io.tcbs.template.repository.OutboxEventRepository;
import io.tcbs.template.service.OutboxEventService;
import io.tcbs.template.util.ContextUtil;
import io.tcbs.template.util.UuidGeneratorUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxEventServiceImpl implements OutboxEventService {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;
    private final OutboxEventPublisher outboxEventPublisher;

    @Override
    public void create(String aggregateCode, String aggregateType, String topic, EventTypeEnum eventType, Object payload) {
        var event = new OutboxEvent();
        event.setCode(UuidGeneratorUtil.randomUUID());
        event.setRequestId(ContextUtil.getRequestId());
        event.setAggregateCode(aggregateCode);
        event.setAggregateType(aggregateType);
        event.setTopic(topic);
        event.setEventType(eventType);
        event.setPayload(convertPayloadToString(payload));
        event.setStatus(OutboxStatus.PENDING);

        var eventSaved = outboxEventRepository.save(event);

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        outboxEventPublisher.send(eventSaved);
                    }
                }
            );
        } else {
            outboxEventPublisher.send(eventSaved);
        }
    }

    private String convertPayloadToString(Object payload) {
        if (payload == null) {
            return null;
        }
        if (payload instanceof String stringPayload) {
            return stringPayload; // Tránh serialize lại nếu input đã là String
        }
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JacksonException e) {
            log.error("Lỗi khi convert payload object sang String JSON", e);
            throw new IllegalArgumentException("Cannot serialize payload to JSON String", e);
        }
    }
}
