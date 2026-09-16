package io.tcbs.template.service;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Bọc các thao tác Redis hay dùng dưới dạng hàm, để service nghiệp vụ gọi trực tiếp
 * thay vì phụ thuộc vào annotation @Cacheable / @CacheEvict.
 */
public interface RedisService {
    // ----- String / object -----

    void set(String key, Object value);

    void set(String key, Object value, Duration ttl);

    /** Chỉ set khi key chưa tồn tại (SET NX PX) - dùng làm distributed lock. Trả về true nếu chiếm được key. */
    boolean setIfAbsent(String key, Object value, Duration ttl);

    <T> T get(String key, Class<T> type);

    Object get(String key);

    /** Cache-aside: lấy từ Redis, nếu chưa có thì gọi loader, ghi lại vào Redis rồi trả về. */
    <T> T getOrLoad(String key, Class<T> type, Duration ttl, Supplier<T> loader);

    long increment(String key, long delta);

    // ----- Key -----

    boolean delete(String key);

    long delete(Collection<String> keys);

    /** Xoá theo pattern (dùng SCAN, không dùng KEYS để tránh block Redis). */
    long deleteByPattern(String pattern);

    boolean hasKey(String key);

    boolean expire(String key, Duration ttl);

    /** TTL còn lại tính bằng giây: -1 là không hết hạn, -2 là key không tồn tại. */
    long getExpire(String key);

    // ----- Hash -----

    void hSet(String key, String field, Object value);

    void hSetAll(String key, Map<String, Object> values);

    <T> T hGet(String key, String field, Class<T> type);

    Map<String, Object> hGetAll(String key);

    long hDelete(String key, String... fields);

    // ----- List -----

    long lPush(String key, Object value);

    long rPush(String key, Object value);

    Object lPop(String key);

    Object rPop(String key);

    List<Object> lRange(String key, long start, long end);

    // ----- Set -----

    long sAdd(String key, Object... values);

    Set<Object> sMembers(String key);

    boolean sIsMember(String key, Object value);

    long sRemove(String key, Object... values);
}
