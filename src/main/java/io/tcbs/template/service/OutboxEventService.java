package io.tcbs.template.service;

import io.tcbs.template.enums.EventTypeEnum;

public interface OutboxEventService {
    void create(String aggregateCode, String aggregateType, String topic, EventTypeEnum eventType, Object payload);
}
