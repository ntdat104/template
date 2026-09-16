package io.tcbs.template.config;

import io.tcbs.template.constants.Constant;
import io.tcbs.template.constants.Topic;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.ContainerProperties.AckMode;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.util.backoff.ExponentialBackOff;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class KafkaConfiguration {

    private static final String ACKS_CONFIG = "all";
    private static final int BATCH_SIZE_CONFIG = 64 * 1024;
    private static final String COMPRESSION_TYPE_CONFIG = "lz4";
    private static final int LINGER_MS_CONFIG = 10;
    private static final int MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION = 5;
    private static final boolean ENABLE_IDEMPOTENCE_CONFIG = true;
    private static final int DELIVERY_TIMEOUT_MS_CONFIG = 120_000;

    private static final String GROUP_ID_CONFIG = Constant.SERVICE_CODE;
    private static final String AUTO_OFFSET_RESET_CONFIG = "earliest";
    private static final int MAX_POLL_RECORDS_CONFIG = 200;
    private static final boolean ENABLE_AUTO_COMMIT_CONFIG = false;
    private static final int MAX_POLL_INTERVAL_MS_CONFIG = 300_000;

    private static final long BACKOFF_INITIAL_INTERVAL_MS = 500L;
    private static final double BACKOFF_MULTIPLIER = 2.0;
    private static final long BACKOFF_MAX_INTERVAL_MS = 10_000L;
    private static final long BACKOFF_MAX_ELAPSED_TIME_MS = 60_000L;

    private final KafkaProperties kafkaProperties;

    @Value("${application.topic.partitions:3}")
    private int partitions;

    @Bean
    NewTopic templateEventsTopic() {
        return TopicBuilder.name(Topic.TEMPLATE_EVENTS).partitions(partitions).replicas(1).build();
    }

    @Bean
    ProducerFactory<String, String> producerFactory() {
        Map<String, Object> props = new HashMap<>(kafkaProperties.buildProducerProperties());
        props.put(ProducerConfig.ACKS_CONFIG, ACKS_CONFIG);
        props.put(ProducerConfig.BATCH_SIZE_CONFIG, BATCH_SIZE_CONFIG);
        props.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, COMPRESSION_TYPE_CONFIG);
        props.put(ProducerConfig.LINGER_MS_CONFIG, LINGER_MS_CONFIG);
        props.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION);
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, ENABLE_IDEMPOTENCE_CONFIG);
        props.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, DELIVERY_TIMEOUT_MS_CONFIG);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    KafkaTemplate<String, String> kafkaTemplate(ProducerFactory<String, String> pf) {
        KafkaTemplate<String, String> template = new KafkaTemplate<>(pf);
        template.setObservationEnabled(false);
        return template;
    }

    @Bean
    ConsumerFactory<String, String> consumerFactory() {
        Map<String, Object> props = new HashMap<>(kafkaProperties.buildConsumerProperties());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, GROUP_ID_CONFIG);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, AUTO_OFFSET_RESET_CONFIG);
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, MAX_POLL_RECORDS_CONFIG);
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, ENABLE_AUTO_COMMIT_CONFIG);
        props.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, MAX_POLL_INTERVAL_MS_CONFIG);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        props.put(ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS, StringDeserializer.class);
        props.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, StringDeserializer.class);
        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory(
        ConsumerFactory<String, String> cf,
        DefaultErrorHandler errorHandler
    ) {
        ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(cf);
        factory.setConcurrency(partitions);
        factory.getContainerProperties().setObservationEnabled(true);
        factory.getContainerProperties().setAckMode(AckMode.MANUAL_IMMEDIATE);
        factory.setCommonErrorHandler(errorHandler);
        return factory;
    }

    @Bean
    DefaultErrorHandler errorHandler() {
        ExponentialBackOff backOff = new ExponentialBackOff(BACKOFF_INITIAL_INTERVAL_MS, BACKOFF_MULTIPLIER);
        backOff.setMaxInterval(BACKOFF_MAX_INTERVAL_MS);
        backOff.setMaxElapsedTime(BACKOFF_MAX_ELAPSED_TIME_MS);

        DefaultErrorHandler handler = new DefaultErrorHandler(backOff);

        handler.setRetryListeners((record, ex, deliveryAttempt) ->
            log.warn(
                "[Kafka Retry #{}] topic={} partition={} offset={} key={} cause={}",
                deliveryAttempt,
                record.topic(),
                record.partition(),
                record.offset(),
                record.key(),
                ex.getMessage()
            )
        );

        return handler;
    }
}
