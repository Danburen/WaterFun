package org.waterwood.waterfunadminservice.service.ticket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.waterwood.common.RabbitConstants;
import org.waterwood.waterfunadminservice.api.request.ticket.TicketReviewRequest;
import org.waterwood.waterfunadminservice.api.response.BanStatusResponse;
import org.waterwood.waterfunadminservice.api.response.ticket.TicketResponse;
import org.waterwood.waterfunadminservice.api.response.user.UserAdminBrief;
import org.waterwood.waterfunadminservice.infrastructure.mapper.TicketMapper;
import org.waterwood.waterfunadminservice.service.user.UserAdminService;
import org.waterwood.waterfunservicecore.api.message.TicketMessage;
import org.waterwood.waterfunservicecore.api.resp.CloudResPresignedUrlResp;
import org.waterwood.waterfunservicecore.api.resp.user.UserBrief;
import org.waterwood.waterfunservicecore.entity.audit.TargetType;
import org.waterwood.waterfunservicecore.entity.security.PenaltyType;
import org.waterwood.waterfunservicecore.entity.spec.TicketSpec;
import org.waterwood.waterfunservicecore.entity.ticket.Ticket;
import org.waterwood.waterfunservicecore.entity.ticket.TicketAuditStatus;
import org.waterwood.waterfunservicecore.entity.ticket.TicketRejectType;
import org.waterwood.waterfunservicecore.entity.ticket.TicketType;
import org.waterwood.waterfunservicecore.entity.user.User;
import org.waterwood.waterfunservicecore.entity.BanPermission;
import org.waterwood.waterfunservicecore.entity.perm.Permission;
import org.waterwood.waterfunservicecore.entity.user.UserPermission;
import org.waterwood.waterfunservicecore.exception.TicketNotFoundException;
import org.waterwood.waterfunservicecore.infrastructure.persistence.PermissionRepo;
import org.waterwood.waterfunservicecore.infrastructure.persistence.ticket.TicketRepository;
import org.waterwood.waterfunservicecore.infrastructure.persistence.ticket.TicketResourceRepository;
import org.waterwood.waterfunservicecore.infrastructure.persistence.user.UserPenaltyHistoryRepository;
import org.waterwood.waterfunservicecore.infrastructure.persistence.user.UserPermRepo;
import org.waterwood.waterfunservicecore.infrastructure.persistence.user.UserRepository;
import org.waterwood.waterfunservicecore.infrastructure.utils.context.UserCtxHolder;
import org.waterwood.waterfunservicecore.services.user.UserBriefService;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TicketModerationServiceImpl implements TicketModerationService {

    private final TicketRepository ticketRepository;
    private final TicketResourceRepository ticketResourceRepository;
    private final UserPenaltyHistoryRepository userPenaltyHistoryRepository;
    private final UserRepository userRepository;
    private final UserBriefService userBriefService;
    private final TicketMapper ticketMapper;
    private final PenaltyService penaltyService;
    private final RabbitTemplate rabbitTemplate;

    private final UserAdminService userAdminService;

    private final UserPermRepo userPermRepo;
    private final PermissionRepo permissionRepo;

    @Override
    public Page<TicketResponse> listTickets(List<TicketType> ticketTypes, TicketAuditStatus status, String targetId, Pageable pageable) {
        Specification<Ticket> spec = TicketSpec.of(
                ticketTypes, status, null, targetId, null, null
        );
        List<Ticket> tickets = ticketRepository.findAll(spec, pageable).getContent();
        Map<Long, UserBrief> auditorUserBriefMap = userBriefService.queryForMapUserIdBriefMap(
                tickets.stream().map(t -> t.getAuditor() != null ? t.getAuditor().getUid() : null).filter(Objects::nonNull).toList()
        );
        Map<Long, UserAdminBrief> submitterUserAdminBriefMap = userAdminService.batchGetUserAdminBrief(
                tickets.stream().map(t -> t.getSubmitter() != null ? t.getSubmitter().getUid() : null).filter(Objects::nonNull).toList()
        );
        Map<Long, UserAdminBrief> targetUserAdminBriefMap = userAdminService.batchGetUserAdminBrief(
                tickets.stream().map(Ticket::getTargetUserUid).filter(Objects::nonNull).toList()
        );
        // Batch-load current ban status
        Map<Long, BanStatusResponse> currentBansMap = loadCurrentBansForTickets(tickets);
        return ticketRepository.findAll(spec, pageable).map(ticket-> {
            TicketResponse resp = ticketMapper.toTicketResponse(ticket);
            resp.setSubmitter(submitterUserAdminBriefMap.get(ticket.getSubmitter() == null ?  null : ticket.getSubmitter().getUid()));
            resp.setTargetUser(targetUserAdminBriefMap.get(ticket.getTargetUserUid()));
            if (ticket.getAuditor() != null) {
                resp.setAuditor(
                        auditorUserBriefMap.get(ticket.getAuditor().getUid())
                );
            }
            if (ticket.getRejectType() != null) {
                resp.setRejectType(ticket.getRejectType().name());
            }
            
            resp.setEvidenceResourceUuids(loadEvidenceUuids(ticket.getId()));
            resp.setOriginalPenalty(loadOriginalPenalty(ticket));
            // For appeals, currentBans targets the submitter; for content reports, the target user
            Long currentBansUserUid = ticket.getTicketType() == TicketType.CONTENT_REPORT
                    ? ticket.getTargetUserUid()
                    : (ticket.getSubmitter() != null ? ticket.getSubmitter().getUid() : null);
            resp.setCurrentBans(currentBansMap.get(currentBansUserUid));
            resp.setTimeline(buildTimeline(ticket));
            return resp;
        });
    }

    @Transactional
    @Override
    public void reviewTicket(Long ticketId, TicketReviewRequest request) {
        Ticket ticket = ticketRepository.findByIdAndStatus(ticketId, TicketAuditStatus.PENDING)
                .orElseThrow(TicketNotFoundException::new);

        User auditor = userRepository.getReferenceById(UserCtxHolder.getUserUid());

        switch (request.getAction()) {
            case APPROVE -> approveTicket(ticket, auditor, request);
            case REJECT -> rejectTicket(ticket, auditor, request);
        }
    }

    private void approveTicket(Ticket ticket, User auditor, TicketReviewRequest request) {
        ticket.setStatus(TicketAuditStatus.RESOLVED);
        ticket.setAuditor(auditor);
        ticket.setAuditAt(Instant.now());
        ticket.setUpdatedAt(Instant.now());
        ticket.setAuditNote(request.getAuditNote());
        ticket.setReplyContent(request.getReplyContent());
        ticketRepository.save(ticket);

        PenaltyType penaltyType = null;
        Long targetUserUid = null;
        String targetUserDisplayName = null;

        if (ticket.getTicketType() == TicketType.CONTENT_REPORT && request.getPenaltyType() != null) {
            penaltyType = request.getPenaltyType();
            targetUserUid = resolveTargetUserUid(ticket);

            if (targetUserUid != null) {
                Instant expiresAt = request.getPenaltyDurationHours() != null
                        ? Instant.now().plusSeconds(request.getPenaltyDurationHours() * 3600)
                        : null;
                penaltyService.applyPenalty(targetUserUid, penaltyType,
                        request.getBanReasonType(), expiresAt,
                        ticket.getTargetId(), ticket.getTargetType(), ticket.getContent());

                try {
                    targetUserDisplayName = userBriefService.getUserBrief(targetUserUid).getDisplayName();
                } catch (Exception e) {
                    log.warn("Failed to get display name for target user {}: {}", targetUserUid, e.getMessage());
                }
            }
        }

        if (ticket.getTicketType() == TicketType.ACCOUNT_APPEAL) {
            Long submitterUid = ticket.getSubmitter().getUid();
            penaltyService.liftAllPenalties(submitterUid);
        }

        sendTicketMessage(ticket, request.getReplyContent(), penaltyType, targetUserUid, targetUserDisplayName);
    }

    private void rejectTicket(Ticket ticket, User auditor, TicketReviewRequest request) {
        ticket.setStatus(TicketAuditStatus.REJECTED);
        ticket.setAuditor(auditor);
        ticket.setRejectType(request.getRejectType() != null ? request.getRejectType() : TicketRejectType.NONE);
        ticket.setAuditNote(request.getAuditNote());
        ticket.setReplyContent(request.getReplyContent());
        ticket.setAuditAt(Instant.now());
        ticket.setUpdatedAt(Instant.now());
        ticketRepository.save(ticket);

        sendTicketMessage(ticket, request.getReplyContent());
    }

    private void sendTicketMessage(Ticket ticket, String replyContent) {
        sendTicketMessage(ticket, replyContent, null, null, null);
    }

    private void sendTicketMessage(Ticket ticket, String replyContent, PenaltyType penaltyType,
                                    Long targetUserUid, String targetUserDisplayName) {
        try {
            TicketMessage message = ticketMapper.toTicketMessage(ticket);
            message.setReplyContent(replyContent);
            message.setPenaltyType(penaltyType);
            message.setTargetUserUid(targetUserUid);
            message.setTargetUserDisplayName(targetUserDisplayName);
            rabbitTemplate.convertAndSend(
                    RabbitConstants.MODERATION_EXCHANGE,
                    RabbitConstants.ROUTE_TICKET_RESULT,
                    message
            );
        } catch (Exception e) {
            log.warn("Failed to send ticket message for ticket {}: {}", ticket.getId(), e.getMessage());
        }
    }

    private Long resolveTargetUserUid(Ticket ticket) {
        // Priority 1: use targetUserUid if already resolved at ticket creation time
        if (ticket.getTargetUserUid() != null) {
            return ticket.getTargetUserUid();
        }
        // Priority 2: fallback for legacy tickets — USER type stores UID in targetId
        if (ticket.getTargetType() == TargetType.USER) {
            try {
                return Long.parseLong(ticket.getTargetId());
            } catch (NumberFormatException e) {
                log.warn("Invalid target user UID for ticket {}: {}", ticket.getId(), ticket.getTargetId());
            }
        }
        return null;
    }

    /**
     * Batch-load current active ban restrictions for tickets that need ban status.
     * - ACCOUNT_APPEAL: bans for the submitter (person appealing)
     * - CONTENT_REPORT: bans for the target user (reported user)
     */
    private Map<Long, BanStatusResponse> loadCurrentBansForTickets(List<Ticket> tickets) {
        // Resolve ban permission IDs
        Set<String> banCodes = Arrays.stream(BanPermission.values())
                .map(BanPermission::getCode)
                .collect(Collectors.toSet());
        Set<Integer> banPermIds = permissionRepo.findByCodeIn(List.copyOf(banCodes)).stream()
                .map(Permission::getId)
                .collect(Collectors.toSet());

        if (banPermIds.isEmpty()) {
            return Collections.emptyMap();
        }

        // Collect UIDs to check — different per ticket type
        Set<Long> relevantUids = new HashSet<>();
        for (Ticket t : tickets) {
            if (t.getTicketType() == TicketType.ACCOUNT_APPEAL
                    && t.getSubmitter() != null) {
                relevantUids.add(t.getSubmitter().getUid());
            } else if (t.getTicketType() == TicketType.CONTENT_REPORT
                    && t.getTargetUserUid() != null) {
                relevantUids.add(t.getTargetUserUid());
            }
        }
        if (relevantUids.isEmpty()) {
            return Collections.emptyMap();
        }

        // Batch query all active restrictions for these users
        List<UserPermission> allPerms = userPermRepo.findByUserUidInAndPermissionIdIn(
                relevantUids, banPermIds);

        Instant now = Instant.now();
        // Group by userUid, filter active only
        Map<Long, List<UserPermission>> permsByUser = allPerms.stream()
                .filter(up -> up.getExpiresAt() == null || up.getExpiresAt().isAfter(now))
                .collect(Collectors.groupingBy(up -> up.getUser().getUid()));

        Map<Long, BanStatusResponse> result = new HashMap<>();
        for (Long uid : relevantUids) {
            List<UserPermission> activePerms = permsByUser.getOrDefault(uid, List.of());
            List<BanStatusResponse.ActiveRestriction> restrictions = activePerms.stream()
                    .map(up -> BanStatusResponse.ActiveRestriction.builder()
                            .permissionCode(up.getCode())
                            .permissionName(up.getName())
                            .banReasonType(up.getBanReasonType() != null ? up.getBanReasonType().name() : null)
                            .expiresAt(up.getExpiresAt())
                            .permanent(up.getExpiresAt() == null)
                            .createdAt(up.getCreatedAt())
                            .build())
                    .toList();
            result.put(uid, BanStatusResponse.builder()
                    .userUid(uid)
                    .banned(!restrictions.isEmpty())
                    .restrictions(restrictions)
                    .build());
        }
        return result;
    }

    private List<String> loadEvidenceUuids(Long ticketId) {
        try {
            return ticketResourceRepository.findByIdTicketId(ticketId).stream()
                    .map(tr -> tr.getId().getResourceUuid())
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("Failed to load evidence for ticket {}: {}", ticketId, e.getMessage());
            return List.of();
        }
    }

    private TicketResponse.PenaltyDetail loadOriginalPenalty(Ticket ticket) {
        if (ticket.getTicketType() != TicketType.ACCOUNT_APPEAL) {
            return null;
        }
        try {
            return userPenaltyHistoryRepository.findTopByUserUidOrderByCreatedAtDesc(
                    ticket.getSubmitter().getUid()
            ).map(history -> {
                String operatorName = "";
                if (history.getOperator() != null) {
                    try {
                        operatorName = userBriefService.getUserBrief(history.getOperator().getUid()).getDisplayName();
                    } catch (Exception e) {
                        operatorName = "System";
                    }
                }
                return new TicketResponse.PenaltyDetail(
                        history.getPenaltyType() != null ? history.getPenaltyType().name() : null,
                        history.getReason(),
                        operatorName,
                        history.getCreatedAt()
                );
            }).orElse(null);
        } catch (Exception e) {
            log.warn("Failed to load penalty history for user {}: {}", ticket.getSubmitter().getUid(), e.getMessage());
            return null;
        }
    }

    private TicketResponse.Timeline buildTimeline(Ticket ticket) {
        return new TicketResponse.Timeline(
                ticket.getCreatedAt(),
                ticket.getAuditAt(),
                ticket.getStatus() != null ? ticket.getStatus().name() : null
        );
    }

    private UserAdminBrief buildUserAdminBrief(User user, UserBrief brief, CloudResPresignedUrlResp resp) {
        return new UserAdminBrief(
                user.getUid(),
                brief != null ? brief.getDisplayName() : null,
                resp, (short) 0, null,
                user.getCreatedAt(), 0L,
                null
        );
    }
}
