package org.waterwood.waterfunservice.api.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserPrivacySettings {
    private String profileVisibility = "PUBLIC";
    private String workVisibility = "PUBLIC";
    private String commentPermission = "ALL";
    private String messagePermission = "ALL";
    private Boolean allowFollow = true;
    private Boolean showActiveStatus = true;
}
