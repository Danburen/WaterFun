package org.waterwood.waterfunservice.service.report;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.waterwood.common.CloudFSRoot;
import org.waterwood.common.io.FileExtension;
import org.waterwood.common.io.ResourceType;
import org.waterwood.utils.CollectionUtil;
import org.waterwood.utils.StringUtil;
import org.waterwood.waterfunservice.api.UserBizType;
import org.waterwood.waterfunservice.api.UserUploadContext;
import org.waterwood.waterfunservice.api.UserUploadPolicyReq;
import org.waterwood.waterfunservice.api.response.ticket.TicketStatsResponse;
import org.waterwood.waterfunservice.api.response.ticket.UserTicketDetailResponse;
import org.waterwood.waterfunservice.api.response.ticket.UserTicketListResponse;
import org.waterwood.waterfunservice.infrastructure.exception.ReportTargetInvalidException;
import org.waterwood.waterfunservicecore.api.UploadItem;
import org.waterwood.waterfunservicecore.api.req.CloudPutCallbackReq;
import org.waterwood.waterfunservicecore.api.resp.CloudResPresignedUrlResp;
import org.waterwood.waterfunservicecore.api.resp.PresignedResp;
import org.waterwood.waterfunservicecore.entity.audit.AuditType;
import org.waterwood.waterfunservicecore.entity.audit.TargetType;
import org.waterwood.waterfunservicecore.entity.notification.BusinessType;
import org.waterwood.waterfunservicecore.entity.notification.Inbox;
import org.waterwood.waterfunservicecore.entity.resource.Resource;
import org.waterwood.waterfunservicecore.entity.resource.ResourceStatus;
import org.waterwood.waterfunservicecore.entity.security.PenaltyType;
import org.waterwood.waterfunservicecore.entity.spec.TicketSpec;
import org.waterwood.waterfunservicecore.entity.ticket.Ticket;
import org.waterwood.waterfunservicecore.entity.ticket.TicketAuditStatus;
import org.waterwood.waterfunservicecore.entity.ticket.TicketRejectType;
import org.waterwood.waterfunservicecore.entity.ticket.TicketResource;
import org.waterwood.waterfunservicecore.entity.ticket.TicketResourceId;
import org.waterwood.waterfunservicecore.entity.ticket.TicketType;
import org.waterwood.waterfunservicecore.entity.ticket.UserTicket;
import org.waterwood.waterfunservicecore.entity.ticket.UserTicketId;
import org.waterwood.waterfunservicecore.exception.ForbiddenException;
import org.waterwood.waterfunservicecore.exception.ReportAlreadyExistException;
import org.waterwood.waterfunservicecore.exception.ReportNotFoundException;
import org.waterwood.waterfunservicecore.exception.reference.ResourceReferenceInvalidException;
import org.waterwood.waterfunservicecore.entity.post.CommentDO;
import org.waterwood.waterfunservicecore.entity.post.CommentStatus;
import org.waterwood.waterfunservicecore.infrastructure.persistence.CommentRepository;
import org.waterwood.waterfunservicecore.infrastructure.persistence.PostRepository;
import org.waterwood.waterfunservicecore.infrastructure.persistence.ResourceRepository;
import org.waterwood.waterfunservicecore.infrastructure.persistence.UserReportRepository;
import org.waterwood.waterfunservicecore.infrastructure.persistence.notification.InboxRepository;
import org.waterwood.waterfunservicecore.infrastructure.persistence.ticket.TicketRepository;
import org.waterwood.waterfunservicecore.infrastructure.persistence.ticket.TicketResourceRepository;
import org.waterwood.waterfunservicecore.infrastructure.persistence.user.UserRepository;
import org.waterwood.waterfunservicecore.exception.ServiceException;
import org.waterwood.waterfunservicecore.infrastructure.utils.BizUploadPayload;
import org.waterwood.waterfunservicecore.infrastructure.utils.CosKeyPathGenerator;
import org.waterwood.waterfunservicecore.infrastructure.utils.IdGenerator;
import org.waterwood.waterfunservicecore.infrastructure.utils.context.UserCtxHolder;
import org.waterwood.waterfunservicecore.services.sys.storage.CloudFileService;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {
    private final UserReportRepository userReportRepository;
    private final TicketRepository ticketRepository;
    private final TicketResourceRepository ticketResourceRepository;
    private final UserRepository userRepository;
    private final ResourceRepository resourceRepository;
    private final CloudFileService cloudFileService;
    private final InboxRepository inboxRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;

    @Transactional
    @Override
    public Long submitReport(String targetId, TargetType targetType, AuditType type, String reason, List<String> resourceUuids) {
        Long userUid = UserCtxHolder.getUserUid();
        // Ensure targetId and targetType are valid for content reports
        if (!StringUtil.isNotBlank(targetId) || targetType == null || targetType == TargetType.DEFAULT) {
            throw new ReportTargetInvalidException();
        }
        ticketRepository.findBySubmitterUidAndTargetIdAndTargetTypeAndTicketTypeAndStatusIn(
                userUid, targetId, targetType, TicketType.CONTENT_REPORT,
                List.of(TicketAuditStatus.PENDING, TicketAuditStatus.RESOLVED, TicketAuditStatus.REJECTED)
        ).ifPresent(r -> { throw new ReportAlreadyExistException(); });
        return submitUserContent(
                TicketType.CONTENT_REPORT,
                targetId,
                targetType,
                reason,
                null,
                resourceUuids
        );
    }

    @Transactional
    @Override
    public Long submitSuggestion(String content, List<String> resourceUuids) {
        return submitUserContent(
                TicketType.SUGGESTION,
                null,
                TargetType.DEFAULT,
                content,
                null,
                resourceUuids
        );
    }

    @Transactional
    @Override
    public Long submitFeedback(String content, List<String> resourceUuids) {
        return submitUserContent(
                TicketType.FEATURE_FEEDBACK,
                null, TargetType.DEFAULT,
                content,
                null,
                resourceUuids
        );
    }

    @Transactional
    @Override
    public Long submitAppeal(String targetId, TargetType targetType, String content, PenaltyType penaltyType, List<String> resourceUuids) {
        return submitUserContent(
                TicketType.ACCOUNT_APPEAL,
                targetId,
                targetType,
                content,
                penaltyType,
                resourceUuids
        );
    }

    @Transactional
    @Override
    public void cancelReport(Long userUid, Long reportId) {
        userReportRepository.findByTicketIdAndUserUid(reportId, userUid)
                .orElseThrow(ReportNotFoundException::new);

        Ticket ticket = ticketRepository.findById(reportId)
                .orElseThrow(ReportNotFoundException::new);
        if (ticket.getStatus() != TicketAuditStatus.PENDING) {
            throw new ServiceException("Ticket is not in PENDING status, cannot be cancelled");
        }

        // Release evidence resources back to ORPHAN so the uploader can reuse them
        List<TicketResource> ticketResources = ticketResourceRepository.findByIdTicketId(reportId);
        if (!ticketResources.isEmpty()) {
            List<String> releaseUuids = ticketResources.stream()
                    .map(tr -> tr.getId().getResourceUuid())
                    .toList();
            resourceRepository.batchUpdateStatusFromTo(ResourceStatus.ACTIVE, ResourceStatus.ORPHAN, releaseUuids);
        }

        ticket.setStatus(TicketAuditStatus.CANCELLED);
        ticket.setUpdatedAt(Instant.now());
        ticketRepository.save(ticket);
    }

    private Long submitUserContent(TicketType ticketType,
                                   String targetId,
                                   TargetType targetType,
                                   String content,
                                   PenaltyType penaltyType,
                                   List<String> resourceUuids) {
        Long userUid = UserCtxHolder.getUserUid();
        List<String> availableUuids = List.of();
        if(CollectionUtil.isNotEmpty(resourceUuids)) {
            availableUuids = resourceRepository.findByUuidInAndUploaderIdAndStatus(
                    resourceUuids, userUid, ResourceStatus.ORPHAN
            ).stream().map(Resource::getUuid).toList();
            if (availableUuids.size() != resourceUuids.size()) {
                List<String> invalidUuids = new ArrayList<>(resourceUuids);
                invalidUuids.removeAll(availableUuids);
                log.warn("User {} submitted report with invalid resource UUIDs (not ORPHAN or not owned): {}", userUid, invalidUuids);
                throw new ResourceReferenceInvalidException(String.join(",", invalidUuids));
            }
            resourceRepository.batchUpdateStatusFromTo( ResourceStatus.ORPHAN, ResourceStatus.ACTIVE, availableUuids);
        }
        Ticket ticket = new Ticket();
        ticket.setId(IdGenerator.generateTicketId());
        ticket.setTargetId(targetId);
        ticket.setTargetType(targetType);
        ticket.setContent(content);
        ticket.setTicketType(ticketType);
        ticket.setPenaltyType(penaltyType);
        ticket.setSubmitter(userRepository.getReferenceById(userUid));
        ticket.setStatus(TicketAuditStatus.PENDING);
        // Resolve targetUserUid: for CONTENT_REPORT, look up the content owner;
        // for ACCOUNT_APPEAL, the submitter IS the target user
        if (targetId != null && StringUtil.isNotBlank(targetId)) {
            if (ticketType == TicketType.CONTENT_REPORT) {
                resolveTargetUserUid(ticket);
            } else if (ticketType == TicketType.ACCOUNT_APPEAL) {
                ticket.setTargetUserUid(userUid);
            }
        }
        ticketRepository.save(ticket);

        UserTicket userTicket = new UserTicket();
        UserTicketId userTicketId = new UserTicketId();
        userTicketId.setTicketId(ticket.getId());
        userTicketId.setUserUid(userUid);
        userTicket.setId(userTicketId);
        userReportRepository.save(userTicket);

        if (!availableUuids.isEmpty()) {
            List<TicketResource> ticketResources = availableUuids.stream().map(uuid -> {
                TicketResource tr = new TicketResource();
                TicketResourceId trId = new TicketResourceId();
                trId.setTicketId(ticket.getId());
                trId.setResourceUuid(uuid);
                tr.setId(trId);
                return tr;
            }).toList();
            ticketResourceRepository.saveAll(ticketResources);
        }
        return ticket.getId();
    }

    /**
     * Resolve the target user UID for CONTENT_REPORT tickets.
     * For USER / USER_AVATAR targetType, the targetId IS the user's UID.
     * For POST / POST_COVERAGE_IMAGE targetType, look up the content owner.
     * For COMMENT targetType, look up the comment author.
     */
    private void resolveTargetUserUid(Ticket ticket) {
        try {
            switch (ticket.getTargetType()) {
                case USER, USER_AVATAR -> ticket.setTargetUserUid(Long.parseLong(ticket.getTargetId()));
                case POST -> {
                    Long postId = Long.parseLong(ticket.getTargetId());
                    ticket.setTargetUserUid(postRepository.findAuthorUidById(postId));
                }
                case POST_COVERAGE_IMAGE -> {
                    String resourceUuid = ticket.getTargetId();
                    ticket.setTargetUserUid(postRepository.findAuthorUidByCoverageResourceUuid(resourceUuid));
                }
                case COMMENT -> {
                    Long commentId = Long.parseLong(ticket.getTargetId());
                    commentRepository.findAuthorUidByIdAndStatus(commentId, CommentStatus.NORMAL)
                            .map(CommentDO::getAuthorUid)
                            .ifPresentOrElse(
                                ticket::setTargetUserUid,
                                () -> log.warn("Comment not found or not visible for ticket {}: {}", ticket.getId(), commentId)
                            );
                }
                case POST_CONTENT_IMAGE, BANNER_IMAGE, MODERATION_IMAGE ->
                    log.warn("Cannot resolve target user for targetType {} on ticket {}: no direct owner link",
                            ticket.getTargetType(), ticket.getId());
                default ->
                    log.warn("Unsupported targetType {} for ticket {}", ticket.getTargetType(), ticket.getId());
            }
        } catch (NumberFormatException e) {
            log.warn("Invalid targetId format for ticket {} (type={}): {}", ticket.getId(), ticket.getTargetType(), ticket.getTargetId());
        } catch (Exception e) {
            log.error("Failed to resolve target user for ticket {} (type={}): {}", ticket.getId(), ticket.getTargetType(), e.getMessage(), e);
        }
    }

    @Override
    public List<PresignedResp> handleImageUpload(UserUploadPolicyReq request) {
        Long bizId = UserCtxHolder.getUserUid();
        List<PresignedResp> results = new ArrayList<>();
        List<UploadItem> validItems = new ArrayList<>();

        List<FileExtension> exts = request.getExts().stream().map(FileExtension::fromExt).toList();
        for(int i = 0; i < exts.size(); i++){
            if (TargetType.USER_REPORT_ATTACHMENT.isAllowed(exts.get(i))) {
                UUID uuid = UUID.randomUUID();
                results.add(null);
                validItems.add(new UploadItem(
                        i,
                        CosKeyPathGenerator.of(uuid, exts.get(i)),
                        StringUtil.noDashUUIDString(uuid),
                        uuid,
                        exts.get(i))
                );
            }else{
                results.add(PresignedResp.ofError("system.file_type_not_allowed"));
            }
        }

        if(! validItems.isEmpty()){
            List<Resource> resources = validItems.stream()
                    .map(item -> {
                        return cloudFileService.createAndSetUpUploadRes(item.uuidPlain(), item.path(), UserCtxHolder.getUserUid());
                    })
                    .toList();
            resourceRepository.saveAll(resources);
            List<BizUploadPayload> payloads = validItems.stream()
                    .map(item -> BizUploadPayload.of(
                            bizId,
                            UserBizType.REPORT_ATTACHMENT_IMAGE.name(),
                            item.uuid()
                    ))
                    .toList();
            List<PresignedResp> signed = cloudFileService.batchBuildPutPolicyForUploads(
                    validItems.stream().map(UploadItem::path).toList(),
                    payloads
            );
            for (int i = 0; i < signed.size(); i++) {
                int originalIndex = validItems.get(i).originalIndex();
                results.set(originalIndex, signed.get(i));
            }
        }
        return results;
    }

    @Transactional
    @Override
    public void handleImageCallback(CloudPutCallbackReq request, UserUploadContext<Long> ctx) {
        String resourceUuid = ctx.getResourceUuid();
        Long bizId = UserCtxHolder.getUserUid();
        if(! bizId.equals(ctx.getBizId())) throw new ForbiddenException();
        Resource newRes = resourceRepository.findByUuidAndStatus(resourceUuid, ResourceStatus.UPLOAD_PENDING)
                .orElseThrow(() -> new ResourceReferenceInvalidException(resourceUuid));
        cloudFileService.setAndValidResourceForCallback(
                newRes,
                CloudFSRoot.UPLOADS,
                ResourceStatus.ORPHAN,
                ResourceType.IMAGE
        );
        resourceRepository.save(newRes);
    }

    @Override
    public Page<UserTicketListResponse> listUserTickets(Long userUid, TicketType ticketType, TicketAuditStatus status, Pageable pageable) {
        List<TicketType> ticketTypes = ticketType != null ? List.of(ticketType) : null;
        Specification<Ticket> spec = TicketSpec.of(ticketTypes, status, userUid, null, null, null);
        Page<Ticket> ticketPage = ticketRepository.findAll(spec, pageable);

        Map<Long, Integer> evidenceCountMap = buildEvidenceCountMap(ticketPage.getContent());

        return ticketPage.map(ticket -> {
            TicketRejectType rejectType = ticket.getRejectType();
            return new UserTicketListResponse(
                    ticket.getId(),
                    ticket.getTicketType(),
                    ticket.getStatus(),
                    ticket.getContent(),
                    ticket.getTargetId(),
                    ticket.getTargetType(),
                    ticket.getCreatedAt(),
                    ticket.getUpdatedAt(),
                    ticket.getAuditAt(),
                    ticket.getAuditNote(),
                    rejectType != null ? rejectType.name() : null,
                    evidenceCountMap.getOrDefault(ticket.getId(), 0)
            );
        });
    }

    @Override
    public UserTicketDetailResponse getUserTicketDetail(Long userUid, Long ticketId) {
        userReportRepository.findByTicketIdAndUserUid(ticketId, userUid)
                .orElseThrow(ReportNotFoundException::new);

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(ReportNotFoundException::new);

        List<UserTicketDetailResponse.EvidenceItem> evidenceItems = loadEvidenceWithUrls(ticketId);
        UserTicketDetailResponse.Timeline timeline = buildTimeline(ticket);
        List<UserTicketDetailResponse.ReplyItem> replies = loadReplies(ticketId);

        TicketRejectType rejectType = ticket.getRejectType();
        UserTicketDetailResponse resp = new UserTicketDetailResponse();
        resp.setTicketId(ticket.getId());
        resp.setTicketType(ticket.getTicketType());
        resp.setStatus(ticket.getStatus());
        resp.setContent(ticket.getContent());
        resp.setTargetId(ticket.getTargetId());
        resp.setTargetType(ticket.getTargetType());
        resp.setCreatedAt(ticket.getCreatedAt());
        resp.setUpdatedAt(ticket.getUpdatedAt());
        resp.setAuditAt(ticket.getAuditAt());
        resp.setAuditNote(ticket.getAuditNote());
        resp.setRejectType(rejectType != null ? rejectType.name() : null);
        resp.setEvidence(evidenceItems);
        resp.setTimeline(timeline);
        resp.setReplies(replies);
        return resp;
    }

    @Override
    public TicketStatsResponse getUserTicketStats(Long userUid) {
        List<Object[]> results = ticketRepository.countByTicketTypeAndUserUid(userUid);
        long reportCount = 0, appealCount = 0, feedbackCount = 0, suggestionCount = 0;
        for (Object[] row : results) {
            TicketType type = (TicketType) row[0];
            long count = (Long) row[1];
            switch (type) {
                case CONTENT_REPORT -> reportCount = count;
                case ACCOUNT_APPEAL -> appealCount = count;
                case FEATURE_FEEDBACK -> feedbackCount = count;
                case SUGGESTION -> suggestionCount = count;
            }
        }
        return new TicketStatsResponse(reportCount, appealCount, feedbackCount, suggestionCount);
    }

    private Map<Long, Integer> buildEvidenceCountMap(List<Ticket> tickets) {
        if (tickets.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> ticketIds = tickets.stream().map(Ticket::getId).toList();
        List<TicketResource> allResources = ticketResourceRepository.findByIdTicketIdIn(ticketIds);
        return allResources.stream()
                .collect(Collectors.groupingBy(
                        tr -> tr.getId().getTicketId(),
                        Collectors.summingInt(tr -> 1)
                ));
    }

    private List<UserTicketDetailResponse.EvidenceItem> loadEvidenceWithUrls(Long ticketId) {
        List<TicketResource> ticketResources = ticketResourceRepository.findByIdTicketId(ticketId);
        if (ticketResources.isEmpty()) {
            return List.of();
        }
        List<String> uuids = ticketResources.stream()
                .map(tr -> tr.getId().getResourceUuid())
                .toList();
        List<Resource> resources = resourceRepository.findByUuidIn(uuids);

        Map<String, String> uuidPathMap = resources.stream()
                .collect(Collectors.toMap(Resource::getUuid, Resource::getResourceKey));

        Map<String, CloudResPresignedUrlResp> urlMap = batchLoadPresignedUrls(uuidPathMap, ticketId);

        return uuids.stream().map(uuid -> {
            CloudResPresignedUrlResp urlResp = urlMap.get(uuid);
            return new UserTicketDetailResponse.EvidenceItem(
                    uuid,
                    urlResp != null ? urlResp.getUrl() : null,
                    urlResp != null ? urlResp.getExpireAt() : null
            );
        }).toList();
    }

    private Map<String, CloudResPresignedUrlResp> batchLoadPresignedUrls(Map<String, String> uuidPathMap, Long ticketId) {
        try {
            return cloudFileService.batchGetReadPublicUrlCached(
                    CloudFSRoot.UPLOADS, uuidPathMap, TargetType.USER_REPORT_ATTACHMENT
            );
        } catch (Exception e) {
            log.warn("Failed to get presigned URLs for ticket {} evidence: {}", ticketId, e.getMessage());
            return Collections.emptyMap();
        }
    }

    private UserTicketDetailResponse.Timeline buildTimeline(Ticket ticket) {
        return new UserTicketDetailResponse.Timeline(
                ticket.getCreatedAt(),
                ticket.getAuditAt(),
                ticket.getStatus() != null ? ticket.getStatus().name() : null
        );
    }

    private List<UserTicketDetailResponse.ReplyItem> loadReplies(Long ticketId) {
        try {
            List<Inbox> inboxReplies = inboxRepository
                    .findByBusinessTypeAndTargetIdOrderByCreatedAtDesc(
                            BusinessType.TICKET_REPLY, String.valueOf(ticketId)
                    );
            return inboxReplies.stream().map(inbox -> {
                String senderName = "";
                if (inbox.getSender() != null) {
                    try {
                        senderName = inbox.getSender().getUid().toString();
                    } catch (Exception e) {
                        senderName = "System";
                    }
                }
                String text = "";
                if (inbox.getContent() != null) {
                    Object textObj = inbox.getContent().get("text");
                    text = textObj != null ? textObj.toString() : "";
                }
                return new UserTicketDetailResponse.ReplyItem(
                        inbox.getId(),
                        text,
                        senderName,
                        inbox.getCreatedAt()
                );
            }).toList();
        } catch (Exception e) {
            log.warn("Failed to load replies for ticket {}: {}", ticketId, e.getMessage());
            return List.of();
        }
    }
}
