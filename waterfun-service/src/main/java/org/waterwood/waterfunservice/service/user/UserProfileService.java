package org.waterwood.waterfunservice.service.user;

import org.waterwood.waterfunservice.api.UserUploadContext;
import org.waterwood.waterfunservice.api.UserUploadPolicyReq;
import org.waterwood.waterfunservicecore.api.req.CloudPutCallbackReq;
import org.waterwood.waterfunservicecore.api.resp.PresignedResp;

import java.util.List;

public interface UserProfileService {
    List<PresignedResp> handleUserAvatarUpload(UserUploadPolicyReq request);
    /**
     * Callback procession for avatar callback.
     *
     * @param req     the callback req body, contains the file key and token.
     */
    void uploadAvatarCallback(CloudPutCallbackReq req, UserUploadContext<Long> ctx);

}
