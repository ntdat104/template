package io.tcbs.template.service;

public interface TestRedisService {
    String getUserById(String userId);
    void updateUser(String userId, String newName);
    String getUserByIdFromRedis(String userId);
    void evictUserFromRedis(String userId);
}
