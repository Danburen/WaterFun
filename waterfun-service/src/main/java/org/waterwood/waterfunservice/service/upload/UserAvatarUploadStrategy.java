package org.waterwood.waterfunservice.service.upload;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.waterwood.waterfunservice.api.BizType;
import org.waterwood.waterfunservice.api.UploadContext;
import org.waterwood.waterfunservice.api.request.UploadPolicyReq;
import org.waterwood.waterfunservice.service.user.UserProfileService;
import org.waterwood.waterfunservicecore.api.req.CloudPutCallbackReq;
import org.waterwood.waterfunservicecore.api.resp.PresignedResp;
import org.waterwood.waterfunservicecore.utils.BizUploadPayload;

import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class UserAvatarUploadStrategy implements UploadBizStrategy{
    private final UserProfileService userProfileService;

    @Override
    public Set<BizType> getTargetBizTypes() {
        return Set.of(BizType.AVATAR);
    }

    @Override
    public List<PresignedResp> handle(UploadPolicyReq request) {
        return userProfileService.handleUserAvatarUpload(request);
    }

    @Override
    public void handleCallback(CloudPutCallbackReq request, BizUploadPayload payload) {
        userProfileService.uploadAvatarCallback(request, UploadContext.fromPayload(payload, Long.class));
    }
}
