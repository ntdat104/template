package io.tcbs.template.dto.response;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ErrorViolation {

    private String field;
    private Object rejectedValue;
    private int code;
    private String message;
}
