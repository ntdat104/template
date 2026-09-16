package io.tcbs.template.service.impl;

import io.tcbs.template.service.RedisService;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisServiceImpl implements RedisService {

    private static final int SCAN_BATCH_SIZE = 500;

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    // ----- String / object -----

    @Override
    public void set(String key, Object value) {
        redisTemplate.opsForValue().set(key, value);
    }

    @Override
    public void set(String key, Object value, Duration ttl) {
        redisTemplate.opsForValue().set(key, value, ttl);
    }

    @Override
    public boolean setIfAbsent(String key, Object value, Duration ttl) {
        return Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(key, value, ttl));
    }

    @Override
    public <T> T get(String key, Class<T> type) {
        return convert(redisTemplate.opsForValue().get(key), type);
    }

    @Override
    public Object get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    @Override
    public <T> T getOrLoad(String key, Class<T> type, Duration ttl, Supplier<T> loader) {
        T cached = get(key, type);
        if (cached != null) {
            return cached;
        }

        T loaded = loader.get();
        if (loaded != null) {
            set(key, loaded, ttl);
        }
        return loaded;
    }

    @Override
    public long increment(String key, long delta) {
        Long value = redisTemplate.opsForValue().increment(key, delta);
        return value == null ? 0L : value;
    }

    // ----- Key -----

    @Override
    public boolean delete(String key) {
        return Boolean.TRUE.equals(redisTemplate.delete(key));
    }

    @Override
    public long delete(Collection<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return 0L;
        }
        Long deleted = redisTemplate.delete(keys);
        return deleted == null ? 0L : deleted;
    }

    @Override
    public long deleteByPattern(String pattern) {
        long deleted = 0L;
        List<String> batch = new ArrayList<>(SCAN_BATCH_SIZE);
        ScanOptions options = ScanOptions.scanOptions().match(pattern).count(SCAN_BATCH_SIZE).build();

        try (Cursor<String> cursor = redisTemplate.scan(options)) {
            while (cursor.hasNext()) {
                batch.add(cursor.next());
                if (batch.size() >= SCAN_BATCH_SIZE) {
                    deleted += delete(batch);
                    batch.clear();
                }
            }
        }
        deleted += delete(batch);

        log.debug("Đã xoá {} key theo pattern {}", deleted, pattern);
        return deleted;
    }

    @Override
    public boolean hasKey(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    @Override
    public boolean expire(String key, Duration ttl) {
        return Boolean.TRUE.equals(redisTemplate.expire(key, ttl));
    }

    @Override
    public long getExpire(String key) {
        Long expire = redisTemplate.getExpire(key);
        return expire == null ? -2L : expire;
    }

    // ----- Hash -----

    @Override
    public void hSet(String key, String field, Object value) {
        redisTemplate.opsForHash().put(key, field, value);
    }

    @Override
    public void hSetAll(String key, Map<String, Object> values) {
        if (values == null || values.isEmpty()) {
            return;
        }
        redisTemplate.opsForHash().putAll(key, values);
    }

    @Override
    public <T> T hGet(String key, String field, Class<T> type) {
        return convert(redisTemplate.opsForHash().get(key, field), type);
    }

    @Override
    public Map<String, Object> hGetAll(String key) {
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);
        if (entries.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, Object> result = new LinkedHashMap<>(entries.size());
        entries.forEach((field, value) -> result.put(String.valueOf(field), value));
        return result;
    }

    @Override
    public long hDelete(String key, String... fields) {
        if (fields == null || fields.length == 0) {
            return 0L;
        }
        Long deleted = redisTemplate.opsForHash().delete(key, (Object[]) fields);
        return deleted == null ? 0L : deleted;
    }

    // ----- List -----

    @Override
    public long lPush(String key, Object value) {
        Long size = redisTemplate.opsForList().leftPush(key, value);
        return size == null ? 0L : size;
    }

    @Override
    public long rPush(String key, Object value) {
        Long size = redisTemplate.opsForList().rightPush(key, value);
        return size == null ? 0L : size;
    }

    @Override
    public Object lPop(String key) {
        return redisTemplate.opsForList().leftPop(key);
    }

    @Override
    public Object rPop(String key) {
        return redisTemplate.opsForList().rightPop(key);
    }

    @Override
    public List<Object> lRange(String key, long start, long end) {
        List<Object> values = redisTemplate.opsForList().range(key, start, end);
        return values == null ? Collections.emptyList() : values;
    }

    // ----- Set -----

    @Override
    public long sAdd(String key, Object... values) {
        Long added = redisTemplate.opsForSet().add(key, values);
        return added == null ? 0L : added;
    }

    @Override
    public Set<Object> sMembers(String key) {
        Set<Object> members = redisTemplate.opsForSet().members(key);
        return members == null ? Collections.emptySet() : members;
    }

    @Override
    public boolean sIsMember(String key, Object value) {
        return Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(key, value));
    }

    @Override
    public long sRemove(String key, Object... values) {
        Long removed = redisTemplate.opsForSet().remove(key, values);
        return removed == null ? 0L : removed;
    }

    /**
     * Value đọc từ Redis đã là object đúng kiểu khi JSON có @class, còn lại (Map, số, chuỗi...)
     * thì convert về kiểu mong muốn để hàm gọi luôn nhận đúng type.
     */
    private <T> T convert(Object value, Class<T> type) {
        if (value == null) {
            return null;
        }
        if (type.isInstance(value)) {
            return type.cast(value);
        }
        return objectMapper.convertValue(value, type);
    }
}
