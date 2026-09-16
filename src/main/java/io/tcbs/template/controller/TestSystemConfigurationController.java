package io.tcbs.template.controller;

import io.tcbs.template.constants.Topic;
import io.tcbs.template.constants.UrlExternal;
import io.tcbs.template.dto.response.BaseResponse;
import io.tcbs.template.enums.EventTypeEnum;
import io.tcbs.template.mapper.SystemConfigurationMapper;
import io.tcbs.template.model.SystemConfiguration;
import io.tcbs.template.repository.SystemConfigurationRepository;
import io.tcbs.template.service.OutboxEventService;
import io.tcbs.template.util.UuidGeneratorUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class TestSystemConfigurationController {

    private final SystemConfigurationRepository systemConfigurationRepository;
    private final SystemConfigurationMapper systemConfigurationMapper;
    private final OutboxEventService outboxEventService;
    private final TransactionTemplate transactionTemplate;

    @GetMapping(UrlExternal.TEST_SYSTEM_CONFIG_PATH)
    public BaseResponse<?> getAllSystemConfig() {
        var data = systemConfigurationRepository.findAll();
        return BaseResponse.success(systemConfigurationMapper.convertToList(data));
    }

    @PostMapping(UrlExternal.TEST_SYSTEM_CONFIG_PATH)
    public BaseResponse<?> createSysTemConfig() {
        var data = new SystemConfiguration();
        data.setCode(UuidGeneratorUtil.randomUUID());
        data.setName("Test");
        data.setValue("123");
        var result = transactionTemplate.execute(c -> {
            var resultSaved = systemConfigurationRepository.save(data);
            outboxEventService.create(
                resultSaved.getCode(),
                SystemConfiguration.class.getSimpleName(),
                Topic.TEMPLATE_EVENTS,
                EventTypeEnum.SYSTEM_CONFIGURATION_CREATED,
                resultSaved
            );
            return resultSaved;
        });

        return BaseResponse.success(systemConfigurationMapper.convert(result));
    }
}
