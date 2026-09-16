package io.tcbs.template.service.impl;

import io.tcbs.template.concurrency.DistributedSemaphoreTemplate;
import io.tcbs.template.concurrency.SemaphoreTemplate;
import io.tcbs.template.service.TestSemaphoreService;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TestSemaphoreServiceImpl implements TestSemaphoreService {

    private final SemaphoreTemplate semaphoreTemplate;
    private final DistributedSemaphoreTemplate distributedSemaphoreTemplate;

    // Ví dụ 1: Blocking call với Lambda
    @Override
    public String processOrder(String orderId) {
        return semaphoreTemplate.execute(() -> {
            // Logic xử lý đơn hàng nằm gọn trong callback này
            Thread.sleep(2000);
            return "Đã xử lý xong đơn hàng: " + orderId;
        });
    }

    // Ví dụ 2: Callback có Timeout và Fallback khi hệ thống bận
    @Override
    public String processOrderWithFallback(String orderId) {
        return semaphoreTemplate.executeWithTimeout(
            500,
            TimeUnit.MILLISECONDS,

            // Callback chính
            () -> {
                Thread.sleep(1000);
                return "Xử lý thành công cho đơn: " + orderId;
            },

            // Callback dự phòng (Fallback) khi hết permit
            () -> "Hệ thống quá tải, không thể xử lý đơn " + orderId + " lúc này!"
        );
    }

    // Ví dụ 3: Callback cho phương thức không trả về kết quả (void)
    @Override
    public void sendNotification(String userId) {
        semaphoreTemplate.executeVoid(() -> {
            // Logic gửi thông báo
            System.out.println("Đã gửi thông báo cho: " + userId);
        });
    }

    @Override
    public String placeOrder(String orderId) {
        return distributedSemaphoreTemplate.execute(
            "semaphore:trade-order-limit", // Key chia sẻ chung trên Redis
            10, // Tối đa 10 request đồng thời trên tất cả instance
            2,
            TimeUnit.SECONDS, // Chờ tối đa 2 giây

            // Success Callback
            () -> {
                // Logic đặt lệnh giao dịch
                return "Đặt lệnh thành công cho order: " + orderId;
            },

            // Fallback Callback
            () -> "Hệ thống giao dịch đang quá tải. Vui lòng thử lại sau!"
        );
    }

    // Ví dụ 5: Distributed semaphore dạng blocking - chờ vô thời hạn đến khi có permit
    @Override
    public String placeOrderBlocking(String orderId) {
        return distributedSemaphoreTemplate.execute(
            "semaphore:trade-order-blocking", // Key chia sẻ chung trên Redis
            5, // Tối đa 5 request đồng thời trên tất cả instance

            // Success Callback - không có fallback vì luôn chờ đến khi lấy được permit
            () -> {
                Thread.sleep(1000);
                return "Đặt lệnh (blocking) thành công cho order: " + orderId;
            }
        );
    }
}
