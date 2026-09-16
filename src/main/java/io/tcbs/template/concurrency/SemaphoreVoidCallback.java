package io.tcbs.template.concurrency;

@FunctionalInterface
public interface SemaphoreVoidCallback {
    void doInSemaphore() throws Exception;
}
