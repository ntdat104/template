package io.tcbs.template.controller;

import io.tcbs.template.constants.UrlExternal;
import io.tcbs.template.dto.request.TestRequest;
import io.tcbs.template.dto.response.BaseResponse;
import io.tcbs.template.enums.ErrorCode;
import io.tcbs.template.exception.BusinessException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class TestController {

    @GetMapping(UrlExternal.TEST_REQUEST_PARAM)
    public BaseResponse<?> testRequestParam(
        @RequestParam(value = "status", required = true) String status,
        @RequestParam(value = "page", defaultValue = "0") int page,
        @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        return BaseResponse.success();
    }

    @GetMapping(UrlExternal.TEST_PATH_VARIABLE)
    public BaseResponse<?> testPathVariable(@PathVariable("id") Long id) {
        return BaseResponse.success("Lấy thông tin người dùng có ID: " + id);
    }

    @GetMapping(UrlExternal.TEST_PATH)
    public BaseResponse<?> test() {
        log.info("Hello tôi là nguyễn Tiến Đạt");
        throw new BusinessException(ErrorCode.EMAIL_REQUIRED);
        // return BaseResponse.success();
    }

    @PostMapping(UrlExternal.TEST_V2_PATH)
    public BaseResponse<?> testv2(@Valid @RequestBody TestRequest req) {
        return BaseResponse.success();
    }
}
