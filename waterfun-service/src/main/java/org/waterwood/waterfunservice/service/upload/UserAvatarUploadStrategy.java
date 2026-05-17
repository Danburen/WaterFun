package org.waterwood.waterfunservice.service.upload;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.waterwood.common.io.FileExtension;
import org.waterwood.waterfunservice.api.BizType;
import org.waterwood.waterfunservice.api.request.UploadPolicyReq;
import org.waterwood.waterfunservice.service.user.UserProfileService;
import org.waterwood.waterfunservicecore.api.req.CloudPutCallbackReq;
import org.waterwood.waterfunservicecore.api.resp.PresignedResp;
import org.waterwood.waterfunservicecore.entity.audit.task.MediaResourceType;
import org.waterwood.waterfunservicecore.exception.IllegalUploadArgumentException;
import org.waterwood.waterfunservicecore.exception.UnsupportedFileExtension;
import org.waterwood.waterfunservicecore.infrastructure.utils.context.UserCtxHolder;
import org.waterwood.waterfunservicecore.utils.BizPayload;

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
    public PresignedResp handle(UploadPolicyReq request) {
        Long userId = UserCtxHolder.getUserUid();
        if(request.getExts().size() != 1){
            throw new IllegalUploadArgumentException(1);
        }
        FileExtension ext = FileExtension.fromExt(request.getExts().getFirst());
        if(MediaResourceType.USER_AVATAR.isAllowed(ext)){
            throw new UnsupportedFileExtension(ext.getExt(), request.getExts().getFirst());
        }
        return userProfileService.getUploadPolicyAndSubmitAvatar(
                userId, ext
        );
    }

    @Override
    public void handleCallback(CloudPutCallbackReq request, BizPayload payload) {
        userProfileService.uploadAvatarCallback(request, payload);
    }
}
