package io.tcbs.template.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SystemConfigurationResponse extends AbstractResponse {

    private Long id;
    private String code;
    private String name;
    private String value;
}
