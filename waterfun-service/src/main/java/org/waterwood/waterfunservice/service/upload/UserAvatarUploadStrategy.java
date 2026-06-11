package org.waterwood.waterfunservice.service.upload;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.waterwood.waterfunservice.api.UserBizType;
import org.waterwood.waterfunservice.api.UserUploadContext;
import org.waterwood.waterfunservice.api.UserUploadPolicyReq;
import org.waterwood.waterfunservice.service.user.UserProfileService;
import org.waterwood.waterfunservicecore.api.req.CloudPutCallbackReq;
import org.waterwood.waterfunservicecore.api.resp.PresignedResp;
import org.waterwood.waterfunservicecore.infrastructure.utils.BizUploadPayload;
import org.waterwood.waterfunservicecore.services.sys.upload.UploadBizStrategy;

import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class UserAvatarUploadStrategy implements UploadBizStrategy<UserUploadPolicyReq> {
    private final UserProfileService userProfileService;

    @Override
    public Set<String> getTargetBizTypeCodes() {
        return Set.of(UserBizType.AVATAR.getCode());
    }

    @Override
    public List<PresignedResp> handle(UserUploadPolicyReq request) {
        return userProfileService.handleUserAvatarUpload(request);
    }

    @Override
    public String handleCallback(CloudPutCallbackReq request, BizUploadPayload payload) {
        userProfileService.uploadAvatarCallback(
                request, payload.toContext(UserBizType.class, Long.class, UserUploadContext::new)
        );
        return payload.getResourceUuid();
    }
}
