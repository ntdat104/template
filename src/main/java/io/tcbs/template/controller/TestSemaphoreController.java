package io.tcbs.template.controller;

import io.tcbs.template.concurrency.DistributedSemaphoreTemplate;
import io.tcbs.template.concurrency.SemaphoreTemplate;
import io.tcbs.template.constants.UrlExternal;
import io.tcbs.template.dto.response.BaseResponse;
import io.tcbs.template.service.TestSemaphoreService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.IntFunction;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Các endpoint dùng để test Semaphore của JVM (SemaphoreTemplate) và Distributed Semaphore trên
 * Redis (DistributedSemaphoreTemplate).
 */
@RestController
@RequiredArgsConstructor
public class TestSemaphoreController {

    private final TestSemaphoreService testSemaphoreService;
    private final SemaphoreTemplate semaphoreTemplate;
    private final DistributedSemaphoreTemplate distributedSemaphoreTemplate;

    /** Local semaphore - blocking: chờ vô thời hạn đến khi có permit (3 permit, mỗi lượt ~2s). */
    @GetMapping(UrlExternal.TEST_SEMAPHORE_LOCAL_PATH)
    public BaseResponse<?> localBlocking(@PathVariable String orderId) {
        long start = System.currentTimeMillis();
        String result = testSemaphoreService.processOrder(orderId);
        return BaseResponse.success(describe(result, start));
    }

    /** Local semaphore - timeout 500ms, hết permit thì trả về fallback thay vì chờ. */
    @GetMapping(UrlExternal.TEST_SEMAPHORE_LOCAL_FALLBACK_PATH)
    public BaseResponse<?> localWithFallback(@PathVariable String orderId) {
        long start = System.currentTimeMillis();
        String result = testSemaphoreService.processOrderWithFallback(orderId);
        return BaseResponse.success(describe(result, start));
    }

    /** Local semaphore - callback không có giá trị trả về (void). */
    @GetMapping(UrlExternal.TEST_SEMAPHORE_LOCAL_NOTIFY_PATH)
    public BaseResponse<?> localNotify(@PathVariable String userId) {
        long start = System.currentTimeMillis();
        testSemaphoreService.sendNotification(userId);
        return BaseResponse.success(describe("Đã gửi thông báo cho: " + userId, start));
    }

    /** Distributed semaphore - đặt lệnh qua permit dùng chung trên Redis. */
    @GetMapping(UrlExternal.TEST_SEMAPHORE_DISTRIBUTED_PATH)
    public BaseResponse<?> distributed(@PathVariable String orderId) {
        long start = System.currentTimeMillis();
        String result = testSemaphoreService.placeOrder(orderId);
        return BaseResponse.success(describe(result, start));
    }

    /** Distributed semaphore - blocking: chờ vô thời hạn đến khi có permit, không có fallback. */
    @GetMapping(UrlExternal.TEST_SEMAPHORE_DISTRIBUTED_BLOCKING_PATH)
    public BaseResponse<?> distributedBlocking(@PathVariable String orderId) {
        long start = System.currentTimeMillis();
        String result = testSemaphoreService.placeOrderBlocking(orderId);
        return BaseResponse.success(describe(result, start));
    }

    /**
     * Bắn đồng thời nhiều request vào SemaphoreTemplate (3 permit) trong cùng 1 lần gọi HTTP, vì
     * file .http chạy tuần tự nên không tự tạo được tải đồng thời.
     */
    @GetMapping(UrlExternal.TEST_SEMAPHORE_LOCAL_BURST_PATH)
    public BaseResponse<?> localBurst(
        @RequestParam(defaultValue = "6") int concurrency,
        @RequestParam(defaultValue = "1000") long holdMillis,
        @RequestParam(defaultValue = "500") long timeoutMillis
    ) {
        return BaseResponse.success(
            runBurst(concurrency, index ->
                semaphoreTemplate.executeWithTimeout(
                    timeoutMillis,
                    TimeUnit.MILLISECONDS,
                    () -> {
                        Thread.sleep(holdMillis);
                        return "ACQUIRED - task " + index;
                    },
                    () -> "FALLBACK - task " + index + " không lấy được permit trong " + timeoutMillis + "ms"
                )
            )
        );
    }

