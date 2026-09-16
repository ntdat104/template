package io.tcbs.template.service;

public interface TestSemaphoreService {
    String processOrder(String orderId);
    String processOrderWithFallback(String orderId);
    void sendNotification(String userId);
    String placeOrder(String orderId);
    String placeOrderBlocking(String orderId);
}
