package org.waterwood.waterfunservice.api.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserNotificationSettings {
    private Boolean messageNotifications = true;
    private Boolean commentNotifications = true;
    private Boolean likeNotifications = true;
    private Boolean followNotifications = true;
    private Boolean eventNotifications = true;
    private Boolean emailNotifications = false;
}
