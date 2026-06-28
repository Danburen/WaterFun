package org.waterwood.waterfunservice.service.user;

import org.waterwood.waterfunservice.api.response.UserSettingsResp;

public interface UserSettingsService {

    UserSettingsResp getSettings(Long userUid);

    void updateSettings(Long userUid, UserSettingsResp settings);
}
