package io.tcbs.template.dto.request;

import io.tcbs.template.annotation.ValidEnum;
import io.tcbs.template.enums.SessionChannel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Map;

public class TestRequest extends BaseRequest {

    @NotNull(message = "4000011")
    private String tenantCode;

    @NotBlank(message = "4000012")
    @Size(max = 255, message = "4000013")
    private String name;

    @NotNull(message = "4000029")
    @ValidEnum(enumClass = SessionChannel.class, message = "4000029")
    private String channel;

    @NotEmpty(message = "4000031")
    private Map<String, Object> inputData;
}
