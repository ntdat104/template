package io.tcbs.template.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import io.tcbs.template.client.RandomUserServiceClient;
import io.tcbs.template.constants.UrlExternal;
import io.tcbs.template.dto.response.BaseResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j 
@RestController
@RequiredArgsConstructor
public class TestFeignClient {

    private final RandomUserServiceClient randomUserServiceClient;
    
    @GetMapping(UrlExternal.TEST_RANDOMUSER_GENERATE)
    public BaseResponse<?> testGetRandomUserGenerate() {
        return BaseResponse.success(randomUserServiceClient.generate());
    }

}
