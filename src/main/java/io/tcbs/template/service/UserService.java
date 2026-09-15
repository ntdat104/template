package io.tcbs.template.service;

public interface UserService {
    String getUserById(String userId);
    void updateUser(String userId, String newName);
}
