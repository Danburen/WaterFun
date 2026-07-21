package org.waterwood.waterfunservice.api.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class EmailReAuthReq {
    @NotBlank
    private String reAuthToken;
    @NotBlank
    private String email;
}
