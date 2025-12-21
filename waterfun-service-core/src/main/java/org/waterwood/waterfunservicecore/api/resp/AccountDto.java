package org.waterwood.waterfunservicecore.api.resp;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;
import java.time.Instant;

@Data
@AllArgsConstructor
public class AccountDto implements Serializable {
    private String phoneMasked;
    private String emailMasked;
    private Boolean phoneVerified;
    private Boolean emailVerified;
}
