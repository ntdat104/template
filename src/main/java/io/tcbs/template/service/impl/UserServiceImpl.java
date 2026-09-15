package io.tcbs.template.service.impl;

import io.tcbs.template.service.UserService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {

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

    private void simulateSlowService() {
        try {
            Thread.sleep(3000); // Giả lập chờ 3 giây
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
