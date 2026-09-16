package io.tcbs.template.controller;

import io.tcbs.template.constants.UrlExternal;
import io.tcbs.template.dto.response.BaseResponse;
import io.tcbs.template.service.RedisService;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class TestRedisController {

    private final RedisService redisService;

    @GetMapping(UrlExternal.TEST_REDIS_DEMO_PATH)
    public BaseResponse<?> demo() {
        Map<String, Object> result = new LinkedHashMap<>();

        // String / object kèm TTL
        redisService.set("demo:string", "Nguyễn Văn A", Duration.ofMinutes(5));
        result.put("string", redisService.get("demo:string", String.class));
        result.put("ttlSeconds", redisService.getExpire("demo:string"));

        // Counter
        redisService.delete("demo:counter");
        redisService.increment("demo:counter", 1);
        result.put("counter", redisService.increment("demo:counter", 2));

        // Hash
        redisService.hSet("demo:hash", "name", "Trần Thị B");
        redisService.hSet("demo:hash", "age", 30);
        result.put("hash", redisService.hGetAll("demo:hash"));

        // List
        redisService.delete("demo:list");
        redisService.rPush("demo:list", "job-1");
        redisService.rPush("demo:list", "job-2");
        result.put("list", redisService.lRange("demo:list", 0, -1));
        result.put("popped", redisService.lPop("demo:list"));

        // Set
        redisService.delete("demo:set");
        redisService.sAdd("demo:set", "a", "b", "a");
        result.put("set", redisService.sMembers("demo:set"));

        // Distributed lock: chỉ request đầu tiên chiếm được key
        boolean locked = redisService.setIfAbsent("demo:lock", "owner-1", Duration.ofSeconds(30));
        result.put("acquiredLock", locked);

        return BaseResponse.success(result);
    }
}
