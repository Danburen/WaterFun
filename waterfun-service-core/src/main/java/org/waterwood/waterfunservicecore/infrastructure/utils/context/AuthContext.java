package org.waterwood.waterfunservicecore.infrastructure.utils.context;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.waterwood.waterfunservicecore.entity.user.UserSetting;

import java.io.Serializable;
import java.util.Locale;

@Data
@AllArgsConstructor
@NoArgsConstructor
public final class AuthContext implements Serializable {
    private Long userUid;
    private String jti;
    private String did;
    private Locale locale = Locale.ENGLISH;
    private String clientIp;
    private UserSetting userSetting;
}
