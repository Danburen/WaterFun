package org.waterwood.waterfunadminservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import org.waterwood.api.ApiResponse;
import org.waterwood.api.VO.BatchResult;
import org.waterwood.waterfunadminservice.api.request.audit.DeleteAuditLogRequest;
import org.waterwood.waterfunadminservice.api.response.AuditLogResponse;
import org.waterwood.waterfunadminservice.api.response.IpBanResponse;
import org.waterwood.waterfunadminservice.api.response.SiteStatisticResponse;
import org.waterwood.waterfunadminservice.service.AuditLogService;
import org.waterwood.waterfunadminservice.service.IpBanService;
import org.waterwood.waterfunadminservice.service.StatisticService;
import org.waterwood.waterfunservicecore.entity.AuditLog;
import org.waterwood.waterfunservicecore.entity.AuditLogActionType;
import org.waterwood.waterfunservicecore.entity.AuditLogStatusType;
import org.waterwood.waterfunservicecore.entity.IpBan;
import org.waterwood.waterfunservicecore.entity.spec.AuditLogSpec;
import org.waterwood.waterfunservicecore.entity.spec.IpBanSpec;

import java.time.Instant;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/admin/security")
@RequiredArgsConstructor
public class SecurityController {
    private final IpBanService ipBanService;
    private final AuditLogService auditLogService;
    private final StatisticService statisticService;

    @GetMapping("/list")
    public ApiResponse<Page<IpBanResponse>> listBanResponses(
            @RequestParam(required = false) String ip,
            @RequestParam(required = false) String reason,
            @RequestParam(required = false) Instant bannedAtStart,
            @RequestParam(required = false) Instant bannedAtEnd,
            @RequestParam(required = false) Instant expiresStart,
            @RequestParam(required = false) Instant expiresEnd,
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "20") int size
    ){
        Specification<IpBan> spec = IpBanSpec.of(ip, reason, bannedAtStart, bannedAtEnd, expiresStart, expiresEnd);
        Pageable pageable = PageRequest
                .of(Math.max(page -1, 0), Math.max(size, 100))
                .withSort(Sort.Direction.DESC, "bannedAt");
        return ApiResponse.success(
            ipBanService.listIpBanResponse(spec, pageable)
        );
    }

    @GetMapping("/audit-log/list")
    public ApiResponse<Page<AuditLogResponse>> listAuditLogs(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) AuditLogActionType action,
            @RequestParam(required = false) String ip,
            @RequestParam(required = false) AuditLogStatusType status,
            @RequestParam(required = false) Instant createdAtStart,
            @RequestParam(required = false) Instant createdAtEnd,
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest
                .of(Math.max(page -1, 0), Math.max(size, 100))
                .withSort(Sort.Direction.DESC, "createdAt");
        Specification<AuditLog> spec = AuditLogSpec.of(userId, username, action, ip, status, createdAtStart, createdAtEnd);
        return ApiResponse.success(auditLogService.listAuditLogs(spec, pageable));
    }

    @GetMapping("/audit-log/{id}")
    public ApiResponse<AuditLogResponse> getAuditLog(@PathVariable Long id) {
        return ApiResponse.success(auditLogService.getAuditLog(id));
    }

    @DeleteMapping("/audit-log")
    public ApiResponse<BatchResult> deleteAuditLogs(@RequestBody @Valid DeleteAuditLogRequest req) {
        return ApiResponse.success(auditLogService.deleteAuditLogs(req.getLogIds()));
    }

    @DeleteMapping("/audit-log/{id}")
    public ApiResponse<Void> deleteAuditLog(@PathVariable Long id) {
        auditLogService.deleteAuditLog(id);
        return ApiResponse.success();
    }

    @GetMapping("/statistic/list")
    public ApiResponse<Page<SiteStatisticResponse>> listStatistics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @PageableDefault(sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.success(statisticService.listStatistics(startDate, endDate, pageable));
    }

    @GetMapping("/statistic/latest")
    public ApiResponse<SiteStatisticResponse> getLatestStatistic() {
        return ApiResponse.success(statisticService.getLatestStatistic());
    }

    @GetMapping("/statistic/{date}")
    public ApiResponse<SiteStatisticResponse> getStatistic(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ApiResponse.success(statisticService.getStatistic(date));
    }
}
