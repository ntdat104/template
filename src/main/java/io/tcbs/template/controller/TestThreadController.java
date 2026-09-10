package io.tcbs.template.controller;

import io.tcbs.template.constants.UrlExternal;
import io.tcbs.template.dto.response.BaseResponse;
import io.tcbs.template.service.TestAsyncService;
import java.text.MessageFormat;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class TestThreadController {

    private final TestAsyncService testAsyncService;

    @GetMapping(UrlExternal.TEST_THREAD)
    public BaseResponse<?> testThread() {
        Thread thread = Thread.currentThread();
        var data = MessageFormat.format("Thread info: {0} | Is Virtual: {1}", thread.toString(), thread.isVirtual());
        return BaseResponse.success(data);
    }

    @GetMapping(UrlExternal.TEST_ASYNC_VIRTUAL_THREAD)
    public BaseResponse<?> testAsyncVirtualThread() {
        testAsyncService.processAsyncTask();
        return BaseResponse.success();
    }

    @GetMapping(UrlExternal.TEST_ASYNC_PLATFORM_THREAD)
    public BaseResponse<?> testAsyncPlatformThread() {
        testAsyncService.processCpuIntensiveTask();
        return BaseResponse.success();
    }
}
