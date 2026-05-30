package org.waterwood.waterfunservice.service.upload;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.waterwood.waterfunservice.api.BizType;
import org.waterwood.waterfunservice.api.UploadContext;
import org.waterwood.waterfunservice.api.request.UploadPolicyReq;
import org.waterwood.waterfunservice.service.post.PostService;
import org.waterwood.waterfunservicecore.api.req.CloudPutCallbackReq;
import org.waterwood.waterfunservicecore.api.resp.PresignedResp;
import org.waterwood.waterfunservicecore.services.sys.storage.CloudFileService;
import org.waterwood.waterfunservicecore.utils.BizUploadPayload;

import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class PostUploadStrategy implements UploadBizStrategy{

    private final CloudFileService cloudFileService;
    private final PostService postService;

    @Override
    public Set<BizType> getTargetBizTypes() {
        return Set.of(
                BizType.POST_COVERAGE_IMAGE,
                BizType.POST_CONTENT_IMAGE
        );
    }

    // TODO
    @Override
    public List<PresignedResp> handle(UploadPolicyReq request) {
        return switch (request.getBizType()){
            case POST_COVERAGE_IMAGE -> postService.handlePostCoverageImageUpload(request);
            case POST_CONTENT_IMAGE ->  postService.handlePostContentImageUpload(request);
            default -> throw new IllegalStateException("Unexpected value: " + request.getBizType());
        };
    }

    @Override
    public void handleCallback(CloudPutCallbackReq request, BizUploadPayload payload) {
        UploadContext<Long> ctx = UploadContext.fromPayload(payload, Long.class);
        BizType type = ctx.getBizType();
        if (type == BizType.POST_CONTENT_IMAGE || type == BizType.POST_COVERAGE_IMAGE) {
            postService.handlePostImageUploadCallback(request, ctx);
        } else {
            throw new IllegalStateException("Unexpected biz type for PostUploadStrategy: " + type);
        }
    }
}
