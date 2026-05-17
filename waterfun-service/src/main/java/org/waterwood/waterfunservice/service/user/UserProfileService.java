package org.waterwood.waterfunservice.service.user;

import org.waterwood.common.io.FileExtension;
import org.waterwood.waterfunservicecore.api.req.CloudPutCallbackReq;
import org.waterwood.waterfunservicecore.api.resp.PresignedResp;
import org.waterwood.waterfunservicecore.utils.BizPayload;

public interface UserProfileService {
    /**
     * Get the upload policy for user avatar, temporary store the file
     * soon admin will audit the avatar
     *
     * @param userUid       target user uid
     * @param fileExtension suffix ofPending the avatar file
     * @return post policy resp
     */
    PresignedResp getUploadPolicyAndSubmitAvatar(Long userUid, FileExtension fileExtension);

    /**
     * Callback procession for avatar callback.
     *
     * @param req     the callback req body, contains the file key and token.
     * @param payload
     */
    void uploadAvatarCallback(CloudPutCallbackReq req, BizPayload payload);
}
