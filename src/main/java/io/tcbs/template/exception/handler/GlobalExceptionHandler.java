package io.tcbs.template.exception.handler;

import io.tcbs.template.dto.response.BaseResponse;
import io.tcbs.template.enums.ErrorCode;
import io.tcbs.template.exception.BusinessException;
import io.tcbs.template.util.MessagesUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.helpers.MessageFormatter;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.ObjectUtils;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final ExceptionResolve exceptionResolve;

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<BaseResponse<?>> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        return exceptionResolve.resolveBindException(ex);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<BaseResponse<?>> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException ex) {
        var args = new Object[] { ex.getMethod() };
        var errorCode = ErrorCode.METHOD_NOT_ALLOWED;
        String message = resolveMessage(errorCode, args);
        return new ResponseEntity<>(BaseResponse.failed(errorCode, message), resolveHttpStatus(errorCode));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<BaseResponse<?>> handleNoResourceFoundException(NoResourceFoundException ex) {
        var args = new Object[] { ex.getResourcePath() };
        var errorCode = ErrorCode.NOT_FOUND;
        String message = resolveMessage(errorCode, args);
        return new ResponseEntity<>(BaseResponse.failed(errorCode, message), resolveHttpStatus(errorCode));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<BaseResponse<?>> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException ex) {
        var args = new Object[] { ex.getName() };
        var errorCode = ErrorCode.INVALID_PATH_VARIABLE;
        String message = resolveMessage(errorCode, args);
        return new ResponseEntity<>(BaseResponse.failed(errorCode, message), resolveHttpStatus(errorCode));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<BaseResponse<?>> handleMissingServletRequestParameterException(MissingServletRequestParameterException ex) {
        var args = new Object[] { ex.getParameterName() };
        var errorCode = ErrorCode.MISSING_REQUEST_PARAM;
        String message = resolveMessage(errorCode, args);
        return new ResponseEntity<>(BaseResponse.failed(errorCode, message), resolveHttpStatus(errorCode));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<BaseResponse<?>> handleBusinessException(BusinessException ex) {
        var errorCode = ex.getErrorCode();
        String message = resolveMessage(errorCode, ex.getArgs());
        return new ResponseEntity<>(BaseResponse.failed(errorCode, message), resolveHttpStatus(errorCode));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<BaseResponse<?>> handleGeneric(Exception ex) {
        log.error("Có lỗi xảy ra: {}", ex.getMessage());
        return new ResponseEntity<>(
            BaseResponse.failed(ErrorCode.INTERNAL_SERVER_ERROR, ex.getMessage()),
            HttpStatus.INTERNAL_SERVER_ERROR
        );
    }

    private HttpStatus resolveHttpStatus(ErrorCode errorCode) {
        var httpCode = errorCode.getCode() / 10000;
        return switch (httpCode) {
            case 401 -> HttpStatus.UNAUTHORIZED;
            case 403 -> HttpStatus.FORBIDDEN;
            case 404 -> HttpStatus.NOT_FOUND;
            case 405 -> HttpStatus.METHOD_NOT_ALLOWED;
            case 409 -> HttpStatus.CONFLICT;
            case 500 -> HttpStatus.INTERNAL_SERVER_ERROR;
            default -> HttpStatus.BAD_REQUEST;
        };
    }

    private String resolveMessage(ErrorCode errorCode, Object[] args) {
        String msg = MessagesUtil.getMessage(String.valueOf(errorCode.getCode()));
        String template = ObjectUtils.isEmpty(msg) ? String.valueOf(errorCode.getCode()) : msg;
        return formatMessage(template, args);
    }

    private String formatMessage(String messagePattern, Object[] argArray) {
        if (argArray != null && argArray.length > 0) {
            return MessageFormatter.arrayFormat(messagePattern, argArray).getMessage();
        }
        return messagePattern;
    }
}
