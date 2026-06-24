package org.waterwood.waterfunadminservice.service.upload;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.waterwood.common.CloudFSRoot;
import org.waterwood.common.io.FileExtension;
import org.waterwood.common.io.ResourceType;
import org.waterwood.utils.StringUtil;
import org.waterwood.waterfunadminservice.api.AdminUploadContext;
import org.waterwood.waterfunadminservice.api.request.AdminUploadPolicyReq;
import org.waterwood.waterfunadminservice.service.content.AdminBizType;
import org.waterwood.waterfunservicecore.api.req.CloudPutCallbackReq;
import org.waterwood.waterfunservicecore.api.resp.PresignedResp;
import org.waterwood.waterfunservicecore.entity.audit.TargetType;
import org.waterwood.waterfunservicecore.entity.resource.Resource;
import org.waterwood.waterfunservicecore.entity.resource.ResourceStatus;
import org.waterwood.waterfunservicecore.exception.ForbiddenException;
import org.waterwood.waterfunservicecore.exception.reference.ResourceReferenceInvalidException;
import org.waterwood.waterfunservicecore.infrastructure.persistence.ResourceRepository;
import org.waterwood.waterfunservicecore.infrastructure.utils.BizUploadPayload;
import org.waterwood.waterfunservicecore.infrastructure.utils.CosKeyPathGenerator;
import org.waterwood.waterfunservicecore.infrastructure.utils.context.UserCtxHolder;
import org.waterwood.waterfunservicecore.infrastructure.validation.UploadValidator;
import org.waterwood.waterfunservicecore.services.sys.storage.CloudFileService;
import org.waterwood.waterfunservicecore.services.sys.upload.UploadBizStrategy;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AdminPostUploadStrategy implements UploadBizStrategy<AdminUploadPolicyReq> {

    private final CloudFileService cloudFileService;
    private final ResourceRepository resourceRepository;

    @Override
    public Set<String> getTargetBizTypeCodes() {
        return Set.of(
                AdminBizType.POST_COVERAGE_IMAGE.getCode(),
                AdminBizType.POST_CONTENT_IMAGE.getCode()
        );
    }

    @Override
    public List<PresignedResp> handle(AdminUploadPolicyReq request) {
        TargetType targetType = switch (request.getBizType()) {
            case POST_COVERAGE_IMAGE -> TargetType.POST_COVERAGE_IMAGE;
            case POST_CONTENT_IMAGE -> TargetType.POST_CONTENT_IMAGE;
            default -> throw new IllegalStateException("Unexpected biz type: " + request.getBizType());
        };

        FileExtension ext = UploadValidator.validateSingleFileUpload(request, targetType);

        UUID resourceUUID = UUID.randomUUID();
        BizUploadPayload payload = BizUploadPayload.of(
                UserCtxHolder.getUserUid(),
                request.getBizType().getCode(),
                resourceUUID
        );

        String cosPath = CosKeyPathGenerator.of(resourceUUID, ext);
        resourceRepository.save(
                cloudFileService.createAndSetUpUploadRes(
                        StringUtil.noDashUUIDString(resourceUUID),
                        CosKeyPathGenerator.of(resourceUUID, ext),
                        UserCtxHolder.getUserUid()
                )
        );
        return List.of(cloudFileService.buildPutPolicyWithPayload(
                CloudFSRoot.SYSTEM,
                cosPath,
                payload)
        );
    }

    @Override
    public String handleCallback(CloudPutCallbackReq request, BizUploadPayload payload) {
        AdminUploadContext<Long> ctx = payload.toContext(
                AdminBizType.class,
                Long.class,
                AdminUploadContext::new
        );

        Long uploaderUid = UserCtxHolder.getUserUid();
        if (!uploaderUid.equals(ctx.getBizId())) {
            throw new ForbiddenException();
        }

        String resourceUuid = ctx.getResourceUuid();
        Resource res = resourceRepository.findByUuidAndStatus(
                resourceUuid,
                ResourceStatus.UPLOAD_PENDING
        ).orElseThrow(() -> new ResourceReferenceInvalidException(resourceUuid));

        cloudFileService.setAndValidResourceForCallback(
                res,
                CloudFSRoot.SYSTEM,
                ResourceStatus.ORPHAN,
                ResourceType.IMAGE
        );
        resourceRepository.save(res);
        return resourceUuid;
    }
}