    /**
     * Bắn đồng thời nhiều request vào DistributedSemaphoreTemplate. Chạy nhiều instance ứng dụng
     * cùng lúc sẽ thấy tổng số permit được chia sẻ qua Redis chứ không tính riêng từng instance.
     */
    @GetMapping(UrlExternal.TEST_SEMAPHORE_DISTRIBUTED_BURST_PATH)
    public BaseResponse<?> distributedBurst(
        @RequestParam(defaultValue = "semaphore:test-burst") String key,
        @RequestParam(defaultValue = "3") int permits,
        @RequestParam(defaultValue = "6") int concurrency,
        @RequestParam(defaultValue = "1000") long holdMillis,
        @RequestParam(defaultValue = "500") long timeoutMillis
    ) {
        return BaseResponse.success(
            runBurst(concurrency, index ->
                distributedSemaphoreTemplate.execute(
                    key,
                    permits,
                    timeoutMillis,
                    TimeUnit.MILLISECONDS,
                    () -> {
                        Thread.sleep(holdMillis);
                        return "ACQUIRED - task " + index;
                    },
                    () -> "FALLBACK - task " + index + " không lấy được permit trong " + timeoutMillis + "ms"
                )
            )
        );
    }

    /**
     * Bắn đồng thời nhiều request vào DistributedSemaphoreTemplate ở chế độ blocking. Không có
     * task nào bị loại, chúng xếp hàng theo từng lượt nên totalMillis ~ ceil(concurrency/permits)
     * * holdMillis.
     */
    @GetMapping(UrlExternal.TEST_SEMAPHORE_DISTRIBUTED_BLOCKING_BURST_PATH)
    public BaseResponse<?> distributedBlockingBurst(
        @RequestParam(defaultValue = "semaphore:test-blocking-burst") String key,
        @RequestParam(defaultValue = "2") int permits,
        @RequestParam(defaultValue = "6") int concurrency,
        @RequestParam(defaultValue = "500") long holdMillis
    ) {
        return BaseResponse.success(
            runBurst(concurrency, index ->
                distributedSemaphoreTemplate.execute(key, permits, () -> {
                    Thread.sleep(holdMillis);
                    return "ACQUIRED - task " + index;
                })
            )
        );
    }

    private Map<String, Object> runBurst(int concurrency, IntFunction<String> task) {
        long start = System.currentTimeMillis();
        List<Map<String, Object>> tasks = new ArrayList<>();

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<CompletableFuture<Map<String, Object>>> futures = new ArrayList<>();
            for (int i = 1; i <= concurrency; i++) {
                final int index = i;
                futures.add(
                    CompletableFuture.supplyAsync(() -> {
                        long taskStart = System.currentTimeMillis();
                        Map<String, Object> item = new LinkedHashMap<>();
                        item.put("task", index);
                        try {
                            item.put("result", task.apply(index));
                        } catch (Exception e) {
                            item.put("result", "ERROR - " + e.getMessage());
                        }
                        item.put("elapsedMillis", System.currentTimeMillis() - taskStart);
                        return item;
                    }, executor)
                );
            }
            futures.forEach(future -> tasks.add(future.join()));
        }

        long acquired = tasks
            .stream()
            .filter(item -> String.valueOf(item.get("result")).startsWith("ACQUIRED"))
            .count();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("concurrency", concurrency);
        result.put("acquired", acquired);
        result.put("fallback", concurrency - acquired);
        result.put("totalMillis", System.currentTimeMillis() - start);
        result.put("tasks", tasks);
        return result;
    }

    private Map<String, Object> describe(String result, long start) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("result", result);
        data.put("elapsedMillis", System.currentTimeMillis() - start);
        data.put("thread", Thread.currentThread().toString());
        return data;
    }
}
