package io.tcbs.template.dto.response;

import io.tcbs.template.constants.Constant;
import io.tcbs.template.enums.ErrorCode;
import io.tcbs.template.util.ContextUtil;
import io.tcbs.template.util.DatetimeUtil;
import io.tcbs.template.util.MessagesUtil;
import java.time.Instant;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@Setter
@NoArgsConstructor
public class Meta {

    private String serviceCode;
    private String requestId;

    private Long timestamp;
    private String datetime;

    private Integer code;
    private String message;

    private Long total = null;

    private List<ErrorViolation> errors;

    private static Meta base() {
        var now = Instant.now();
        var meta = new Meta();
        meta.setServiceCode(Constant.SERVICE_CODE);
        meta.setRequestId(ContextUtil.getRequestId());
        meta.setTimestamp(DatetimeUtil.toEpochMilli(now));
        meta.setDatetime(DatetimeUtil.format(now));
        return meta;
    }

    public static Meta success() {
        Meta meta = base();
        meta.setCode(ErrorCode.SUCCESS.getCode());
        meta.setMessage(MessagesUtil.getMessage(ErrorCode.SUCCESS));
        return meta;
    }

    public static Meta success(Long total) {
        Meta meta = success();
        meta.setTotal(total);
        return meta;
    }

    public static Meta failed(ErrorCode errorCode) {
        Meta meta = base();
        meta.setCode(errorCode.getCode());
        meta.setMessage(MessagesUtil.getMessage(errorCode));
        return meta;
    }

    public static Meta failed(ErrorCode errorCode, String message) {
        Meta meta = base();
        meta.setCode(errorCode.getCode());
        meta.setMessage(message);
        return meta;
    }

    public static Meta failed(ErrorCode errorCode, List<ErrorViolation> errors) {
        Meta meta = base();
        meta.setCode(errorCode.getCode());
        meta.setMessage(MessagesUtil.getMessage(errorCode));
        meta.setErrors(errors);
        return meta;
    }
}
