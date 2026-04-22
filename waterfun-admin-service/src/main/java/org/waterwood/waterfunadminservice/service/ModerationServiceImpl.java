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
import org.waterwood.common.CloudStorageRootKey;
import org.waterwood.common.RabbitConstants;
import org.waterwood.utils.CollectionUtil;
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
import org.waterwood.waterfunservicecore.entity.audit.task.AuditTask;
import org.waterwood.waterfunservicecore.entity.audit.AuditStatus;
import org.waterwood.waterfunservicecore.entity.audit.resource.AuditTaskResource;
import org.waterwood.waterfunservicecore.entity.user.User;
import org.waterwood.waterfunservicecore.exception.NotFoundException;
import org.waterwood.waterfunservicecore.infrastructure.persistence.audit.AuditTaskRepository;
import org.waterwood.waterfunservicecore.infrastructure.persistence.audit.AuditTaskResourceRepository;
import org.waterwood.waterfunservicecore.infrastructure.utils.context.UserCtxHolder;
import org.waterwood.waterfunservicecore.services.sys.storage.CloudFileService;
import org.waterwood.waterfunservicecore.services.user.UserCoreService;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

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

    @Override
    public Page<AuditTask> listTasks(Specification<AuditTask> spec, Pageable pageable) {
        return auditTaskRepository.findAll(spec, pageable);
    }

    @Override
    public Page<ModerateTaskResponse> listTasksWithPayload(Specification<AuditTask> spec, Pageable pageable) {
        Page<AuditTask> tasks = listTasks(spec, pageable);
        List<Long> taskIds = tasks.getContent().stream().map(AuditTask::getId).toList();
        Map<Long, List<AuditTaskResource>> taskAndResources = loadTaskResources(taskIds);
        return tasks.map(task -> {
            ModerateTaskResponse resp = auditTaskMapper.toModerateTaskResponse(task);
            resp.setContent(task.getContent());
            List<AuditTaskResource> resources = taskAndResources.getOrDefault(task.getId(), Collections.emptyList());
            resp.setPayload(buildPayload(task, resources));
            return resp;
        });
    }

    @Override
    public List<ModerationResourceRes> listTaskResources(Long taskId) {
        if (!auditTaskRepository.existsById(taskId)) {
            throw new NotFoundException("Task UID: " + taskId);
        }
        List<AuditTaskResource> resources = auditTaskResourceRepository.findAllByTask_IdOrderBySortNoAsc(taskId);
        return resources.stream().map(this::toModerationResourceRes).toList();
    }

    @Override
    public Page<ModerationResourceRes> listResourcesWithPayload(Specification<AuditTaskResource> spec, Pageable pageable) {
        return auditTaskResourceRepository.findAll(spec, pageable).map(this::toModerationResourceRes);
    }

    @Override
    public ModerationResourceRes getTaskResource(Long resourceId) {
        AuditTaskResource resource = auditTaskResourceRepository.findById(resourceId).orElseThrow(
                () -> new NotFoundException("Audit Resource UID: " + resourceId)
        );
        return toModerationResourceRes(resource);
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
        AuditTask task = auditTaskRepository.findById(id).orElseThrow(
                () -> new NotFoundException("Task UID: " + id)
        );
        User auditor = userCoreService.getUser(UserCtxHolder.getUserUid());
        task.setAuditor(auditor);
        task.setStatus(AuditStatus.APPROVED);
        auditTaskRepository.save(task);
        sendMessage(task);
    }

    @Override
    public void reject(Long id, ModerateRejectRequest req) {
        AuditTask task = auditTaskRepository.findById(id).orElseThrow(
                () -> new NotFoundException("Task UID: " + id)
        );
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
        AuditTaskResource resource = auditTaskResourceRepository.findByIdAndStatus(resourceId, AuditStatus.PENDING).orElseThrow(
                () -> new NotFoundException("Pending audit resource UID: " + resourceId)
        );
        User auditor = userCoreService.getUser(UserCtxHolder.getUserUid());
        resource.setStatus(AuditStatus.APPROVED);
        resource.setAuditor(auditor);
        resource.setAuditAt(Instant.now());
        resource.setRejectType(null);
        resource.setRejectReason(null);
        auditTaskResourceRepository.save(resource);
        aggregateTaskStatus(resource.getTask(), resource);
    }

    @Transactional
    @Override
    public void rejectResource(Long resourceId, ModerateRejectRequest req) {
        AuditTaskResource resource = auditTaskResourceRepository.findByIdAndStatus(resourceId, AuditStatus.PENDING).orElseThrow(
                () -> new NotFoundException("Pending audit resource UID: " + resourceId)
        );
        User auditor = userCoreService.getUser(UserCtxHolder.getUserUid());
        resource.setStatus(AuditStatus.REJECTED);
        resource.setAuditor(auditor);
        resource.setAuditAt(Instant.now());
        resource.setRejectType(req.getRejectType());
        resource.setRejectReason(req.getRejectReason());
        auditTaskResourceRepository.save(resource);
        aggregateTaskStatus(resource.getTask(), resource);
    }

    private void aggregateTaskStatus(AuditTask task, AuditTaskResource lastUpdated) {
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

    private Map<Long, List<AuditTaskResource>> loadTaskResources(List<Long> taskIds) {
        if (CollectionUtil.isEmpty(taskIds)) {
            return Collections.emptyMap();
        }
        List<AuditTaskResource> resources = auditTaskResourceRepository.findAllByTask_IdInOrderBySortNoAsc(taskIds);
        Map<Long, List<AuditTaskResource>> grouped = new HashMap<>();
        resources.forEach(resource -> {
            Long taskId = resource.getTask().getId();
            grouped.computeIfAbsent(taskId, ignored -> new ArrayList<>()).add(resource);
        });
        return grouped;
    }

    private ModerationTaskPayloadRes buildPayload(AuditTask task, List<AuditTaskResource> resources) {
        if (CollectionUtil.isEmpty(resources)) {
            return new ModerationTaskPayloadRes(
                    ModerationTaskPayloadRes.PayloadType.PLAIN_TEXT,
                    null,
                    List.of(),
                    task.getContent()
            );
        }
        if (resources.size() == 1) {
            ModerationResourceRes single = toModerationResourceRes(resources.get(0));
            return new ModerationTaskPayloadRes(
                    ModerationTaskPayloadRes.PayloadType.SINGLE_RESOURCE,
                    single,
                    List.of(single),
                    task.getContent()
            );
        }
        List<ModerationResourceRes> items = resources.stream().map(this::toModerationResourceRes).toList();
        String rendered = renderContent(task.getContent(), items);
        return new ModerationTaskPayloadRes(
                ModerationTaskPayloadRes.PayloadType.RICH_TEXT,
                null,
                items,
                rendered
        );
    }

    private String renderContent(String content, List<ModerationResourceRes> resources) {
        if (content == null) {
            return null;
        }
        String rendered = content;
        for (ModerationResourceRes resource : resources) {
            String url = resource.getPresignedUrl().getUrl();
            if (resource.getPlaceholder() == null || url == null) {
                continue;
            }
            rendered = rendered
                    .replace("{{" + resource.getPlaceholder() + "}}", url)
                    .replace(resource.getPlaceholder(), url);
        }
        return rendered;
    }

    private ModerationResourceRes toModerationResourceRes(AuditTaskResource resource) {
        CloudResPresignedUrlResp urlResp = null;
        try {
            urlResp = cloudFileService.getReadUrlCached( CloudStorageRootKey.TEMP,
                    resource.getResourceKey(),
                    "audit-res-" + resource.getId(),
                    resource.getTask().getTargetType());
        } catch (Exception e) {
            log.debug("Failed generating preview url for resource {}, key={}, err={}", resource.getId(), resource.getResourceKey(), e.getMessage());
        }
        ModerationResourceRes res = auditTaskResourceMapper.toModerationResourceRes(resource);
        res.setPresignedUrl(urlResp);
        return res;
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
}
