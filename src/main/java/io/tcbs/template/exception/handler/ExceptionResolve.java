package io.tcbs.template.exception.handler;

import io.tcbs.template.dto.response.BaseResponse;
import io.tcbs.template.dto.response.ErrorViolation;
import io.tcbs.template.enums.ErrorCode;
import io.tcbs.template.util.MessagesUtil;
import java.util.Comparator;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindException;

@Component
public class ExceptionResolve {

    public ResponseEntity<BaseResponse<?>> resolveBindException(BindException exception) {
        List<ErrorViolation> errors = exception
            .getBindingResult()
            .getFieldErrors()
            .stream()
            .map(e -> {
                var errorCode = ErrorCode.INVALID_PARAMETERS;
                String defaultMessage;
                if (e.getDefaultMessage() != null) {
                    defaultMessage = e.getDefaultMessage();
                } else {
                    defaultMessage = String.valueOf(errorCode.getCode());
                }
                var code = parseErrorCode(defaultMessage, errorCode.getCode());
                ErrorViolation violation = new ErrorViolation();
                violation.setField(e.getField());
                violation.setRejectedValue(e.getRejectedValue());
                violation.setCode(code);
                violation.setMessage(MessagesUtil.getMessage(ErrorCode.findByCode(code, errorCode)));
                return violation;
            })
            .sorted(Comparator.comparingInt(ErrorViolation::getCode))
            .toList();
        var defaultErrorCode = ErrorCode.INVALID_PARAMETERS;
        var error = BaseResponse.failed(defaultErrorCode, errors);
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    private int parseErrorCode(String value, int defaultValue) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
