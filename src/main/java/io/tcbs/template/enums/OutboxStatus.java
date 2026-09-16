package io.tcbs.template.enums;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Getter;

@Getter
public enum OutboxStatus {
    PENDING(1, "PENDING"),
    SENDING(2, "SENDING"),
    PUBLISHED(3, "PUBLISHED"),
    FAILED(4, "FAILED");

    private final Integer id;
    private final String value;

    OutboxStatus(Integer id, String value) {
        this.id = id;
        this.value = value;
    }

    private static <K> Map<K, OutboxStatus> buildLookupMap(Function<OutboxStatus, K> keyExtractor) {
        return Stream.of(values()).collect(Collectors.toUnmodifiableMap(keyExtractor, Function.identity()));
    }

    private static final Map<Integer, OutboxStatus> ID_MAP = buildLookupMap(OutboxStatus::getId);
    private static final Map<String, OutboxStatus> VALUE_MAP = buildLookupMap(OutboxStatus::getValue);

    public static OutboxStatus findById(Integer id, OutboxStatus defaultValue) {
        return ID_MAP.getOrDefault(id, defaultValue);
    }

    public static OutboxStatus findById(Integer id) {
        return findById(id, null);
    }

    public static OutboxStatus findByValue(String value, OutboxStatus defaultValue) {
        return VALUE_MAP.getOrDefault(value, defaultValue);
    }

    public static OutboxStatus findByValue(String value) {
        return findByValue(value, null);
    }
}
