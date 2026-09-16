package io.tcbs.template.controller;

import io.tcbs.template.constants.UrlExternal;
import io.tcbs.template.dto.response.BaseResponse;
import io.tcbs.template.service.TestRedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class TestRedisCacheController {

    private final TestRedisService testRedisService;

    @GetMapping(UrlExternal.TEST_REDIS_PATH)
    public BaseResponse<?> getUser(@PathVariable("id") String id) {
        long start = System.currentTimeMillis();
        String result = testRedisService.getUserById(id);
        long duration = System.currentTimeMillis() - start;
        var response = result + " (Thời gian xử lý: " + duration + " ms)";
        return BaseResponse.success(response);
    }

    @GetMapping(UrlExternal.TEST_REDIS_MANUAL_PATH)
    public BaseResponse<?> getUserFromRedis(@PathVariable("id") String id) {
        long start = System.currentTimeMillis();
        String result = testRedisService.getUserByIdFromRedis(id);
        long duration = System.currentTimeMillis() - start;
        var response = result + " (Thời gian xử lý: " + duration + " ms)";
        return BaseResponse.success(response);
    }

    @DeleteMapping(UrlExternal.TEST_REDIS_MANUAL_PATH)
    public BaseResponse<?> evictUserFromRedis(@PathVariable("id") String id) {
        testRedisService.evictUserFromRedis(id);
        var response = "Đã xóa key redis cho userId: " + id;
        return BaseResponse.success(response);
    }

    @DeleteMapping(UrlExternal.TEST_REDIS_PATH)
    public BaseResponse<?> evictUser(@PathVariable("id") String id) {
        testRedisService.updateUser(id, "Updated Name");
        var response = "Đã xóa cache cho userId: " + id;
        return BaseResponse.success(response);
    }
}
