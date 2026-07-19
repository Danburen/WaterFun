package org.waterwood.waterfunadminservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.MessageSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
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
import org.waterwood.waterfunadminservice.api.request.AuditResponse;
import org.waterwood.waterfunadminservice.api.request.ModerationBaseQuery;
import org.waterwood.waterfunadminservice.api.request.content.audit.BatchModerateRejectRequest;
import org.waterwood.waterfunadminservice.api.request.content.audit.BatchModerateRequest;
import org.waterwood.waterfunadminservice.api.request.content.audit.ModerateRejectRequest;
import org.waterwood.waterfunadminservice.api.response.ModerationStatsResp;
import org.waterwood.waterfunadminservice.api.response.content.audit.ModerationResourceRes;
import org.waterwood.waterfunadminservice.api.response.content.audit.SourceContext;
import org.waterwood.waterfunadminservice.api.response.content.audit.UserAuditStats;
import org.waterwood.waterfunadminservice.infrastructure.mapper.AuditTaskMapper;
import org.waterwood.waterfunadminservice.infrastructure.mapper.AuditTaskResourceMapper;
import org.waterwood.waterfunadminservice.service.content.PostService;
import org.waterwood.waterfunadminservice.service.user.UserAdminService;
import org.waterwood.waterfunservicecore.api.message.ModerationBatchMessage;
import org.waterwood.waterfunservicecore.api.message.ModerationConsumerMessage;
import org.waterwood.waterfunservicecore.api.moderation.AuditPayload;
import org.waterwood.waterfunservicecore.api.moderation.ImageAuditPayload;
import org.waterwood.waterfunservicecore.api.moderation.PostAuditPayload;
import org.waterwood.waterfunservicecore.api.moderation.ReplyPayload;
import org.waterwood.waterfunservicecore.api.resp.CloudResPresignedUrlResp;
import org.waterwood.waterfunservicecore.api.resp.user.UserBrief;
import org.waterwood.waterfunservicecore.infrastructure.persistence.PostRepository;
import org.waterwood.waterfunadminservice.api.response.user.UserAdminBrief;
import org.waterwood.waterfunservicecore.services.user.UserBriefService;
import org.waterwood.waterfunservicecore.entity.audit.*;
import org.waterwood.waterfunservicecore.entity.resource.AuditResource;
import org.waterwood.waterfunservicecore.entity.resource.Resource;
import org.waterwood.waterfunservicecore.entity.resource.ResourceStatus;
import org.waterwood.waterfunservicecore.entity.spec.AuditTaskSpec;
import org.waterwood.waterfunservicecore.entity.user.User;
import org.waterwood.waterfunservicecore.exception.notfound.AuditTaskNotFoundException;
import org.waterwood.waterfunservicecore.exception.notfound.AuditTaskResourceNotFoundException;
import org.waterwood.waterfunservicecore.exception.reference.AuditTaskReferenceInvalidException;
import org.waterwood.waterfunservicecore.exception.reference.AuditTaskResourceReferenceInvalid;
import org.waterwood.waterfunservicecore.infrastructure.persistence.ResourceRepository;
import org.waterwood.waterfunservicecore.infrastructure.persistence.audit.AuditTaskRepository;
import org.waterwood.waterfunservicecore.infrastructure.persistence.audit.AuditTaskResourceRepository;
import org.waterwood.waterfunservicecore.infrastructure.persistence.user.UserRepository;
import org.waterwood.waterfunservicecore.infrastructure.utils.context.UserCtxHolder;
import org.waterwood.waterfunservicecore.services.sys.storage.CloudFileService;
import org.waterwood.waterfunservicecore.services.user.UserCoreService;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(readOnly = true)
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
    private final UserRepository userRepository;
    private final UserBriefService userBriefService;
    private final PostRepository postRepository;
    private final MessageSource messageSource;
    private final UserAdminService userAdminService;
    private final PostService postService;


    @Override
    public List<ModerationResourceRes> listTaskResources(Long taskId) {
        if (!auditTaskRepository.existsById(taskId)) {
            throw new AuditTaskReferenceInvalidException(taskId);
        }
        List<AuditResource> auditResources = auditTaskResourceRepository
                .findAllByTaskId(taskId);
        return auditResources.stream().map(this::toModerationResourceRes).toList();
    }

    @Override
    public ModerationResourceRes getTaskResource(Long taskId, String resourceUuid) {
        AuditResource auditResource = auditTaskResourceRepository
                .findByTaskIdAndResourceUuid(taskId, resourceUuid)
                .orElseThrow(AuditTaskResourceNotFoundException::new);
        return toModerationResourceRes(auditResource);
    }

    @Override
    public BatchResult approveAll(BatchModerateRequest req) {
        int success = 0;
        List<AuditTask> tasks = new ArrayList<>();
        if(CollectionUtil.isNotEmpty(req.getAuditTaskIds())){
            List<Long> availableTaskIds = auditTaskRepository.findAllByIdInAndStatus(
                    req.getAuditTaskIds(), AuditStatus.PENDING
            ).stream().map(AuditTask::getId).toList();
            User u = userRepository.getReferenceById(UserCtxHolder.getUserUid());
            auditTaskResourceRepository.updateStatusAndRejectTypeAndAuditorAndAuditAtByTaskIdIn(
                    AuditStatus.APPROVED,
                    AuditType.CASCADE,
                    u,
                    Instant.now(),
                    availableTaskIds
            );
            tasks = auditTaskRepository.updateStatusAndAuditorAndAuditAtByIdInAndStatus(
                    AuditStatus.APPROVED,
                    u,
                    Instant.now(),
                    availableTaskIds,
                    AuditStatus.PENDING
            );
        }
        if(!tasks.isEmpty()){
            sendMessages(tasks);
        }
        return BatchResult.ofNullable(req.getAuditTaskIds(), tasks.size());
    }

    @Override
    public BatchResult rejectAll(BatchModerateRejectRequest req) {
        int success = 0;
        List<AuditTask> tasks = new ArrayList<>();
        if(CollectionUtil.isNotEmpty(req.getAuditTaskIds())){
            List<Long> availableTaskIds = auditTaskRepository.findAllByIdInAndStatus(
                    req.getAuditTaskIds(), AuditStatus.PENDING
            ).stream().map(AuditTask::getId).toList();
            User u = userRepository.getReferenceById(UserCtxHolder.getUserUid());
            auditTaskResourceRepository.updateStatusAndRejectTypeAndAuditorAndAuditAtByTaskIdIn(
                    AuditStatus.REJECTED,
                    AuditType.CASCADE,
                    u,
                    Instant.now(),
                    availableTaskIds
            );
             tasks = auditTaskRepository.updateStatusAndRejectTypeAndRejectReasonAndAuditorAndAuditAtByIdInAndStatus(
                    AuditStatus.REJECTED,
                    req.getRejectType(),
                    req.getRejectReason(),
                    u,
                    Instant.now(),
                    availableTaskIds,
                    AuditStatus.PENDING
            );
            success = tasks.size();
        }
        if(!tasks.isEmpty()){
            sendMessages(tasks);
        }
        return BatchResult.ofNullable(req.getAuditTaskIds(), success);
    }

    @Override
    @Transactional
    public List<ModerationResourceRes> approve(Long id) {
        AuditTask task = auditTaskRepository.findByIdAndStatus(id, AuditStatus.PENDING)
                .orElseThrow(AuditTaskNotFoundException::new);
        task.setAuditor(userRepository.getReferenceById(UserCtxHolder.getUserUid()));
        task.setStatus(AuditStatus.APPROVED);
        List<AuditResource> auditResources = auditTaskResourceRepository.findAllByTaskId(id);
        List<AuditResource> blocked = auditResources.stream()
                .filter(auditResource ->
                        auditResource.getStatus().equals(AuditStatus.REJECTED) ||
                                auditResource.getStatus().equals(AuditStatus.SUSPECT))
                .toList();
        if(!blocked.isEmpty()) {
            return blocked.stream().map(this::toModerationResourceRes).collect(Collectors.toList());
        }
        auditTaskRepository.save(task);
        sendMessage(task);
        return List.of();
    }

    @Override
    @Transactional
    public void reject(Long id, ModerateRejectRequest req) {
        AuditTask task = auditTaskRepository.findByIdAndStatus(id, AuditStatus.PENDING)
                .orElseThrow(AuditTaskNotFoundException::new);
        User auditor = userRepository.getReferenceById(UserCtxHolder.getUserUid());
        auditTaskResourceRepository.updateStatusAndRejectTypeAndAuditorAndAuditAtByTaskIdAndStatus(
                AuditStatus.REJECTED,
                AuditType.CASCADE,
                auditor,
                Instant.now(),
                task.getId(),
                AuditStatus.PENDING
        );
        task.setStatus(AuditStatus.REJECTED);
        task.setAuditor(auditor);
        task.setRejectType(req.getRejectType());
        task.setRejectReason(req.getRejectReason());
        auditTaskRepository.save(task);
        sendMessage(task);
    }

    @Transactional
    @Override
    public void approveResource(Long taskId, String resourceUuid) {
        AuditResource auditResource = auditTaskResourceRepository
                .findByTaskIdAndResourceUuidAndStatus(taskId, resourceUuid, AuditStatus.PENDING)
                .orElseThrow(() -> new AuditTaskResourceReferenceInvalid(resourceUuid));
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
    public void rejectResource(Long taskId, String resourceUuid, ModerateRejectRequest req) {
        AuditResource auditResource = auditTaskResourceRepository
                .findByTaskIdAndResourceUuidAndStatus(taskId, resourceUuid, AuditStatus.PENDING)
                .orElseThrow(() -> new AuditTaskResourceReferenceInvalid(resourceUuid));
        User auditor = userCoreService.getUser(UserCtxHolder.getUserUid());
        auditResource.setStatus(AuditStatus.REJECTED);
        auditResource.setAuditor(auditor);
        auditResource.setAuditAt(Instant.now());
        auditResource.setRejectType(req.getRejectType());
        auditResource.setRejectReason(req.getRejectReason());
        auditTaskResourceRepository.save(auditResource);
        aggregateTaskStatus(auditResource.getTask(), auditResource);
    }

    @Override
    public Page<AuditResponse<PostAuditPayload>> listPendingPostTasks(ModerationBaseQuery query, Pageable pageable) {
        return listTasksWithPayload(query, pageable, TargetType.POST, null, PostAuditPayload.class);
    }

    @Override
    public Page<AuditResponse<ImageAuditPayload>> listPendingImageTasks(ModerationBaseQuery query, Pageable pageable) {
        List<AuditResponse<ImageAuditPayload>> res = listTasksWithPayload(query, pageable, null, AuditContentFormat.IMAGE, ImageAuditPayload.class).getContent();
        Map<String, CloudResPresignedUrlResp> urlUuidRespMap = cloudFileService.batchGetReadPublicUrlCached(
                CloudFSRoot.UPLOADS,
                res.stream().collect(
                        Collectors.toMap(
                                r -> r.getPayload().getUuid(),
                                r -> r.getPayload().getResourceKey()
                        )
                ),
                TargetType.MODERATION_IMAGE
        );
        return new PageImpl<>(res, pageable, res.size()).map(r -> {;
            r.getPayload().setPresignedUrl(
                    urlUuidRespMap.get(r.getPayload().getUuid())
            );
            return r;
        });
    }

    @Override
    public Page<AuditResponse<ReplyPayload>> listPendingTextTasks(ModerationBaseQuery query, Pageable pageable) {
        return listTasksWithPayload(query, pageable, null, AuditContentFormat.TXT, ReplyPayload.class);
    }

    @Override
    public AuditResponse<PostAuditPayload> getPostTask(Long id) {
        return getTask(id, PostAuditPayload.class);
    }

    @Override
    public AuditResponse<ImageAuditPayload> getImageTask(Long id) {
        return getTask(id, ImageAuditPayload.class);
    }

    @Override
    public AuditResponse<ReplyPayload> getTextTask(Long id) {
        return getTask(id, ReplyPayload.class);
    }

    @Override
    public ModerationStatsResp getModerationStats(TargetType targetType) {
        Instant todayStart = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant();
        long pending;
        long todayApproved;
        long todayRejected;
        if (targetType != null) {
            pending = auditTaskRepository.countByStatusAndTargetType(AuditStatus.PENDING, targetType);
            todayApproved = auditTaskRepository.countByStatusAndTargetTypeAndAuditAtAfter(
                    AuditStatus.APPROVED, targetType, todayStart);
            todayRejected = auditTaskRepository.countByStatusAndTargetTypeAndAuditAtAfter(
                    AuditStatus.REJECTED, targetType, todayStart);
        } else {
            pending = auditTaskRepository.countByStatus(AuditStatus.PENDING);
            todayApproved = auditTaskRepository.countByStatusAndAuditAtAfter(AuditStatus.APPROVED, todayStart);
            todayRejected = auditTaskRepository.countByStatusAndAuditAtAfter(AuditStatus.REJECTED, todayStart);
        }
        return new ModerationStatsResp(pending, todayApproved, todayRejected);
    }

    @Override
    public UserAuditStats getUserAuditStats(Long userId) {
        int passed = (int) auditTaskRepository.countBySubmitterUidAndStatus(userId, AuditStatus.APPROVED);
        int rejected = (int) auditTaskRepository.countBySubmitterUidAndStatus(userId, AuditStatus.REJECTED);
        int total = passed + rejected;
        double passRate = total > 0 ? (double) passed / total * 100 : 0;
        return new UserAuditStats(passed, rejected, passRate);
    }

    private <T extends AuditPayload> Page<AuditResponse<T>> listTasksWithPayload(
            ModerationBaseQuery query, Pageable pageable,
            TargetType targetType, AuditContentFormat format,
            Class<T> type
    ) {
        Specification<AuditTask> spec = AuditTaskSpec.of(
                query.triggerType(), query.priority(), query.status(),
                query.submitterUid(), query.submitAtStart(), query.submitAtEnd(),
                targetType, format
        );
        Page<AuditTask> taskPage = auditTaskRepository.findAll(spec, pageable);
        List<AuditTask> res = taskPage.getContent();
        Map<Long, UserBrief> userBriefMap = userBriefService.queryForMapUserIdBriefMap(
                res.stream().map(task -> task.getSubmitter().getUid()).toList()
        );
        Map<Long, UserAdminBrief> userAdminBriefMap = userAdminService.batchGetUserAdminBrief(
                res.stream().map(task -> task.getSubmitter().getUid()).toList()
        );
        Map<Long, List<AuditResource>> taskIdAuditResListMap = auditTaskResourceRepository.findAllByTaskIdIn(
                res.stream().map(AuditTask::getId).toList()
        ).stream().collect(Collectors.groupingBy(t -> t.getTask().getId()));
        return res.stream().map(task -> {
            AuditResponse<T> resp = new AuditResponse<>();
            auditTaskMapper.toModerateTaskResponse(task, resp);
            resp.setPayload(parsePayload(task, type));
            resp.setLinkedResources(taskIdAuditResListMap.getOrDefault(task.getId(), List.of()).stream()
                    .map(this::toModerationResourceRes).toList());
            resp.setSubmitter(userAdminBriefMap.get(task.getSubmitter().getUid()));
            resp.setAuditor(userBriefMap.getOrDefault(
                    task.getAuditor() != null ? task.getAuditor().getUid() : null,
                    null
            ));
            resp.setSourceContext(resolveSourceContext(task, type));
            return resp;
        }).collect(Collectors.collectingAndThen(Collectors.toList(),
                list -> new PageImpl<>(list, pageable, taskPage.getTotalElements())));
    }

    private <T extends AuditPayload> AuditResponse<T> getTask(Long id, Class<T> type) {
        return auditTaskRepository.findById(id)
                .map(task -> {
                    AuditResponse<T> resp = new AuditResponse<>();
                    auditTaskMapper.toModerateTaskResponse(task, resp);
                    List<AuditResource> auditResources = auditTaskResourceRepository
                            .findAllByTaskId(task.getId());
                    resp.setPayload(parsePayload(task, type));
                    resp.setLinkedResources(auditResources.stream()
                            .map(this::toModerationResourceRes).toList());
                    resp.setSubmitter(userAdminService.getUserAdminBrief(task.getSubmitter().getUid()));
                    resp.setAuditor(task.getAuditor() != null ?
                            userBriefService.getUserBrief(task.getAuditor().getUid()) : null);
                    resp.setSourceContext(resolveSourceContext(task, type));
                    return resp;
                })
                .orElseThrow(AuditTaskNotFoundException::new);
    }

    private <T extends AuditPayload> SourceContext resolveSourceContext(AuditTask task, Class<T> type) {
        if (type == ReplyPayload.class) {
            ReplyPayload payload = JsonUtil.fromJson(task.getPayload(), ReplyPayload.class);
            if (payload != null && payload.getPostId() != null) {
                return postRepository.findPostAuthorIdTitleDOById(payload.getPostId())
                        .map(do_ -> new SourceContext(
                                postService.getPostBrief(payload.getPostId())
                        ))
                        .orElse(null);
            }
        }
        if (type == ImageAuditPayload.class) {
            return auditTaskResourceRepository
                    .findTaskByResourceUuidAndTaskTargetType(task.getTargetId(), TargetType.POST)
                    .stream().findFirst()
                    .map(postTask -> {
                        Long postId = Long.valueOf(postTask.getTargetId());
                        return postRepository.findPostAuthorIdTitleDOById(postId)
                                .map(do_ -> {
                                    return new SourceContext(postService.getPostBrief(postId));
                                })
                                .orElse(null);
                    })
                    .orElse(null);
        }
        return null;
    }

    private <T extends AuditPayload> T parsePayload(AuditTask task, Class<T> type) {
        AuditPayload payload = getPayload(task.getPayload(), task.getTargetType(), task.getFormat());
        if (payload == null || !type.isInstance(payload)) {
            throw new IllegalArgumentException(
                    "Task " + task.getId() + " is not of type " + type.getSimpleName()
                            + " (targetType=" + task.getTargetType()
                            + ", format=" + task.getFormat() + ")"
            );
        }
        return type.cast(payload);
    }

    private void aggregateTaskStatus(AuditTask task, AuditResource lastUpdated) {
        long rejectedCount = auditTaskResourceRepository.countByTask_IdAndStatus(task.getId(), AuditStatus.REJECTED);
        long pendingCount = auditTaskResourceRepository.countByTask_IdAndStatus(task.getId(), AuditStatus.PENDING);
        AuditStatus prev = task.getStatus();
        if (task.getFormat() != AuditContentFormat.RICH) {
            if (rejectedCount > 0) {
                task.setStatus(AuditStatus.REJECTED);
                task.setRejectType(lastUpdated.getRejectType());
                task.setRejectReason(lastUpdated.getRejectReason());
            } else if (pendingCount == 0) {
                task.setStatus(AuditStatus.APPROVED);
                task.setRejectType(null);
                task.setRejectReason(null);
            }
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
        List<AuditResource> auditResources = auditTaskResourceRepository
                .findAllByTaskIdIn(taskIds);
        Map<Long, List<AuditResource>> grouped = new HashMap<>();
        auditResources.forEach(resource -> {
            Long taskId = resource.getTask().getId();
            grouped.computeIfAbsent(taskId, ignored -> new ArrayList<>()).add(resource);
        });
        return grouped;
    }

    private AuditPayload getPayload(String json, TargetType targetType, AuditContentFormat format) {
        return switch (targetType) {
            case POST -> {
                PostAuditPayload payload = JsonUtil.fromJson(json, PostAuditPayload.class);
                if (payload != null) {
                    if (payload.getCoverageResUuid() != null) {
                        Resource coverageRes = resourceRepository.findByUuidAndStatusNot(
                                payload.getCoverageResUuid(), ResourceStatus.DELETED
                        ).orElse(null);
                        if (coverageRes != null) {
                            payload.setCoverResPresignedUrl(cloudFileService.getReadUrlCached(
                                    getCloudFSRootByTargetType(targetType),
                                    coverageRes.getResourceKey(),
                                    payload.getCoverageResUuid(),
                                    targetType
                            ));
                        } else {
                            payload.setCoverResPresignedUrl(null);
                        }
                    }
                    // Resolve res://<uuid> in content
                    resolveContentImages(payload);
                }
                yield payload;
            }
            case POST_CONTENT_IMAGE, POST_COVERAGE_IMAGE, BANNER_IMAGE, USER_AVATAR -> {
                yield json != null ? JsonUtil.fromJson(json, ImageAuditPayload.class) : null;
            }
            default -> {
                if (format == AuditContentFormat.TXT) {
                    yield json != null ? JsonUtil.fromJson(json, ReplyPayload.class) : null;
                }
                yield null;
            }
        };
    }


    private void resolveContentImages(PostAuditPayload payload) {
        String content = payload.getContent();
        if (StringUtil.isBlank(content)) return;
        Set<String> uuids = StringUtil.extraResPlaceholders(content);
        if (uuids.isEmpty()) return;
        Map<String, String> uuidToKey = resourceRepository.findByUuidIn(uuids).stream()
                .filter(r -> r.getStatus() != ResourceStatus.DELETED)
                .collect(Collectors.toMap(Resource::getUuid, Resource::getResourceKey));
        if (uuidToKey.isEmpty()) return;
        Map<String, CloudResPresignedUrlResp> urlMap = cloudFileService.batchGetReadPublicUrlCached(
                CloudFSRoot.UPLOADS, uuidToKey, TargetType.POST_CONTENT_IMAGE);
        payload.setContentHtml(StringUtil.replaceResPlaceholders(content,
                urlMap.entrySet().stream()
                        .filter(e -> e.getValue() != null && e.getValue().getUrl() != null)
                        .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getUrl()))));
    }

    private ModerationResourceRes toModerationResourceRes(AuditResource auditResource) {
        CloudResPresignedUrlResp urlResp = null;
        try {
            urlResp = cloudFileService.getReadUrlCached(
                    getCloudFSRootByTargetType(auditResource.getTask().getTargetType()),
                    auditResource.getResource().getResourceKey(),
                    auditResource.getResource().getUuid(),
                    auditResource.getTask().getTargetType()
            );
        } catch (Exception e) {
            log.debug("Failed generating preview url for resource {}, key={}, err={}",
                    auditResource.getId(), auditResource.getResource().getResourceKey(), e.getMessage());
        }
        ModerationResourceRes result = auditTaskResourceMapper
                .toModerationResourceRes(auditResource);
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

    private void sendMessages(List<AuditTask> tasks) {
        if (CollectionUtil.isEmpty(tasks)) return;

        try {
            List<ModerationConsumerMessage> messages = tasks.stream()
                    .map(auditTaskMapper::toModerationConsumerMessage)
                    .toList();

            if (messages.size() == 1) {
                rabbitTemplate.convertAndSend(
                        RabbitConstants.MODERATION_EXCHANGE,
                        RabbitConstants.ROUTE_MODERATION_RESULT,
                        messages.getFirst()
                );
                return;
            }

            rabbitTemplate.convertAndSend(
                    RabbitConstants.MODERATION_EXCHANGE,
                    RabbitConstants.ROUTE_MODERATION_BATCH_RESULT,
                    new ModerationBatchMessage(messages)
            );

        } catch (Exception e) {
            log.error("Failed to send moderation result messages, task count: {}, error: {}",
                    tasks.size(), e.getMessage(), e);
        }
    }

    private CloudFSRoot getCloudFSRootByTargetType(TargetType targetType) {
        return switch (targetType) {
            case USER_AVATAR, POST, POST_CONTENT_IMAGE, POST_COVERAGE_IMAGE -> CloudFSRoot.UPLOADS;
            default -> throw new IllegalArgumentException("Unsupported target type: " + targetType);
        };
    }
}
