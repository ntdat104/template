package io.tcbs.template.dto.response;

import io.tcbs.template.enums.ErrorCode;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class BaseResponse<T> {

    private Meta meta;

    private T data;

    public static <T> BaseResponse<T> success(T data) {
        BaseResponse<T> response = new BaseResponse<>();
        response.setData(data);
        response.setMeta(Meta.success());
        return response;
    }

    public static <T> BaseResponse<T> success() {
        BaseResponse<T> response = new BaseResponse<>();
        response.setMeta(Meta.success());
        return response;
    }

    public static <T> BaseResponse<T> failed(ErrorCode errorCode) {
        BaseResponse<T> response = new BaseResponse<>();
        response.setMeta(Meta.failed(errorCode));
        return response;
    }

    public static <T> BaseResponse<T> failed(ErrorCode errorCode, String message) {
        BaseResponse<T> response = new BaseResponse<>();
        response.setMeta(Meta.failed(errorCode, message));
        return response;
    }

    public static <T> BaseResponse<T> failed(ErrorCode errorCode, List<ErrorViolation> errors) {
        BaseResponse<T> response = new BaseResponse<>();
        response.setMeta(Meta.failed(errorCode, errors));
        return response;
    }
}
