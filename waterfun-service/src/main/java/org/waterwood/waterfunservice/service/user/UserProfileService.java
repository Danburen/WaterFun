package org.waterwood.waterfunservice.service.user;

import org.waterwood.waterfunservice.api.UploadContext;
import org.waterwood.waterfunservice.api.request.UploadPolicyReq;
import org.waterwood.waterfunservicecore.api.req.CloudPutCallbackReq;
import org.waterwood.waterfunservicecore.api.resp.PresignedResp;

import java.util.List;

public interface UserProfileService {
    List<PresignedResp> handleUserAvatarUpload(UploadPolicyReq request);
    /**
     * Callback procession for avatar callback.
     *
     * @param req     the callback req body, contains the file key and token.
     */
    void uploadAvatarCallback(CloudPutCallbackReq req, UploadContext<Long> ctx);

}
