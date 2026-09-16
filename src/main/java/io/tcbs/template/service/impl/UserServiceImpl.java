package io.tcbs.template.service.impl;

import io.tcbs.template.service.RedisService;
import io.tcbs.template.service.UserService;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private static final String USER_KEY_PREFIX = "users:";
    private static final Duration USER_TTL = Duration.ofMinutes(10);

    private final RedisService redisService;

    // Kết quả của hàm này sẽ được cache lại trong cache key/namespace "users" với key là userId
    @Override
    @Cacheable(value = "users", key = "#userId")
    public String getUserById(String userId) {
        // Giả lập tác vụ nặng hoặc query DB
        simulateSlowService();
        return "User details for ID: " + userId;
    }

    // Khi xóa/cập nhật user, xóa bản ghi cache tương ứng
    @Override
    @CacheEvict(value = "users", key = "#userId")
    public void updateUser(String userId, String newName) {
        // Xử lý cập nhật thông tin trong DB...
    }

    /**
     * Cách 2: gọi Redis dưới dạng hàm - chủ động về key, TTL và thời điểm ghi cache.
     * Hữu ích khi key phụ thuộc logic runtime, hoặc khi cần cache bên trong một hàm (self-invocation
     * không kích hoạt được @Cacheable vì không đi qua proxy của Spring).
     */
    @Override
    public String getUserByIdFromRedis(String userId) {
        return redisService.getOrLoad(USER_KEY_PREFIX + userId, String.class, USER_TTL, () -> {
            simulateSlowService();
            return "User details for ID: " + userId;
        });
    }

    @Override
    public void evictUserFromRedis(String userId) {
        redisService.delete(USER_KEY_PREFIX + userId);
    }

    private void simulateSlowService() {
        try {
            Thread.sleep(3000); // Giả lập chờ 3 giây
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
