package io.tcbs.template.exception;

import io.tcbs.template.enums.ErrorCode;
import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;
    private final Object[] args;

    public BusinessException(ErrorCode errorCode) {
        this.errorCode = errorCode;
        this.args = null;
    }

    public BusinessException(ErrorCode errorCode, Object... args) {
        this.errorCode = errorCode;
        this.args = args == null ? null : args.clone();
    }

    public Object[] getArgs() {
        return args == null ? null : args.clone();
    }
}
