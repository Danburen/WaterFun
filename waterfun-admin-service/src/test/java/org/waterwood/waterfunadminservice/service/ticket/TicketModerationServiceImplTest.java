package org.waterwood.waterfunadminservice.service.ticket;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.waterwood.waterfunadminservice.api.request.ticket.TicketReviewRequest;
import org.waterwood.waterfunservicecore.entity.security.PenaltyType;
import org.waterwood.waterfunservicecore.entity.ticket.Ticket;
import org.waterwood.waterfunservicecore.entity.ticket.TicketAuditStatus;
import org.waterwood.waterfunservicecore.entity.ticket.TicketType;
import org.waterwood.waterfunservicecore.entity.user.User;
import org.waterwood.waterfunservicecore.infrastructure.persistence.ticket.TicketRepository;
import org.waterwood.waterfunservicecore.infrastructure.persistence.user.UserRepository;
import org.waterwood.waterfunservicecore.infrastructure.utils.context.UserCtxHolder;
import org.waterwood.waterfunservicecore.services.user.UserBriefService;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TicketModerationServiceImplTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserBriefService userBriefService;

    @Mock
    private PenaltyService penaltyService;

    @InjectMocks
    private TicketModerationServiceImpl ticketModerationService;

    @Captor
    private ArgumentCaptor<Ticket> ticketCaptor;

    private Ticket reportTicket;
    private User submitter;
    private User auditor;

    @BeforeEach
    void setUp() {
        submitter = mock(User.class);
        when(submitter.getUid()).thenReturn(500L);
        when(submitter.getCreatedAt()).thenReturn(Instant.now());

        auditor = mock(User.class);
        when(auditor.getUid()).thenReturn(1L);
        when(auditor.getCreatedAt()).thenReturn(Instant.now());

        reportTicket = new Ticket();
        reportTicket.setId(100L);
        reportTicket.setTicketType(TicketType.CONTENT_REPORT);
        reportTicket.setStatus(TicketAuditStatus.PENDING);
        reportTicket.setSubmitter(submitter);
    }

    @Test
    void reviewTicket_approveReportWithoutPenalty_shouldNotApplyPenalty() {
        when(ticketRepository.findByIdAndStatus(100L, TicketAuditStatus.PENDING))
                .thenReturn(Optional.of(reportTicket));
        when(userRepository.getReferenceById(anyLong())).thenReturn(auditor);

        TicketReviewRequest request = new TicketReviewRequest();
        request.setAction(TicketReviewRequest.ReviewAction.APPROVE);
        request.setPenaltyType(null);

        try (MockedStatic<UserCtxHolder> ctx = mockStatic(UserCtxHolder.class)) {
            ctx.when(UserCtxHolder::getUserUid).thenReturn(1L);
            ticketModerationService.reviewTicket(100L, request);
        }

        verify(ticketRepository).save(any());
        verifyNoInteractions(penaltyService);
    }

    @Test
    void reviewTicket_taskNotFound_shouldThrow() {
        when(ticketRepository.findByIdAndStatus(999L, TicketAuditStatus.PENDING))
                .thenReturn(Optional.empty());

        TicketReviewRequest request = new TicketReviewRequest();
        request.setAction(TicketReviewRequest.ReviewAction.APPROVE);

        try (MockedStatic<UserCtxHolder> ctx = mockStatic(UserCtxHolder.class)) {
            ctx.when(UserCtxHolder::getUserUid).thenReturn(1L);
            assertThrows(IllegalArgumentException.class,
                    () -> ticketModerationService.reviewTicket(999L, request));
        }
    }

    @Test
    void reviewTicket_approveSuggestion_shouldNotApplyPenalty() {
        Ticket suggestionTicket = new Ticket();
        suggestionTicket.setId(200L);
        suggestionTicket.setTicketType(TicketType.SUGGESTION);
        suggestionTicket.setStatus(TicketAuditStatus.PENDING);
        suggestionTicket.setSubmitter(submitter);

        when(ticketRepository.findByIdAndStatus(200L, TicketAuditStatus.PENDING))
                .thenReturn(Optional.of(suggestionTicket));
        when(userRepository.getReferenceById(anyLong())).thenReturn(auditor);

        TicketReviewRequest request = new TicketReviewRequest();
        request.setAction(TicketReviewRequest.ReviewAction.APPROVE);
        request.setPenaltyType(PenaltyType.BAN_POST);

        try (MockedStatic<UserCtxHolder> ctx = mockStatic(UserCtxHolder.class)) {
            ctx.when(UserCtxHolder::getUserUid).thenReturn(1L);
            ticketModerationService.reviewTicket(200L, request);
        }

        verify(ticketRepository).save(any());
        verifyNoInteractions(penaltyService);
    }
}
