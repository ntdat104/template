package io.tcbs.template.concurrency;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class SemaphoreTemplate {

    private final Semaphore semaphore;

    public SemaphoreTemplate(int permits, boolean fair) {
        this.semaphore = new Semaphore(permits, fair);
    }

    // Chờ vô thời hạn cho đến khi lấy được permit
    public <T> T execute(SemaphoreCallback<T> action) {
        try {
            semaphore.acquire();
            return action.doInSemaphore();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Thread bị ngắt khi đang chờ Semaphore", e);
        } catch (Exception e) {
            throw new RuntimeException("Lỗi xảy ra trong Callback", e);
        } finally {
            semaphore.release();
        }
    }

    // Thử lấy permit với thời gian Timeout (Tránh treo Thread)
    public <T> T executeWithTimeout(long timeout, TimeUnit unit, SemaphoreCallback<T> action, SemaphoreCallback<T> fallback) {
        boolean acquired = false;
        try {
            acquired = semaphore.tryAcquire(timeout, unit);
            if (acquired) {
                return action.doInSemaphore();
            } else {
                // Xử lý khi không lấy được permit (Fallback)
                return fallback.doInSemaphore();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Thread bị ngắt khi đang chờ Semaphore", e);
        } catch (Exception e) {
            throw new RuntimeException("Lỗi xảy ra trong Callback", e);
        } finally {
            if (acquired) {
                semaphore.release();
            }
        }
    }

    // Phương thức tiện ích cho các hàm Không có giá trị trả về (void)
    public void executeVoid(SemaphoreVoidCallback action) {
        execute(() -> {
            action.doInSemaphore();
            return null;
        });
    }
}
