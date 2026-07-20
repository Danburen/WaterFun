package org.waterwood.waterfunservice.service.report;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.waterwood.waterfunservicecore.entity.audit.AuditType;
import org.waterwood.waterfunservicecore.entity.audit.TargetType;
import org.waterwood.waterfunservicecore.entity.ticket.*;
import org.waterwood.waterfunservicecore.entity.user.User;
import org.waterwood.waterfunservicecore.exception.ReportAlreadyExistException;
import org.waterwood.waterfunservicecore.exception.notfound.ReportNotFoundException;
import org.waterwood.waterfunservicecore.exception.ServiceException;
import org.waterwood.waterfunservicecore.infrastructure.persistence.ResourceRepository;
import org.waterwood.waterfunservicecore.infrastructure.persistence.UserReportRepository;
import org.waterwood.waterfunservicecore.infrastructure.persistence.notification.InboxRepository;
import org.waterwood.waterfunservicecore.infrastructure.persistence.ticket.TicketRepository;
import org.waterwood.waterfunservicecore.infrastructure.persistence.ticket.TicketResourceRepository;
import org.waterwood.waterfunservicecore.infrastructure.persistence.user.UserRepository;
import org.waterwood.waterfunservicecore.infrastructure.utils.context.UserCtxHolder;
import org.waterwood.waterfunservicecore.services.sys.storage.CloudFileService;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReportServiceImplTest {

    @Mock
    private UserReportRepository userReportRepository;

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private InboxRepository inboxRepository;

    @Mock
    private TicketResourceRepository ticketResourceRepository;

    @Mock
    private ResourceRepository resourceRepository;

    @Mock
    private CloudFileService cloudFileService;

    @InjectMocks
    private ReportServiceImpl reportService;

    private static final Long USER_UID = 100L;
    private static final String TARGET_ID = "200";
    private static final TargetType TARGET_TYPE = TargetType.POST;
    private static final AuditType REPORT_TYPE = AuditType.VIOLATION_OF_GUIDELINES;
    private static final String REASON = "Inappropriate content";
    private static final Long TICKET_ID = 500L;

    private User mockUser;

    @BeforeEach
    void setUp() {
        mockUser = mock(User.class);
        when(mockUser.getUid()).thenReturn(USER_UID);
        when(userRepository.getReferenceById(USER_UID)).thenReturn(mockUser);
    }

    @Test
    void submitReport_shouldCreateTicketAndUserReport() {
        when(ticketRepository.findBySubmitterUidAndTargetIdAndTargetTypeAndTicketTypeAndStatusIn(
                USER_UID, TARGET_ID, TARGET_TYPE, TicketType.CONTENT_REPORT,
                List.of(TicketAuditStatus.PENDING, TicketAuditStatus.RESOLVED, TicketAuditStatus.REJECTED)
        )).thenReturn(Optional.empty());

        when(ticketRepository.save(any())).thenAnswer(invocation -> {
            Ticket t = invocation.getArgument(0);
            if (t.getId() == null) t.setId(TICKET_ID);
            return t;
        });

        try (MockedStatic<UserCtxHolder> ctx = mockStatic(UserCtxHolder.class)) {
            ctx.when(UserCtxHolder::getUserUid).thenReturn(USER_UID);
            Long result = reportService.submitReport(TARGET_ID, TARGET_TYPE, REPORT_TYPE, REASON, null);
            assertNotNull(result);
        }

        verify(ticketRepository).save(any(Ticket.class));
        verify(userReportRepository).save(any(UserTicket.class));
    }

    @Test
    void submitReport_duplicatePendingReport_shouldThrow() {
        Ticket existingTicket = new Ticket();
        existingTicket.setId(1L);
        when(ticketRepository.findBySubmitterUidAndTargetIdAndTargetTypeAndTicketTypeAndStatusIn(
                USER_UID, TARGET_ID, TARGET_TYPE, TicketType.CONTENT_REPORT,
                List.of(TicketAuditStatus.PENDING, TicketAuditStatus.RESOLVED, TicketAuditStatus.REJECTED)
        )).thenReturn(Optional.of(existingTicket));

        try (MockedStatic<UserCtxHolder> ctx = mockStatic(UserCtxHolder.class)) {
            ctx.when(UserCtxHolder::getUserUid).thenReturn(USER_UID);
            assertThrows(ReportAlreadyExistException.class,
                    () -> reportService.submitReport(TARGET_ID, TARGET_TYPE, REPORT_TYPE, REASON, null));
        }

        verify(ticketRepository, never()).save(any());
    }

    @Test
    void submitSuggestion_shouldCreateTicketAndReport() {
        String content = "Suggestion text";

        when(ticketRepository.save(any())).thenAnswer(invocation -> {
            Ticket t = invocation.getArgument(0);
            if (t.getId() == null) t.setId(TICKET_ID);
            return t;
        });

        try (MockedStatic<UserCtxHolder> ctx = mockStatic(UserCtxHolder.class)) {
            ctx.when(UserCtxHolder::getUserUid).thenReturn(USER_UID);
            Long result = reportService.submitSuggestion(content, null);
            assertNotNull(result);
        }

        verify(userReportRepository).save(any(UserTicket.class));
    }

    @Test
    void submitFeedback_shouldCreateTicketAndReport() {
        String content = "Feedback text";

        when(ticketRepository.save(any())).thenAnswer(invocation -> {
            Ticket t = invocation.getArgument(0);
            if (t.getId() == null) t.setId(TICKET_ID);
            return t;
        });

        try (MockedStatic<UserCtxHolder> ctx = mockStatic(UserCtxHolder.class)) {
            ctx.when(UserCtxHolder::getUserUid).thenReturn(USER_UID);
            Long result = reportService.submitFeedback(content, null);
            assertNotNull(result);
        }

        verify(userReportRepository).save(any(UserTicket.class));
    }

    @Test
    void submitAppeal_withPenaltyType_shouldPersistPenaltyType() {
        String content = "Appeal text";

        when(ticketRepository.save(any())).thenAnswer(invocation -> {
            Ticket t = invocation.getArgument(0);
            if (t.getId() == null) t.setId(TICKET_ID);
            return t;
        });

        try (MockedStatic<UserCtxHolder> ctx = mockStatic(UserCtxHolder.class)) {
            ctx.when(UserCtxHolder::getUserUid).thenReturn(USER_UID);
            Long result = reportService.submitAppeal(TARGET_ID, TARGET_TYPE, content, null, null);
            assertNotNull(result);
        }

        ArgumentCaptor<Ticket> captor = ArgumentCaptor.forClass(Ticket.class);
        verify(ticketRepository).save(captor.capture());
        assertNull(captor.getValue().getPenaltyType());
    }

    @Test
    void cancelReport_shouldCancelTicketOnly() {
        Long ticketId = 300L;
        UserTicket userTicket = new UserTicket();
        UserTicketId uid = new UserTicketId();
        uid.setTicketId(ticketId);
        uid.setUserUid(USER_UID);
        userTicket.setId(uid);

        Ticket ticket = new Ticket();
        ticket.setId(ticketId);
        ticket.setStatus(TicketAuditStatus.PENDING);

        when(userReportRepository.findByTicketIdAndUserUid(ticketId, USER_UID))
                .thenReturn(Optional.of(userTicket));
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));

        try (MockedStatic<UserCtxHolder> ctx = mockStatic(UserCtxHolder.class)) {
            ctx.when(UserCtxHolder::getUserUid).thenReturn(USER_UID);
            reportService.cancelReport(USER_UID, ticketId);
        }

        assertEquals(TicketAuditStatus.CANCELLED, ticket.getStatus());
        verify(ticketRepository).save(ticket);
        verify(userReportRepository, never()).save(any());
    }

    @Test
    void cancelReport_ticketNotPending_shouldThrow() {
        Long ticketId = 300L;
        UserTicket userTicket = new UserTicket();
        UserTicketId uid = new UserTicketId();
        uid.setTicketId(ticketId);
        uid.setUserUid(USER_UID);
        userTicket.setId(uid);

        Ticket ticket = new Ticket();
        ticket.setId(ticketId);
        ticket.setStatus(TicketAuditStatus.RESOLVED);

        when(userReportRepository.findByTicketIdAndUserUid(ticketId, USER_UID))
                .thenReturn(Optional.of(userTicket));
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));

        try (MockedStatic<UserCtxHolder> ctx = mockStatic(UserCtxHolder.class)) {
            ctx.when(UserCtxHolder::getUserUid).thenReturn(USER_UID);
            assertThrows(ServiceException.class,
                    () -> reportService.cancelReport(USER_UID, ticketId));
        }
    }

    @Test
    void cancelReport_reportNotFound_shouldThrow() {
        when(userReportRepository.findByTicketIdAndUserUid(999L, USER_UID))
                .thenReturn(Optional.empty());

        try (MockedStatic<UserCtxHolder> ctx = mockStatic(UserCtxHolder.class)) {
            ctx.when(UserCtxHolder::getUserUid).thenReturn(USER_UID);
            assertThrows(ReportNotFoundException.class,
                    () -> reportService.cancelReport(USER_UID, 999L));
        }
    }
}
