package org.waterwood.waterfunservice.service.upload;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.waterwood.waterfunservice.api.UserBizType;
import org.waterwood.waterfunservice.api.UserUploadContext;
import org.waterwood.waterfunservice.api.UserUploadPolicyReq;
import org.waterwood.waterfunservice.service.post.PostService;
import org.waterwood.waterfunservicecore.api.req.CloudPutCallbackReq;
import org.waterwood.waterfunservicecore.api.resp.PresignedResp;
import org.waterwood.waterfunservicecore.infrastructure.utils.BizUploadPayload;
import org.waterwood.waterfunservicecore.services.sys.storage.CloudFileService;
import org.waterwood.waterfunservicecore.services.sys.upload.UploadBizStrategy;

import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class PostUploadStrategy implements UploadBizStrategy<UserUploadPolicyReq> {

    private final CloudFileService cloudFileService;
    private final PostService postService;

    @Override
    public Set<String> getTargetBizTypeCodes() {
        return Set.of(
                UserBizType.POST_COVERAGE_IMAGE.getCode(),
                UserBizType.POST_CONTENT_IMAGE.getCode()
        );
    }

    @Override
    public List<PresignedResp> handle(UserUploadPolicyReq request) {
        return switch (request.getBizType()){
            case POST_COVERAGE_IMAGE -> postService.handlePostCoverageImageUpload(request);
            case POST_CONTENT_IMAGE ->  postService.handlePostContentImageUpload(request);
            default -> throw new IllegalStateException("Unexpected value: " + request.getBizType());
        };
    }

    @Override
    public String handleCallback(CloudPutCallbackReq request, BizUploadPayload payload) {
        UserUploadContext<Long> ctx = payload.toContext(UserBizType.class, Long.class, UserUploadContext::new);
        UserBizType type = ctx.getBizType();
        if (type == UserBizType.POST_CONTENT_IMAGE || type == UserBizType.POST_COVERAGE_IMAGE) {
            postService.handlePostImageUploadCallback(request, ctx);
        } else {
            throw new IllegalStateException("Unexpected biz type for PostUploadStrategy: " + type);
        }
        return payload.getResourceUuid();
    }
}
