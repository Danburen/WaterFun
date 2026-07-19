package org.waterwood.waterfunservice.service.log;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.waterwood.utils.StringUtil;
import org.waterwood.waterfunservicecore.api.req.auth.DeviceInfoReq;
import org.waterwood.waterfunservicecore.entity.audit.AuditLog;
import org.waterwood.waterfunservicecore.entity.audit.AuditLogActionType;
import org.waterwood.waterfunservicecore.entity.audit.AuditLogStatusType;
import org.waterwood.waterfunservicecore.infrastructure.persistence.AuditLogRepository;
import org.waterwood.waterfunservicecore.services.location.IpLocationService;

import java.time.Instant;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final IpLocationService ipLocationService;

    @Transactional
    public void record(Long userId, String username, AuditLogActionType action,
                       HttpServletRequest request, DeviceInfoReq deviceInfo) {
        record(userId, username, action, AuditLogStatusType.SUCCESS, null, request, deviceInfo);
    }

    @Transactional
    public void record(Long userId, String username, AuditLogActionType action,
                       AuditLogStatusType status, String failReason,
                       HttpServletRequest request, DeviceInfoReq deviceInfo) {
        try {
            AuditLog log = new AuditLog();
            log.setUserId(userId);
            log.setUsername(username);
            log.setAction(action);
            log.setStatus(status);
            log.setFailReason(failReason);
            log.setCreatedAt(Instant.now());

            String ip = resolveClientIp(request);
            log.setIp(ip);

            Map<String, String> location = ipLocationService.lookup(ip);
            log.setCountry(location.getOrDefault("country", ""));
            log.setProvince(location.getOrDefault("province", ""));
            log.setCity(location.getOrDefault("city", ""));

            if (deviceInfo != null) {
                log.setDeviceInfo(deviceInfo.toMap());
            }

            auditLogRepository.save(log);
        } catch (Exception e) {
            log.error("Failed to save audit log", e);
        }
    }

    private String resolveClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Real-Client-Ip");
        if (StringUtil.isNotBlank(ip)) return ip;

        ip = request.getHeader("X-Forwarded-For");
        if (StringUtil.isNotBlank(ip) && !"unknown".equalsIgnoreCase(ip)) {
            return ip.split(",")[0].trim();
        }

        ip = request.getHeader("X-Real-IP");
        if (StringUtil.isNotBlank(ip) && !"unknown".equalsIgnoreCase(ip)) {
            return ip.trim();
        }

        return request.getRemoteAddr();
    }
}
