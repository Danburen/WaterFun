package org.waterwood.waterfunservicecore.api.resp;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

@Data
@AllArgsConstructor
public class AccountResp implements Serializable {
    private String phoneMasked;
    private String emailMasked;
    private Boolean phoneVerified;
    private Boolean emailVerified;
}
