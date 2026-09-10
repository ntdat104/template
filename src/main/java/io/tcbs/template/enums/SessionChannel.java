package io.tcbs.template.enums;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Getter;

@Getter
public enum SessionChannel {
    MOBILE(1, "MOBILE"),
    WEB(2, "WEB"),
    API(3, "API"),
    SDK(4, "SDK");

    private final Integer id;
    private final String value;

    SessionChannel(Integer id, String value) {
        this.id = id;
        this.value = value;
    }

    // --- Generic map builder ---
    private static <K> Map<K, SessionChannel> buildLookupMap(Function<SessionChannel, K> keyExtractor) {
        return Stream.of(values()).collect(Collectors.toUnmodifiableMap(keyExtractor, Function.identity()));
    }

    // --- Static immutable lookup maps ---
    private static final Map<Integer, SessionChannel> ID_MAP = buildLookupMap(SessionChannel::getId);
    private static final Map<String, SessionChannel> VALUE_MAP = buildLookupMap(SessionChannel::getValue);

    // --- Lookup methods ---
    public static SessionChannel findById(Integer id, SessionChannel defaultValue) {
        return ID_MAP.getOrDefault(id, defaultValue);
    }

    public static SessionChannel findById(Integer id) {
        return findById(id, null);
    }

    public static SessionChannel findByValue(String value, SessionChannel defaultValue) {
        return VALUE_MAP.getOrDefault(value, defaultValue);
    }

    public static SessionChannel findByValue(String value) {
        return findByValue(value, null);
    }
}
