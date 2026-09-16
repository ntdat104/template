package io.tcbs.template.model;

import io.tcbs.template.enums.EventTypeEnum;
import io.tcbs.template.enums.OutboxStatus;
import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@ToString
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "outbox_events", indexes = { @Index(columnList = "status"), @Index(columnList = "aggregate_code") })
public class OutboxEvent extends AbstractAuditingEntity<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, unique = true)
    private String code;

    @Column(name = "request_id", nullable = false, unique = true)
    private String requestId;

    @Column(name = "aggregate_code", nullable = false)
    private String aggregateCode;

    @Column(name = "aggregate_type", nullable = false)
    private String aggregateType;

    @Column(name = "topic", nullable = false)
    private String topic;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    private EventTypeEnum eventType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", columnDefinition = "jsonb", nullable = false)
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private OutboxStatus status;

    @Column(name = "published_at")
    private Instant publishedAt;
}
