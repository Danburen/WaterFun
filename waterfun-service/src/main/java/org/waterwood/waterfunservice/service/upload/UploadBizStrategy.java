package org.waterwood.waterfunservice.service.upload;

import org.waterwood.waterfunservice.api.BizType;
import org.waterwood.waterfunservice.api.request.UploadPolicyReq;
import org.waterwood.waterfunservicecore.api.req.CloudPutCallbackReq;
import org.waterwood.waterfunservicecore.api.resp.PresignedResp;
import org.waterwood.waterfunservicecore.utils.BizUploadPayload;

import java.util.List;
import java.util.Set;

public interface UploadBizStrategy {
    Set<BizType> getTargetBizTypes();
    List<PresignedResp> handle(UploadPolicyReq request);
    void handleCallback(CloudPutCallbackReq request, BizUploadPayload payload);
}
