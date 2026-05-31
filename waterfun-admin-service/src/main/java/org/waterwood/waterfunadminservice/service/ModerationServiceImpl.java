package org.waterwood.waterfunadminservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.waterwood.api.VO.BatchResult;
import org.waterwood.common.CloudFSRoot;
import org.waterwood.common.RabbitConstants;
import org.waterwood.common.io.FileMeta;
import org.waterwood.common.io.FileProbeResult;
import org.waterwood.utils.CollectionUtil;
import org.waterwood.utils.JsonUtil;
import org.waterwood.utils.StringUtil;
import org.waterwood.waterfunadminservice.api.request.content.audit.BatchModerateRejectRequest;
import org.waterwood.waterfunadminservice.api.request.content.audit.BatchModerateRequest;
import org.waterwood.waterfunadminservice.api.request.content.audit.ModerateRejectRequest;
import org.waterwood.waterfunadminservice.api.response.ModerateTaskResponse;
import org.waterwood.waterfunadminservice.api.response.content.audit.ModerationResourceRes;
import org.waterwood.waterfunadminservice.api.response.content.audit.ModerationTaskPayloadRes;
import org.waterwood.waterfunadminservice.infrastructure.mapper.AuditTaskMapper;
import org.waterwood.waterfunadminservice.infrastructure.mapper.AuditTaskResourceMapper;
import org.waterwood.waterfunservicecore.api.message.ModerationConsumerMessage;
import org.waterwood.waterfunservicecore.api.resp.CloudResPresignedUrlResp;
import org.waterwood.waterfunservicecore.entity.audit.TargetType;
import org.waterwood.waterfunservicecore.entity.resource.AuditResource;
import org.waterwood.waterfunservicecore.entity.audit.AuditTask;
import org.waterwood.waterfunservicecore.entity.audit.AuditStatus;
import org.waterwood.waterfunservicecore.entity.resource.Resource;
import org.waterwood.waterfunservicecore.entity.resource.ResourceStatus;
import org.waterwood.waterfunservicecore.entity.user.User;
import org.waterwood.waterfunservicecore.exception.notfound.AuditTaskNotFoundException;
import org.waterwood.waterfunservicecore.exception.notfound.AuditTaskResourceNotFoundException;
import org.waterwood.waterfunservicecore.exception.reference.AuditTaskReferenceInvalidException;
import org.waterwood.waterfunservicecore.infrastructure.persistence.ResourceRepository;
import org.waterwood.waterfunservicecore.infrastructure.persistence.audit.AuditTaskRepository;
import org.waterwood.waterfunservicecore.infrastructure.persistence.audit.AuditTaskResourceRepository;
import org.waterwood.waterfunservicecore.infrastructure.utils.context.UserCtxHolder;
import org.waterwood.waterfunservicecore.services.sys.storage.CloudFileService;
import org.waterwood.waterfunservicecore.services.user.UserCoreService;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ModerationServiceImpl implements ModerationService {

    private final AuditTaskRepository auditTaskRepository;
    private final AuditTaskResourceRepository auditTaskResourceRepository;
    private final UserCoreService userCoreService;
    private final CloudFileService cloudFileService;
    private final AuditTaskMapper auditTaskMapper;
    private final RabbitTemplate rabbitTemplate;
    private final AuditTaskResourceMapper auditTaskResourceMapper;
    private final ResourceRepository resourceRepository;

    @Override
    public Page<AuditTask> listTasks(Specification<AuditTask> spec, Pageable pageable) {
        return auditTaskRepository.findAll(spec, pageable);
    }

    @Override
    public Page<ModerateTaskResponse> listTasksWithPayload(Specification<AuditTask> spec, Pageable pageable) {
        Page<AuditTask> tasks = listTasks(spec, pageable);
        List<Long> taskIds = tasks.getContent().stream().map(AuditTask::getId).toList();
        Map<Long, List<AuditResource>> taskAndResources = loadTaskResources(taskIds);
        return tasks.map(task -> {
            ModerateTaskResponse resp = auditTaskMapper.toModerateTaskResponse(task);
            List<AuditResource> auditResources = taskAndResources.getOrDefault(task.getId(), Collections.emptyList());
            resp.setPayload(buildPayload(task, auditResources));
            return resp;
        });
    }

    @Override
    public List<ModerationResourceRes> listTaskResources(Long taskId) {
        if (!auditTaskRepository.existsById(taskId)) {
            throw new AuditTaskReferenceInvalidException(taskId);
        }
        List<AuditResource> auditResources = auditTaskResourceRepository.findAllByTask_IdOrderBySortNoAsc(taskId);
        return auditResources.stream().map(this::toModerationResourceRes).toList();
    }

    @Override
    public Page<ModerationResourceRes> listResourcesWithPayload(Specification<AuditResource> spec, Pageable pageable) {
        return auditTaskResourceRepository.findAll(spec, pageable).map(this::toModerationResourceRes);
    }

    @Override
    public ModerationResourceRes getTaskResource(Long resourceId) {
        AuditResource auditResource = auditTaskResourceRepository.findById(resourceId)
                .orElseThrow(AuditTaskResourceNotFoundException::new);
        return toModerationResourceRes(auditResource);
    }

    @Override
    public BatchResult approveAll(BatchModerateRequest req) {
        AtomicInteger success = new AtomicInteger();
        if(CollectionUtil.isNotEmpty(req.getAuditTaskIds())){
            List<AuditTask> availableTasks = auditTaskRepository.findAllByIdInAndStatus(
                    req.getAuditTaskIds(), AuditStatus.PENDING
            );
            User auditor = userCoreService.getUserByUid(UserCtxHolder.getUserUid());
            availableTasks.forEach(t -> {
                t.setStatus(AuditStatus.APPROVED);
                t.setAuditor(auditor);
                success.getAndIncrement();
            });

            auditTaskRepository.saveAll(availableTasks);
        }
        return BatchResult.ofNullable(req.getAuditTaskIds(), success.get());
    }

    @Override
    public BatchResult rejectAll(BatchModerateRejectRequest req) {
        AtomicInteger success = new AtomicInteger();
        if(CollectionUtil.isNotEmpty(req.getAuditTaskIds())){
            List<AuditTask> availableTasks = auditTaskRepository.findAllByIdInAndStatus(
                    req.getAuditTaskIds(), AuditStatus.PENDING
            );
            User auditor = userCoreService.getUserByUid(UserCtxHolder.getUserUid());
            availableTasks.forEach(t -> {
                t.setStatus(AuditStatus.REJECTED);
                t.setAuditor(auditor);
                t.setRejectType(req.getRejectType());
                t.setRejectReason(req.getRejectReason());
                success.getAndIncrement();
            });
            auditTaskRepository.saveAll(availableTasks);
        }
        return BatchResult.ofNullable(req.getAuditTaskIds(), success.get());
    }

    @Override
    public void approve(Long id) {
        AuditTask task = auditTaskRepository.findById(id)
                .orElseThrow(AuditTaskNotFoundException::new);
        User auditor = userCoreService.getUser(UserCtxHolder.getUserUid());
        task.setAuditor(auditor);
        task.setStatus(AuditStatus.APPROVED);
        auditTaskRepository.save(task);
        sendMessage(task);
    }

    @Override
    public void reject(Long id, ModerateRejectRequest req) {
        AuditTask task = auditTaskRepository.findById(id)
                .orElseThrow(AuditTaskNotFoundException::new);
        User auditor = userCoreService.getUser(UserCtxHolder.getUserUid());
        task.setStatus(AuditStatus.REJECTED);
        task.setAuditor(auditor);
        task.setRejectType(req.getRejectType());
        task.setRejectReason(req.getRejectReason());
        auditTaskRepository.save(task);
        sendMessage(task);
    }

    @Transactional
    @Override
    public void approveResource(Long resourceId) {
        AuditResource auditResource = auditTaskResourceRepository.findByIdAndStatus(resourceId, AuditStatus.PENDING)
                .orElseThrow(() -> new AuditTaskReferenceInvalidException(resourceId));
        User auditor = userCoreService.getUser(UserCtxHolder.getUserUid());
        auditResource.setStatus(AuditStatus.APPROVED);
        auditResource.setAuditor(auditor);
        auditResource.setAuditAt(Instant.now());
        auditResource.setRejectType(null);
        auditResource.setRejectReason(null);
        auditTaskResourceRepository.save(auditResource);
        aggregateTaskStatus(auditResource.getTask(), auditResource);
    }

    @Transactional
    @Override
    public void rejectResource(Long resourceId, ModerateRejectRequest req) {
        AuditResource auditResource = auditTaskResourceRepository.findByIdAndStatus(resourceId, AuditStatus.PENDING)
                .orElseThrow(() -> new AuditTaskReferenceInvalidException(resourceId));
        User auditor = userCoreService.getUser(UserCtxHolder.getUserUid());
        auditResource.setStatus(AuditStatus.REJECTED);
        auditResource.setAuditor(auditor);
        auditResource.setAuditAt(Instant.now());
        auditResource.setRejectType(req.getRejectType());
        auditResource.setRejectReason(req.getRejectReason());
        auditTaskResourceRepository.save(auditResource);
        aggregateTaskStatus(auditResource.getTask(), auditResource);
    }

    private void aggregateTaskStatus(AuditTask task, AuditResource lastUpdated) {
        long rejectedCount = auditTaskResourceRepository.countByTask_IdAndStatus(task.getId(), AuditStatus.REJECTED);
        long pendingCount = auditTaskResourceRepository.countByTask_IdAndStatus(task.getId(), AuditStatus.PENDING);
        AuditStatus prev = task.getStatus();
        if (rejectedCount > 0) {
            task.setStatus(AuditStatus.REJECTED);
            task.setRejectType(lastUpdated.getRejectType());
            task.setRejectReason(lastUpdated.getRejectReason());
        } else if (pendingCount == 0) {
            task.setStatus(AuditStatus.APPROVED);
            task.setRejectType(null);
            task.setRejectReason(null);
        } else {
            task.setStatus(AuditStatus.PENDING);
        }
        task.setAuditor(lastUpdated.getAuditor());
        task.setAuditAt(Instant.now());
        task.setUpdatedAt(Instant.now());
        auditTaskRepository.save(task);
        if (prev != task.getStatus() && task.getStatus() != AuditStatus.PENDING) {
            sendMessage(task);
        }
    }

    private Map<Long, List<AuditResource>> loadTaskResources(List<Long> taskIds) {
        if (CollectionUtil.isEmpty(taskIds)) {
            return Collections.emptyMap();
        }
        List<AuditResource> auditResources = auditTaskResourceRepository.findAllByTask_IdInOrderBySortNoAsc(taskIds);
        Map<Long, List<AuditResource>> grouped = new HashMap<>();
        auditResources.forEach(resource -> {
            Long taskId = resource.getTask().getId();
            grouped.computeIfAbsent(taskId, ignored -> new ArrayList<>()).add(resource);
        });
        return grouped;
    }

    private ModerationTaskPayloadRes buildPayload(AuditTask task, List<AuditResource> auditResources) {
        String renderedContent;
        if(task.getContent() == null){
            renderedContent = "";
        } else {
            Set<String> resourceUuidsInContent = StringUtil.extractResUrls(task.getContent());
            List<String> cosKeys = resourceRepository.findByUuidInAndStatus(resourceUuidsInContent, ResourceStatus.ACTIVE)
                    .stream().map(Resource::getResourceKey).toList();
            Map<String, String> uuidCosKeyMap = resourceUuidsInContent.stream()
                    .collect(Collectors.toMap(k -> k, k -> k));
            renderedContent = StringUtil.replaceResPlaceholders(task.getContent(), uuidCosKeyMap);
        }
        if(CollectionUtil.isEmpty(auditResources)) {
            return new ModerationTaskPayloadRes(
                    ModerationTaskPayloadRes.PayloadType.PLAIN_TEXT,
                    null,
                    Collections.emptyList(),
                    renderedContent,
                    task.getContentFormat(),
                    null
            );
        }

        if (auditResources.size() == 1) {
            ModerationResourceRes single = toModerationResourceRes(auditResources.getFirst());
            return new ModerationTaskPayloadRes(
                    ModerationTaskPayloadRes.PayloadType.SINGLE_RESOURCE,
                    single,
                    List.of(single),
                    renderedContent,
                    task.getContentFormat(),
                    null
            );
        }

        List<ModerationResourceRes> items = auditResources.stream().map(this::toModerationResourceRes).toList();
        return new ModerationTaskPayloadRes(
                ModerationTaskPayloadRes.PayloadType.RICH_TEXT,
                null,
                items,
                renderedContent,
                task.getContentFormat(),
                null
        );
    }

    private ModerationResourceRes toModerationResourceRes(AuditResource auditResource) {
        CloudResPresignedUrlResp urlResp = null;
        try {
            urlResp = cloudFileService.getReadUrlCached(
                    getCloudFSRootByTargetType(auditResource.getTask().getTargetType()),
                    auditResource.getResource().getResourceKey(),
                    "audit-res-" + auditResource.getId(),
                    auditResource.getTask().getTargetType()
            );
        } catch (Exception e) {
            log.debug("Failed generating preview url for resource {}, key={}, err={}", auditResource.getId(), auditResource.getResource().getResourceKey(), e.getMessage());
        }
        ModerationResourceRes result = auditTaskResourceMapper.toModerationResourceRes(auditResource);
        Resource res = auditResource.getResource();
        result.setPresignedUrl(urlResp);
        result.setFileProbeResult(new FileProbeResult(
                res.getSizeBytes(),
                res.getMimeType(),
                JsonUtil.fromJson(res.getFileMeta(), FileMeta.class)
        ));
        return result;
    }


    private void sendMessage(AuditTask task){
        try{
            ModerationConsumerMessage message = auditTaskMapper.toModerationConsumerMessage(task);
            rabbitTemplate.convertAndSend(
                    RabbitConstants.MODERATION_EXCHANGE,
                    RabbitConstants.ROUTE_MODERATION_RESULT,
                    message
            );
        }catch (Exception e){
            log.info("Failed to send moderation result message for task UID: {}, error: {}", task.getId(), e.getMessage());
        }
    }

    private CloudFSRoot getCloudFSRootByTargetType(TargetType targetType) {
        return switch (targetType) {
            case USER_AVATAR -> CloudFSRoot.USER;
            case POST, POST_CONTENT_IMAGE, POST_COVERAGE_IMAGE -> CloudFSRoot.UPLOADS;
            default -> throw new IllegalArgumentException("Unsupported target type: " + targetType);
        };
    }
}
