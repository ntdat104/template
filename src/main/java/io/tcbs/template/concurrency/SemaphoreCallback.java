package io.tcbs.template.concurrency;

@FunctionalInterface
public interface SemaphoreCallback<T> {
    T doInSemaphore() throws Exception;
}
