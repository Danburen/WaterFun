package org.waterwood.waterfunservice.service.user;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.waterwood.common.CloudFSRoot;
import org.waterwood.common.io.FileExtension;
import org.waterwood.common.io.ResourceType;
import org.waterwood.utils.StringUtil;
import org.waterwood.waterfunservice.api.UserUploadContext;
import org.waterwood.waterfunservice.api.UserUploadPolicyReq;
import org.waterwood.waterfunservicecore.api.moderation.AuditPayload;
import org.waterwood.waterfunservicecore.api.moderation.ImageAuditPayload;
import org.waterwood.waterfunservicecore.api.req.CloudPutCallbackReq;
import org.waterwood.waterfunservicecore.api.resp.PresignedResp;
import org.waterwood.waterfunservicecore.entity.resource.Resource;
import org.waterwood.waterfunservicecore.entity.resource.ResourceStatus;
import org.waterwood.waterfunservicecore.exception.ForbiddenException;
import org.waterwood.waterfunservicecore.exception.io.IllegalUploadCountException;
import org.waterwood.waterfunservicecore.exception.io.UnsupportedFileExtension;
import org.waterwood.waterfunservicecore.exception.reference.ResourceReferenceInvalidException;
import org.waterwood.waterfunservicecore.infrastructure.persistence.ResourceRepository;
import org.waterwood.waterfunservicecore.infrastructure.persistence.audit.AuditTaskRepository;
import org.waterwood.waterfunservicecore.entity.audit.TargetType;
import org.waterwood.waterfunservicecore.infrastructure.persistence.audit.AuditTaskResourceRepository;
import org.waterwood.waterfunservicecore.infrastructure.persistence.user.UserRepository;
import org.waterwood.waterfunservicecore.infrastructure.utils.context.UserCtxHolder;
import org.waterwood.waterfunservicecore.services.audit.ContentAuditService;
import org.waterwood.waterfunservicecore.services.sys.storage.CloudFileService;
import org.waterwood.waterfunservicecore.infrastructure.utils.BizUploadPayload;
import org.waterwood.waterfunservicecore.infrastructure.utils.CosKeyPathGenerator;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserProfileServiceImpl implements UserProfileService {
    private final AuditTaskRepository auditTaskRepository;
    private final CloudFileService cloudFileService;
    private final UserRepository userRepository;
    private final AuditTaskResourceRepository auditTaskResourceRepository;
    private final ResourceRepository resourceRepository;
    private final ContentAuditService contentAuditService;

    @Transactional
    @Override
    public List<PresignedResp> handleUserAvatarUpload(UserUploadPolicyReq request) {
        Long userUid = UserCtxHolder.getUserUid();
        if(request.getExts().size() != 1){
            throw new IllegalUploadCountException(1);
        }
        FileExtension ext = FileExtension.fromExt(request.getExts().getFirst());
        if(! TargetType.USER_AVATAR.isAllowed(ext)){
            throw new UnsupportedFileExtension(ext.getExt(), request.getExts().getFirst());
        }

        UUID resourceUUID = UUID.randomUUID();
        BizUploadPayload payload = UserUploadContext.<Long>builder()
                .bizId(userUid)
                .bizType(request.getBizType())
                .resourceUuid(resourceUUID.toString().replace("-", ""))
                .build()
                .toPayload();
        String cosPath = CosKeyPathGenerator.ofUser(userUid, resourceUUID, ext);
        resourceRepository.save(cloudFileService.createAndSetUpUploadRes(
                StringUtil.noDashUUIDString(resourceUUID),
                cosPath,
                UserCtxHolder.getUserUid()
        ));

        return List.of(cloudFileService.buildPutPolicyWithPayload(
                CloudFSRoot.USER,
                cosPath,
                payload)
        );
    }

    @Transactional
    @Override
    public void uploadAvatarCallback(CloudPutCallbackReq req, UserUploadContext<Long> ctx) {
        String resourceUuid = ctx.getResourceUuid();
        Long bizId = UserCtxHolder.getUserUid();
        if(! bizId.equals(ctx.getBizId())) throw new ForbiddenException();
        // Set up new resource
        Resource newRes = resourceRepository.findByUuidAndStatus(resourceUuid, ResourceStatus.UPLOAD_PENDING)
                .orElseThrow(() -> new ResourceReferenceInvalidException(resourceUuid));
        cloudFileService.setAndValidResourceForCallback(
                newRes,
                CloudFSRoot.USER,
                ResourceStatus.ACTIVE,
                ResourceType.IMAGE
        );
        resourceRepository.save(newRes);
        // Update old resource status to orphan if exists
        String newResourceUuid = contentAuditService
                .getLinkedResourcesUuids(bizId, TargetType.USER_AVATAR).getFirst();
        if (newResourceUuid != null) {
            resourceRepository.updateStatusTo(ResourceStatus.ORPHAN, newResourceUuid);
        }
        // Submit audit task
        AuditPayload payload = new ImageAuditPayload(newRes);
        contentAuditService.handleUserSubmit(
                bizId,
                TargetType.USER_AVATAR,
                payload,
                List.of(resourceUuid)
        );
    }

}
