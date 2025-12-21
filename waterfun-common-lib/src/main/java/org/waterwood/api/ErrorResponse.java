package org.waterwood.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.util.Date;
import java.util.List;

/**
 * Unified error code response class
 * All exception returns use this structure
 */
@Getter
@Setter
@AllArgsConstructor
@Builder
public class ErrorResponse {
    private String code;
    private String message;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<String> errors;
    private Date timestamp;

    public  ErrorResponse(String code, String message) {
        this.code = code;
        this.message = message;
        this.timestamp = new Date();
    }
}
