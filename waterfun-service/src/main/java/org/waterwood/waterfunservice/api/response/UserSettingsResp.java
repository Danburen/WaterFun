package org.waterwood.waterfunservice.api.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserSettingsResp {
    private UserNotificationSettings notifications;
    private UserPrivacySettings privacy;
}
