package org.waterwood.waterfunservice.service.user;

import org.waterwood.waterfunservice.api.request.UploadPolicyReq;
import org.waterwood.waterfunservicecore.api.req.CloudPutCallbackReq;
import org.waterwood.waterfunservicecore.api.resp.PresignedResp;
import org.waterwood.waterfunservicecore.utils.BizUploadPayload;

import java.util.List;

public interface UserProfileService {

    /**
     * Callback procession for avatar callback.
     *
     * @param req     the callback req body, contains the file key and token.
     * @param payload
     */
    void uploadAvatarCallback(CloudPutCallbackReq req, BizUploadPayload payload);

    List<PresignedResp> handleUserAvatarUpload(UploadPolicyReq request);
}
