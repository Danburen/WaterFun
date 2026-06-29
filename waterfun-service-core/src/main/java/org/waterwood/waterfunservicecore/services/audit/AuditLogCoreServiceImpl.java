package org.waterwood.waterfunservicecore.services.audit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.waterwood.waterfunservicecore.entity.audit.AuditLog;
import org.waterwood.waterfunservicecore.entity.audit.AuditLogActionType;
import org.waterwood.waterfunservicecore.entity.audit.AuditLogStatusType;
import org.waterwood.waterfunservicecore.infrastructure.persistence.AuditLogRepository;
import org.waterwood.waterfunservicecore.infrastructure.utils.context.UserCtxHolder;
import org.waterwood.waterfunservicecore.services.location.IpLocationService;

import java.time.Instant;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogCoreServiceImpl implements AuditLogCoreService {

    private final AuditLogRepository auditLogRepository;
    private final IpLocationService ipLocationService;

    @Override
    public void record(Long userId, String username, AuditLogActionType action) {
        record(userId, username, action, AuditLogStatusType.SUCCESS, null);
    }

    @Override
    public void record(Long userId, String username, AuditLogActionType action,
                       AuditLogStatusType status, String failReason) {
        try {
            AuditLog log = new AuditLog();
            log.setUserId(userId);
            log.setUsername(username);
            log.setAction(action);
            log.setStatus(status);
            log.setFailReason(failReason != null && failReason.length() > 64 ? failReason.substring(0, 64) : failReason);
            log.setCreatedAt(Instant.now());

            String ip = resolveClientIp();
            log.setIp(ip);

            Map<String, String> location = ipLocationService.lookup(ip);
            log.setCountry(location.getOrDefault("country", ""));
            log.setProvince(location.getOrDefault("province", ""));
            log.setCity(location.getOrDefault("city", ""));

            auditLogRepository.save(log);
        } catch (Exception e) {
            log.error("Failed to save audit log", e);
        }
    }

    private String resolveClientIp() {
        try {
            return UserCtxHolder.getClientIp();
        } catch (Exception e) {
            return "0.0.0.0";
        }
    }
}
