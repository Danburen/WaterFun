package org.waterwood.waterfunservicecore.services.sys.upload;

import org.waterwood.waterfunservicecore.api.req.CloudPutCallbackReq;
import org.waterwood.waterfunservicecore.api.resp.PresignedResp;
import org.waterwood.waterfunservicecore.infrastructure.utils.BizUploadPayload;

import java.util.List;
import java.util.Set;

public interface UploadBizStrategy<R extends UploadPolicy> {
    Set<String> getTargetBizTypeCodes();
    List<PresignedResp> handle(R request);
    void handleCallback(CloudPutCallbackReq request, BizUploadPayload payload);
}
