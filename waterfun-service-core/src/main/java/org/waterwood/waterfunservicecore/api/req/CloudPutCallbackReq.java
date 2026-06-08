package org.waterwood.waterfunservicecore.api.req;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Cloud put callback request body
 * token is the same as resource uuid.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CloudPutCallbackReq {
    @NotBlank
    private String token;
}
