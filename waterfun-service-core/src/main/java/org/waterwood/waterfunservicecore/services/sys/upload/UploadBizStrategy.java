package org.waterwood.waterfunservicecore.services.sys.upload;

import org.waterwood.waterfunservicecore.api.req.CloudPutCallbackReq;
import org.waterwood.waterfunservicecore.api.resp.PresignedResp;
import org.waterwood.waterfunservicecore.infrastructure.utils.BizUploadPayload;

import java.util.List;
import java.util.Set;

public interface UploadBizStrategy<R extends UploadPolicy> {
    Set<String> getTargetBizTypeCodes();
    List<PresignedResp> handle(R request);

    /**
     * Handle resource callback and return the resource uuid
     * @param request {@link CloudPutCallbackReq} callback request from cloud file service
     * @param payload {@link BizUploadPayload} upload payload which contains the biz target id and biz type.
     * @return {@link String} uuid string of the processed resource.
     */
    String handleCallback(CloudPutCallbackReq request, BizUploadPayload payload);
}
