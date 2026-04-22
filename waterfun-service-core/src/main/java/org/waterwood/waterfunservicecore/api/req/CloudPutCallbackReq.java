package org.waterwood.waterfunservicecore.api.req;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CloudPutCallbackReq {
    @NotBlank
    private String key;
    @NotBlank
    private String token;
}
