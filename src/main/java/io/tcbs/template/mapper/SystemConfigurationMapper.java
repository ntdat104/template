package io.tcbs.template.mapper;

import io.tcbs.template.dto.response.SystemConfigurationResponse;
import io.tcbs.template.model.SystemConfiguration;
import io.tcbs.template.util.DatetimeUtil;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class SystemConfigurationMapper extends AbstractMapper<SystemConfiguration, SystemConfigurationResponse> {

    @Override
    public SystemConfigurationResponse convert(SystemConfiguration source, Map<String, Object> parameters) {
        if (source == null) {
            return null;
        }

        var target = new SystemConfigurationResponse();
        target.setId(source.getId());
        target.setCode(source.getCode());
        target.setName(source.getName());
        target.setValue(source.getValue());

        target.setCreatedDateMs(DatetimeUtil.toEpochMilli(source.getCreatedDate()));
        target.setCreatedDate(DatetimeUtil.format(source.getCreatedDate()));
        target.setCreatedDateAgo(DatetimeUtil.toTimeAgo(source.getCreatedDate()));
        target.setLastModifiedDateMs(DatetimeUtil.toEpochMilli(source.getLastModifiedDate()));
        target.setLastModifiedDate(DatetimeUtil.format(source.getLastModifiedDate()));
        target.setLastModifiedDateAgo(DatetimeUtil.toTimeAgo(source.getLastModifiedDate()));

        return target;
    }
}
