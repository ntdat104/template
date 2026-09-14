package io.tcbs.template.dto.response;

import java.io.Serial;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AbstractResponse implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long createdDateMs;
    private String createdDate;
    private String createdDateAgo;
    private Long lastModifiedDateMs;
    private String lastModifiedDate;
    private String lastModifiedDateAgo;
}
