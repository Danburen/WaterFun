package org.waterwood.waterfunservice.service.user.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.waterwood.waterfunservice.api.response.UserNotificationSettings;
import org.waterwood.waterfunservice.api.response.UserPrivacySettings;
import org.waterwood.waterfunservice.api.response.UserSettingsResp;
import org.waterwood.waterfunservice.service.user.UserSettingsService;
import org.waterwood.waterfunservicecore.entity.user.*;
import org.waterwood.waterfunservicecore.infrastructure.persistence.user.UserRepository;
import org.waterwood.waterfunservicecore.infrastructure.persistence.user.UserSettingRepository;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserSettingsServiceImpl implements UserSettingsService {

    private final UserSettingRepository userSettingRepository;
    private final UserRepository userRepository;

    @Override
    public UserSettingsResp getSettings(Long userUid) {
        UserSetting setting = userSettingRepository.findById(userUid).orElse(null);

        UserPrivacySettings privacy = new UserPrivacySettings();
        UserNotificationSettings notifications = new UserNotificationSettings();

        if (setting != null) {
            privacy.setProfileVisibility(setting.getProfileVisibility().name());
            privacy.setWorkVisibility(setting.getWorkVisibility().name());
            privacy.setCommentPermission(setting.getCommentPermission().name());
            privacy.setMessagePermission(setting.getMessagePermission().name());
            privacy.setAllowFollow(setting.getAllowFollow());
            privacy.setShowActiveStatus(setting.getShowActiveStatus());

            notifications.setMessageNotifications(setting.getMessageNotifications());
            notifications.setCommentNotifications(setting.getCommentNotifications());
            notifications.setLikeNotifications(setting.getLikeNotifications());
            notifications.setFollowNotifications(setting.getFollowNotifications());
            notifications.setEventNotifications(setting.getEventNotifications());
            notifications.setEmailNotifications(setting.getEmailNotifications());
        }

        return new UserSettingsResp(notifications, privacy);
    }

    @Override
    @Transactional
    public void updateSettings(Long userUid, UserSettingsResp settings) {
        UserSetting setting = userSettingRepository.findById(userUid).orElseGet(() -> {
            User user = userRepository.getReferenceById(userUid);
            UserSetting newSetting = new UserSetting();
            newSetting.setUser(user);
            return newSetting;
        });

        if (settings.getPrivacy() != null) {
            UserPrivacySettings privacy = settings.getPrivacy();
            if (privacy.getProfileVisibility() != null)
                setting.setProfileVisibility(ProfileVisibility.valueOf(privacy.getProfileVisibility()));
            if (privacy.getWorkVisibility() != null)
                setting.setWorkVisibility(ProfileVisibility.valueOf(privacy.getWorkVisibility()));
            if (privacy.getCommentPermission() != null)
                setting.setCommentPermission(ContentPermission.valueOf(privacy.getCommentPermission()));
            if (privacy.getMessagePermission() != null)
                setting.setMessagePermission(ContentPermission.valueOf(privacy.getMessagePermission()));
            if (privacy.getAllowFollow() != null)
                setting.setAllowFollow(privacy.getAllowFollow());
            if (privacy.getShowActiveStatus() != null)
                setting.setShowActiveStatus(privacy.getShowActiveStatus());
        }

        if (settings.getNotifications() != null) {
            UserNotificationSettings notifications = settings.getNotifications();
            if (notifications.getMessageNotifications() != null)
                setting.setMessageNotifications(notifications.getMessageNotifications());
            if (notifications.getCommentNotifications() != null)
                setting.setCommentNotifications(notifications.getCommentNotifications());
            if (notifications.getLikeNotifications() != null)
                setting.setLikeNotifications(notifications.getLikeNotifications());
            if (notifications.getFollowNotifications() != null)
                setting.setFollowNotifications(notifications.getFollowNotifications());
            if (notifications.getEventNotifications() != null)
                setting.setEventNotifications(notifications.getEventNotifications());
            if (notifications.getEmailNotifications() != null)
                setting.setEmailNotifications(notifications.getEmailNotifications());
        }

        userSettingRepository.save(setting);
    }
}
