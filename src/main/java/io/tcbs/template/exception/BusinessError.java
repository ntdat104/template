package io.tcbs.template.exception;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BusinessError {

    private int code;
    private String message;

    public static BusinessError getError(String code) {
        BusinessError error = new BusinessError();
        error.setCode(Integer.parseInt(code));
        error.setMessage(code);
        return error;
    }

    public static BusinessError getError(int code) {
        BusinessError error = new BusinessError();
        error.setCode(code);
        error.setMessage(String.valueOf(code));
        return error;
    }
}
