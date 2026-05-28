package org.waterwood.waterfunservice.service.user;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;
import org.waterwood.api.BaseResponseCode;
import org.waterwood.common.CloudFSRoot;
import org.waterwood.common.KeyConstants;
import org.waterwood.common.exceptions.BizException;
import org.waterwood.common.io.FileExtension;
import org.waterwood.common.io.ResourceType;
import org.waterwood.common.io.SimpleCloudObject;
import org.waterwood.waterfunservice.api.BizType;
import org.waterwood.waterfunservice.api.request.UploadPolicyReq;
import org.waterwood.waterfunservicecore.api.req.CloudPutCallbackReq;
import org.waterwood.waterfunservicecore.api.resp.PresignedResp;
import org.waterwood.waterfunservicecore.entity.resource.AuditResource;
import org.waterwood.waterfunservicecore.entity.audit.task.AuditTask;
import org.waterwood.waterfunservicecore.entity.audit.AuditStatus;
import org.waterwood.waterfunservicecore.entity.resource.Resource;
import org.waterwood.waterfunservicecore.entity.resource.ResourceStatus;
import org.waterwood.waterfunservicecore.entity.resource.SourceType;
import org.waterwood.waterfunservicecore.exception.NotFoundException;
import org.waterwood.waterfunservicecore.exception.io.IllegalUploadCountException;
import org.waterwood.waterfunservicecore.exception.io.UnsupportedFileExtension;
import org.waterwood.waterfunservicecore.infrastructure.persistence.ResourceRepository;
import org.waterwood.waterfunservicecore.infrastructure.RedisHelper;
import org.waterwood.waterfunservicecore.infrastructure.persistence.audit.AuditTaskRepository;
import org.waterwood.waterfunservicecore.entity.audit.task.TargetType;
import org.waterwood.waterfunservicecore.infrastructure.persistence.audit.AuditTaskResourceRepository;
import org.waterwood.waterfunservicecore.infrastructure.persistence.user.UserRepository;
import org.waterwood.waterfunservicecore.infrastructure.utils.context.UserCtxHolder;
import org.waterwood.waterfunservicecore.services.sys.storage.CloudFileService;
import org.waterwood.waterfunservicecore.services.sys.storage.CloudFileType;
import org.waterwood.waterfunservicecore.utils.BizUploadPayload;
import org.waterwood.waterfunservicecore.utils.BizTargetIdPackager;
import org.waterwood.waterfunservicecore.utils.CosKeyPathGenerator;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserProfileServiceImpl implements UserProfileService {
    private final AuditTaskRepository auditTaskRepository;
    private final CloudFileService cloudFileService;
    private final RedisHelper redisHelper;
    private final UserRepository userRepository;
    private final AuditTaskResourceRepository auditTaskResourceRepository;
    private final ResourceRepository resourceRepository;

    @Transactional
    @Override
    public void uploadAvatarCallback(CloudPutCallbackReq req, BizUploadPayload payload) {;
        Assert.isTrue(payload.getBiz().equals(KeyConstants.USER) && KeyConstants.AVATAR.equals(payload.getType()),
                "Invalid payload for avatar upload callback");
        SimpleCloudObject obj = cloudFileService.detectAndAssertCloudFile(payload.getCosKey(), CloudFileType.IMAGE);
        AuditTask task = auditTaskRepository
                .findByTargetIdAndTargetTypeAndStatus(payload.getBizId(), TargetType.USER_AVATAR,AuditStatus.PENDING)
                .orElseGet(() -> {
                    AuditTask newTask = new AuditTask();
                    newTask.setTargetId(payload.getBizId());
                    newTask.setSubmitAt(Instant.now());
                    newTask.setTargetType(TargetType.USER_AVATAR);
                    newTask.setSubmitter(userRepository.getReferenceById(UserCtxHolder.getUserUid()));
                    return newTask;
                });

        auditTaskRepository.save(task);

        AuditResource auditRes = auditTaskResourceRepository
                .findByTaskId(task.getId())
                .orElseGet(() -> {
                    AuditResource ar = new AuditResource();
                    ar.setTask(task);
                    return auditTaskResourceRepository.save(ar);
                });
        Resource res = auditRes.getResource();
        if(res == null) throw new NotFoundException("Audit resource not found: " + auditRes.getId());
        // clean old pending cloud resource if exists, to ensure no floating resource
        auditTaskResourceRepository.save(auditRes);
        if ( res.getResourceKey() != null) {
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

        res.setResourceKey(obj.getKey());
        res.setResourceType(ResourceType.IMAGE);
        res.setMimeType(obj.getFileMeta().getMimeType());
        res.setSizeBytes(obj.getFileMeta().getSize());
        res.setUploaderId(UserCtxHolder.getUserUid());
        res.setSourceType(SourceType.USER_UPLOADED);
        res.setStatus(ResourceStatus.ACTIVE);
        resourceRepository.save(res);
    }

    @Transactional
    @Override
    public List<PresignedResp> handleUserAvatarUpload(UploadPolicyReq request) {
        Long userUid = UserCtxHolder.getUserUid();
        if(request.getExts().size() != 1){
            throw new IllegalUploadCountException(1);
        }
        FileExtension ext = FileExtension.fromExt(request.getExts().getFirst());
        if(TargetType.USER_AVATAR.isAllowed(ext)){
            throw new UnsupportedFileExtension(ext.getExt(), request.getExts().getFirst());
        }

        UUID resourceUUID = UUID.randomUUID();
        BizUploadPayload payload = BizTargetIdPackager.ofUser(userUid, BizType.AVATAR.name(), resourceUUID);
        String cosPath = CosKeyPathGenerator.ofUser(userUid, resourceUUID, ext);

        Resource res = new Resource();
        res.setUuid(resourceUUID.toString());
        res.setResourceKey(cosPath);
        res.setResourceType(ResourceType.IMAGE);
        res.setUploaderId(UserCtxHolder.getUserUid());
        resourceRepository.save(res);

        return List.of(cloudFileService.buildPutPolicyWithPayload(
                CloudFSRoot.USER,
                cosPath,
                payload)
        );
    }
}
