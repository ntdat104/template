package io.tcbs.template.validator;

import io.tcbs.template.enums.ErrorCode;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

@Component
public class ErrorCodeValidator {

    @PostConstruct
    public void validateErrorCodes() {
        ErrorCode.values();
    }
}
