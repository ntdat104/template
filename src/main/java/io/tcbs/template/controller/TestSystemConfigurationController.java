package io.tcbs.template.controller;

import io.tcbs.template.constants.UrlExternal;
import io.tcbs.template.dto.response.BaseResponse;
import io.tcbs.template.mapper.SystemConfigurationMapper;
import io.tcbs.template.model.SystemConfiguration;
import io.tcbs.template.repository.SystemConfigurationRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class TestSystemConfigurationController {

    private final SystemConfigurationRepository systemConfigurationRepository;
    private final SystemConfigurationMapper systemConfigurationMapper;

    @GetMapping(UrlExternal.TEST_SYSTEM_CONFIG_PATH)
    public BaseResponse<?> getAllSystemConfig() {
        var data = systemConfigurationRepository.findAll();
        return BaseResponse.success(systemConfigurationMapper.convertToList(data));
    }

    @PostMapping(UrlExternal.TEST_SYSTEM_CONFIG_PATH)
    public BaseResponse<?> createSysTemConfig() {
        var data = new SystemConfiguration();
        data.setCode(UUID.randomUUID().toString());
        data.setName("Test");
        data.setValue("123");
        var result = systemConfigurationRepository.save(data);
        return BaseResponse.success(systemConfigurationMapper.convert(result));
    }
}
