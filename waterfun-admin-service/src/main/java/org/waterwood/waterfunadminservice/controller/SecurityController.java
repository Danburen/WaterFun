package org.waterwood.waterfunadminservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.bind.annotation.*;
import org.waterwood.api.ApiResponse;
import org.waterwood.api.VO.BatchResult;
import org.waterwood.waterfunadminservice.api.request.audit.DeleteAuditLogRequest;
import org.waterwood.waterfunadminservice.api.request.security.BanIpRequest;
import org.waterwood.waterfunadminservice.api.request.security.UnbanIpRequest;
import org.waterwood.waterfunadminservice.api.response.AuditLogResponse;
import org.waterwood.waterfunadminservice.api.response.IpBanResponse;
import org.waterwood.waterfunadminservice.service.AuditLogService;
import org.waterwood.waterfunadminservice.service.IpBanService;
import org.waterwood.waterfunservicecore.entity.audit.AuditLog;
import org.waterwood.waterfunservicecore.entity.audit.AuditLogActionType;
import org.waterwood.waterfunservicecore.entity.audit.AuditLogStatusType;
import org.waterwood.waterfunservicecore.entity.IpBan;
import org.waterwood.waterfunservicecore.entity.spec.AuditLogSpec;
import org.waterwood.waterfunservicecore.entity.spec.IpBanSpec;

import java.time.Instant;

@RestController
@RequestMapping("/api/admin/security")
@RequiredArgsConstructor
public class SecurityController {
    private final IpBanService ipBanService;
    private final AuditLogService auditLogService;

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

    @GetMapping("/ban/{id}")
    public ApiResponse<IpBanResponse> getIpBan(@PathVariable Long id) {
        return ApiResponse.success(ipBanService.getIpBan(id));
    }

    @PostMapping("/ban")
    public ApiResponse<IpBanResponse> banIp(@RequestBody @Valid BanIpRequest req) {
        return ApiResponse.success(ipBanService.banIp(req.getIp(), req.getReason(), req.getExpiresAt()));
    }

    @PostMapping("/unban")
    public ApiResponse<Void> unbanIp(@RequestBody @Valid UnbanIpRequest req) {
        ipBanService.unbanIp(req.getIp());
        return ApiResponse.success();
    }

    @DeleteMapping("/ban/{id}")
    public ApiResponse<Void> deleteIpBan(@PathVariable Long id) {
        ipBanService.deleteIpBan(id);
        return ApiResponse.success();
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
}
