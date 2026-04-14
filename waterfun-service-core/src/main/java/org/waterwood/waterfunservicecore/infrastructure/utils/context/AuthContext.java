package org.waterwood.waterfunservicecore.infrastructure.utils.context;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public final class AuthContext implements Serializable {
    private Long userUid;
    private String jti;
    private String did;

}
