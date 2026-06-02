package org.waterwood.waterfunservice.service.user;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;
import org.waterwood.common.CloudFSRoot;
import org.waterwood.common.io.FileExtension;
import org.waterwood.common.io.ResourceType;
import org.waterwood.utils.StringUtil;
import org.waterwood.waterfunservice.api.BizType;
import org.waterwood.waterfunservice.api.UploadContext;
import org.waterwood.waterfunservice.api.request.UploadPolicyReq;
import org.waterwood.waterfunservicecore.api.req.CloudPutCallbackReq;
import org.waterwood.waterfunservicecore.api.resp.PresignedResp;
import org.waterwood.waterfunservicecore.entity.resource.AuditResource;
import org.waterwood.waterfunservicecore.entity.audit.AuditTask;
import org.waterwood.waterfunservicecore.entity.audit.AuditStatus;
import org.waterwood.waterfunservicecore.entity.resource.Resource;
import org.waterwood.waterfunservicecore.entity.resource.ResourceStatus;
import org.waterwood.waterfunservicecore.exception.io.IllegalUploadCountException;
import org.waterwood.waterfunservicecore.exception.io.UnsupportedFileExtension;
import org.waterwood.waterfunservicecore.exception.reference.ResourceReferenceInvalidException;
import org.waterwood.waterfunservicecore.infrastructure.persistence.ResourceRepository;
import org.waterwood.waterfunservicecore.infrastructure.persistence.audit.AuditTaskRepository;
import org.waterwood.waterfunservicecore.entity.audit.TargetType;
import org.waterwood.waterfunservicecore.infrastructure.persistence.audit.AuditTaskResourceRepository;
import org.waterwood.waterfunservicecore.infrastructure.persistence.user.UserRepository;
import org.waterwood.waterfunservicecore.infrastructure.utils.context.UserCtxHolder;
import org.waterwood.waterfunservicecore.services.sys.storage.CloudFileService;
import org.waterwood.waterfunservicecore.infrastructure.utils.BizUploadPayload;
import org.waterwood.waterfunservicecore.infrastructure.utils.CosKeyPathGenerator;

import java.time.Instant;
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
    @Transactional
    @Override
    public List<PresignedResp> handleUserAvatarUpload(UploadPolicyReq request) {
        Long userUid = UserCtxHolder.getUserUid();
        if(request.getExts().size() != 1){
            throw new IllegalUploadCountException(1);
        }
        FileExtension ext = FileExtension.fromExt(request.getExts().getFirst());
        if(! TargetType.USER_AVATAR.isAllowed(ext)){
            throw new UnsupportedFileExtension(ext.getExt(), request.getExts().getFirst());
        }

        UUID resourceUUID = UUID.randomUUID();
        BizUploadPayload payload = UploadContext.<Long>builder()
                .bizId(userUid)
                .bizType(request.getBizType())
                .resourceUuid(resourceUUID.toString().replace("-", ""))
                .build()
                .toPayload();
        String cosPath = CosKeyPathGenerator.ofUser(userUid, resourceUUID, ext);
        resourceRepository.save(cloudFileService.CreateAndSetUpUploadRes(
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
    public void uploadAvatarCallback(CloudPutCallbackReq req, UploadContext<Long> ctx) {
        Assert.isTrue(ctx.getBizType() == BizType.AVATAR,
                "Invalid payload for avatar upload callback: " + ctx.getBizType());
        String resourceUuid = ctx.getResourceUuid();
        AuditTask task = auditTaskRepository
                .findByTargetIdAndTargetTypeAndStatus(String.valueOf(resourceUuid), TargetType.USER_AVATAR,AuditStatus.PENDING)
                .orElseGet(() -> {
                    AuditTask newTask = new AuditTask();
                    newTask.setTargetId(UserCtxHolder.getUserUid().toString());
                    newTask.setSubmitAt(Instant.now());
                    newTask.setTargetType(TargetType.USER_AVATAR);
                    newTask.setSubmitter(userRepository.getReferenceById(UserCtxHolder.getUserUid()));
                    return newTask;
                });

        auditTaskRepository.save(task);
        Resource res;
        AuditResource auditRes = auditTaskResourceRepository
                .findByTaskId(task.getId())
                .orElseGet(() -> {
                    AuditResource ar = new AuditResource();
                    ar.setTask(task);
                    return ar;
                });
        res = auditRes.getResource();
        if(res != null){ // old resource
            // clean old pending cloud resource if exists, to ensure no floating resource
            if (res.getResourceKey() != null) {
                final String oldKey = res.getResourceKey();
                TransactionSynchronizationManager.registerSynchronization(
                        new TransactionSynchronization() {
                            @Override
                            public void afterCommit() {
                                cloudFileService.removeFile(CloudFSRoot.USER, oldKey);
                            }
                        }
                );
            }
            res.setStatus(ResourceStatus.DELETED);
        }

        res = resourceRepository.findByUuidAndStatus(resourceUuid, ResourceStatus.UPLOAD_PENDING)
                .orElseThrow(() -> new ResourceReferenceInvalidException(resourceUuid));
        auditRes.setResource(res);

        cloudFileService.setAndValidResourceForCallback(
                res,
                CloudFSRoot.USER,
                ResourceStatus.ACTIVE,
                ResourceType.IMAGE
        );

        resourceRepository.save(res);
        auditTaskResourceRepository.save(auditRes);
    }

}
