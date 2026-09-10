package io.tcbs.template.service.impl;

import io.tcbs.template.service.TestAsyncService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class TestAsyncServiceImpl implements TestAsyncService {

    @Async // Tự động dùng taskExecutor (Virtual Thread)
    @Override
    public void processAsyncTask() {
        log.info("Running on: {}", Thread.currentThread());
        // VirtualThread[#...]/runnable@ForkJoinPool...
    }

    @Async("platformThreadExecutor") // Chỉ định rõ bean platformThreadExecutor
    @Override
    public void processCpuIntensiveTask() {
        log.info("Running on: {}", Thread.currentThread());
        // Thread[task-1,5,main]
    }
}
