package io.tcbs.template.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class BaseRequest {

    private String requestId;
    private String uri;
    private String apiKey;
    private String apiSecret;
    private String serialNumber;
}
