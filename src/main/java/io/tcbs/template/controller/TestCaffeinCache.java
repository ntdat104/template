package io.tcbs.template.controller;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.tcbs.template.constants.UrlExternal;
import io.tcbs.template.dto.response.BaseResponse;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestCaffeinCache {

    private final Cache<String, String> cache = Caffeine.newBuilder()
        .expireAfterWrite(10, TimeUnit.MINUTES) // Hết hạn sau 10 phút kể từ khi ghi
        .maximumSize(1_000) // Chứa tối đa 1,000 phần tử
        .build();

    @GetMapping(UrlExternal.TEST_CAFFEIN_CACHE_PUT)
    public BaseResponse<?> testCaffeinCachePut() {
        cache.put("user_101", "Nguyễn Văn A");
        return BaseResponse.success();
    }

    @GetMapping(UrlExternal.TEST_CAFFEIN_CACHE_GET_IF_PRESENT)
    public BaseResponse<?> testCaffeinCacheGetIfPresent() {
        // 2. Lấy dữ liệu (Get) - Trả về null nếu không tìm thấy
        String name = cache.getIfPresent("user_101");
        System.out.println("User: " + name);

        if (StringUtils.isBlank(name)) {
            try {
                // Giả lập DB xử lý mất 3 giây
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return BaseResponse.success(name);
        }

        return BaseResponse.success(name);
    }

    @GetMapping(UrlExternal.TEST_CAFFEIN_CACHE_GET)
    public BaseResponse<?> testCaffeinCacheGet() {
        // 3. Get with Fallback (Rất hay dùng): Lấy từ cache, nếu không có thì gọi hàm để load
        String name2 = cache.get("user_102", key -> fetchUserFromDB(key));
        System.out.println("User 102: " + name2);
        return BaseResponse.success(name2);
    }

    @GetMapping(UrlExternal.TEST_CAFFEIN_CACHE_INVALIDATE)
    public BaseResponse<?> testCaffeinCacheInvalidate() {
        cache.invalidate("user_101"); // Xóa 1 key
        cache.invalidateAll(); // Xóa toàn bộ
        return BaseResponse.success();
    }

    private static String fetchUserFromDB(String userId) {
        System.out.println("--> Fetching from DB for: " + userId);
        return "Trần Thị B";
    }
}
