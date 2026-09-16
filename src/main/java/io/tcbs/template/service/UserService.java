package io.tcbs.template.service;

public interface UserService {
    String getUserById(String userId);
    void updateUser(String userId, String newName);

    /** Cùng nghiệp vụ với getUserById nhưng tự quản lý cache bằng RedisService thay vì annotation. */
    String getUserByIdFromRedis(String userId);

    void evictUserFromRedis(String userId);
}
