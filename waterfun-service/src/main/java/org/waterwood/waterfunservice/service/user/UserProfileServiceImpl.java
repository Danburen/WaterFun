package org.waterwood.waterfunservice.service.user;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.waterwood.api.BaseResponseCode;
import org.waterwood.common.CloudStorageRootKey;
import org.waterwood.common.KeyConstants;
import org.waterwood.common.exceptions.BizException;
import org.waterwood.common.io.FileExtension;
import org.waterwood.common.io.ResourceType;
import org.waterwood.common.io.SimpleCloudObject;
import org.waterwood.utils.PathUtil;
import org.waterwood.waterfunservice.api.BizType;
import org.waterwood.waterfunservicecore.api.req.CloudPutCallbackReq;
import org.waterwood.waterfunservicecore.api.resp.PresignedResp;
import org.waterwood.waterfunservicecore.entity.audit.AuditTaskResource;
import org.waterwood.waterfunservicecore.entity.audit.task.AuditTask;
import org.waterwood.waterfunservicecore.entity.audit.AuditStatus;
import org.waterwood.waterfunservicecore.infrastructure.RedisHelper;
import org.waterwood.waterfunservicecore.infrastructure.persistence.audit.AuditTaskRepository;
import org.waterwood.waterfunservicecore.entity.audit.task.MediaResourceType;
import org.waterwood.waterfunservicecore.infrastructure.persistence.audit.AuditTaskResourceRepository;
import org.waterwood.waterfunservicecore.infrastructure.persistence.user.UserRepository;
import org.waterwood.waterfunservicecore.infrastructure.utils.context.UserCtxHolder;
import org.waterwood.waterfunservicecore.services.sys.storage.CloudFileService;
import org.waterwood.waterfunservicecore.services.sys.storage.CloudFileType;
import org.waterwood.waterfunservicecore.utils.BizPayload;
import org.waterwood.waterfunservicecore.utils.BizTargetIdPackager;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class UserProfileServiceImpl implements UserProfileService {
    private final AuditTaskRepository auditTaskRepository;
    private final CloudFileService cloudFileService;
    private final RedisHelper redisHelper;
    private final UserRepository userRepository;
    private final AuditTaskResourceRepository auditTaskResourceRepository;

    @Override
    public PresignedResp getUploadPolicyAndSubmitAvatar(Long userUid, FileExtension fileExtension) {
        if(fileExtension == null){
            throw new BizException(BaseResponseCode.NEED_FILE_TYPE);
        }
        BizPayload payload = BizTargetIdPackager.ofUser(userUid, BizType.AVATAR.name());
        
        String path = PathUtil.getUniquePathFile(fileExtension.getExt());
        return cloudFileService.buildPutPolicyWithBiz(CloudStorageRootKey.TEMP, path,payload);
    }

    @Transactional
    @Override
    public void uploadAvatarCallback(CloudPutCallbackReq req, BizPayload payload) {;
        Assert.isTrue(payload.getBiz().equals(KeyConstants.USER) && KeyConstants.AVATAR.equals(payload.getType()),
                "Invalid payload for avatar upload callback");
        String targetId = payload.getBizId();
        AuditTask task = auditTaskRepository.findByTargetIdAndStatus(targetId, AuditStatus.PENDING);
        SimpleCloudObject obj = cloudFileService.detectAndAssertCloudFile(CloudStorageRootKey.TEMP, payload.getCosKey(), CloudFileType.IMAGE);
        if(task == null){
            task = new AuditTask();
            task.setTargetId(targetId);
            task.setSubmitAt(Instant.now());
            task.setTargetType(MediaResourceType.USER_AVATAR);
            task.setSubmitter(
                    userRepository.findById(UserCtxHolder.getUserUid())
                            .orElseThrow(() -> new BizException(BaseResponseCode.USER_NOT_FOUND))
            );
        }
        auditTaskRepository.save(task);
        auditTaskResourceRepository.deleteByTaskId(task.getId());
        AuditTaskResource res = new AuditTaskResource();
        res.setTask(task);
        res.setResourceKey(obj.getKey());
        res.setResourceType(ResourceType.IMAGE);
        res.setMimeType(obj.getFileMeta().getMimeType());
        res.setSizeBytes(obj.getFileMeta().getSize());
        task.setUpdatedAt(Instant.now());
        auditTaskResourceRepository.save(res);
    }
}
