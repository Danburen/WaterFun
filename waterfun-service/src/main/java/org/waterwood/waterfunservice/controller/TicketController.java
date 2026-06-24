package org.waterwood.waterfunservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;
import org.waterwood.api.ApiResponse;
import org.waterwood.waterfunservice.api.request.CreateUserReportReq;
import org.waterwood.waterfunservice.api.response.ReportResponse;
import org.waterwood.waterfunservice.api.response.ticket.TicketStatsResponse;
import org.waterwood.waterfunservice.api.response.ticket.UserTicketDetailResponse;
import org.waterwood.waterfunservice.api.response.ticket.UserTicketListResponse;
import org.waterwood.waterfunservice.service.report.ReportService;
import org.waterwood.waterfunservicecore.entity.ticket.TicketAuditStatus;
import org.waterwood.waterfunservicecore.entity.ticket.TicketType;
import org.waterwood.waterfunservicecore.exception.ServiceException;
import org.waterwood.waterfunservicecore.infrastructure.utils.context.AuthContext;
import org.waterwood.waterfunservicecore.infrastructure.utils.context.UserCtxHolder;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/tickets")
public class TicketController {
    private final ReportService reportService;

    @PostMapping
    public ApiResponse<ReportResponse> create(@Valid @RequestBody CreateUserReportReq req) {
        AuthContext ctx = UserCtxHolder.safeGet()
                .orElseThrow(() -> new ServiceException("Authentication required"));
        Long userUid = ctx.getUserUid();
        List<String> uuids = req.getResourceUuids();
        Long taskId = switch (req.getTicketType()) {
            case CONTENT_REPORT -> reportService.submitReport(
                    req.getTargetId(), req.getTargetType(), req.getType(), req.getReason(), uuids);
            case SUGGESTION -> reportService.submitSuggestion(req.getReason(), uuids);
            case FEATURE_FEEDBACK -> reportService.submitFeedback(req.getReason(), uuids);
            case ACCOUNT_APPEAL -> reportService.submitAppeal(
                    req.getTargetId(), req.getTargetType(), req.getReason(), req.getPenaltyType(), uuids);
        };
        return ApiResponse.success(new ReportResponse(taskId));
    }

    @PostMapping("/{id}/cancel")
    public ApiResponse<Void> cancel(@PathVariable Long id) {
        Long userUid = UserCtxHolder.getUserUid();
        reportService.cancelReport(userUid, id);
        return ApiResponse.success();
    }

    @GetMapping
    public ApiResponse<Page<UserTicketListResponse>> list(
            @RequestParam(required = false) TicketType ticketType,
            @RequestParam(required = false) TicketAuditStatus status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long userUid = UserCtxHolder.getUserUid();
        Pageable pageable = PageRequest.of(Math.max(0, page - 1), Math.min(size, 50));
        return ApiResponse.success(reportService.listUserTickets(userUid, ticketType, status, pageable));
    }

    @GetMapping("/{id}")
    public ApiResponse<UserTicketDetailResponse> detail(@PathVariable Long id) {
        Long userUid = UserCtxHolder.getUserUid();
        return ApiResponse.success(reportService.getUserTicketDetail(userUid, id));
    }

    @GetMapping("/stats")
    public ApiResponse<TicketStatsResponse> stats() {
        AuthContext ctx = UserCtxHolder.safeGet()
                .orElseThrow(() -> new ServiceException("Authentication required"));
        Long userUid = ctx.getUserUid();
        return ApiResponse.success(reportService.getUserTicketStats(userUid));
    }
}
