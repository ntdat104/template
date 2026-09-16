package io.tcbs.template.controller;

import io.tcbs.template.constants.UrlExternal;
import io.tcbs.template.dto.response.BaseResponse;
import io.tcbs.template.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping(UrlExternal.TEST_REDIS_PATH)
    public BaseResponse<?> getUser(@PathVariable("id") String id) {
        long start = System.currentTimeMillis();
        String result = userService.getUserById(id);
        long duration = System.currentTimeMillis() - start;
        var response = result + " (Thời gian xử lý: " + duration + " ms)";
        return BaseResponse.success(response);
    }

    @GetMapping(UrlExternal.TEST_REDIS_MANUAL_PATH)
    public BaseResponse<?> getUserFromRedis(@PathVariable("id") String id) {
        long start = System.currentTimeMillis();
        String result = userService.getUserByIdFromRedis(id);
        long duration = System.currentTimeMillis() - start;
        var response = result + " (Thời gian xử lý: " + duration + " ms)";
        return BaseResponse.success(response);
    }

    @DeleteMapping(UrlExternal.TEST_REDIS_MANUAL_PATH)
    public BaseResponse<?> evictUserFromRedis(@PathVariable("id") String id) {
        userService.evictUserFromRedis(id);
        var response = "Đã xóa key redis cho userId: " + id;
        return BaseResponse.success(response);
    }

    @DeleteMapping(UrlExternal.TEST_REDIS_PATH)
    public BaseResponse<?> evictUser(@PathVariable("id") String id) {
        userService.updateUser(id, "Updated Name");
        var response = "Đã xóa cache cho userId: " + id;
        return BaseResponse.success(response);
    }
}
