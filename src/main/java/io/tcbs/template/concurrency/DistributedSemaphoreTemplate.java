package io.tcbs.template.concurrency;

import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RSemaphore;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DistributedSemaphoreTemplate {

    private final RedissonClient redissonClient;

    /**
     * Thực thi tác vụ Blocking: Chờ VÔ THỜI HẠN cho đến khi lấy được permit.
     * Không cần timeout hay fallback.
     *
     * @param semaphoreKey Tên key duy nhất trên Redis đại diện cho tài nguyên
     * @param maxPermits   Số lượng permit tối đa cho TOÀN HỆ THỐNG
     * @param action       Callback xử lý chính
     */
    public <T> T execute(String semaphoreKey, int maxPermits, SemaphoreCallback<T> action) {
        RSemaphore semaphore = redissonClient.getSemaphore(semaphoreKey);

        // Khởi tạo số lượng permit trên Redis nếu key chưa tồn tại
        semaphore.trySetPermits(maxPermits);

        boolean acquired = false;
        try {
            // Chặn Thread (Block) cho đến khi lấy được permit thành công
            semaphore.acquire();
            acquired = true;
            return action.doInSemaphore();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Thread bị ngắt khi đang chờ Semaphore cho key: {}", semaphoreKey, e);
            throw new RuntimeException("Thread interrupted during semaphore waiting", e);
        } catch (Exception e) {
            log.error("Lỗi xảy ra trong Callback của Semaphore key: {}", semaphoreKey, e);
            throw new RuntimeException("Execution error inside semaphore callback", e);
        } finally {
            // Chỉ xả permit khi thực sự đã chiếm được, tránh làm phình số permit trên Redis
            if (acquired) {
                semaphore.release();
            }
        }
    }

    /**
     * Thực thi tác vụ được giới hạn đồng thời bởi Redisson Distributed Semaphore.
     *
     * @param semaphoreKey Tên key duy nhất trên Redis đại diện cho tài nguyên
     * @param maxPermits   Số lượng permit tối đa cho TOÀN HỆ THỐNG
     * @param timeout      Thời gian tối đa chờ lấy permit
     * @param unit         Đơn vị thời gian
     * @param action       Callback xử lý chính
     * @param fallback     Callback dự phòng khi không lấy được permit (Timeout/Hệ thống quá tải)
     */
    public <T> T execute(
        String semaphoreKey,
        int maxPermits,
        long timeout,
        TimeUnit unit,
        SemaphoreCallback<T> action,
        SemaphoreCallback<T> fallback
    ) {
        RSemaphore semaphore = redissonClient.getSemaphore(semaphoreKey);

        // Khởi tạo số lượng permit trên Redis nếu key chưa tồn tại
        semaphore.trySetPermits(maxPermits);

        boolean acquired = false;
        try {
            acquired = semaphore.tryAcquire(timeout, unit);
            if (acquired) {
                return action.doInSemaphore();
            } else {
                log.warn("Lấy Distributed Semaphore thất bại cho key: {}. Chuyển sang Fallback", semaphoreKey);
                return fallback.doInSemaphore();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Thread bị ngắt khi chờ Semaphore cho key: {}", semaphoreKey, e);
            throw new RuntimeException("Thread interrupted during semaphore waiting", e);
        } catch (Exception e) {
            log.error("Lỗi xảy ra trong Callback của Semaphore key: {}", semaphoreKey, e);
            throw new RuntimeException("Execution error inside semaphore callback", e);
        } finally {
            if (acquired) {
                semaphore.release();
            }
        }
    }
}
