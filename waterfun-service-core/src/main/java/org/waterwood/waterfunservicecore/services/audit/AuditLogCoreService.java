package org.waterwood.waterfunservicecore.services.audit;

import org.waterwood.waterfunservicecore.api.req.auth.DeviceInfoReq;
import org.waterwood.waterfunservicecore.entity.audit.AuditLogActionType;

public interface AuditLogCoreService {

    /** Basic success audit (no device info). */
    void recordSuccess(Long userId, String username, AuditLogActionType action);

    /** Failure audit with reason (no device info). */
    void recordFailure(Long userId, String username, AuditLogActionType action, String failReason);

    /** Success audit with full device info. */
    void recordSuccess(Long userId, String username, AuditLogActionType action, DeviceInfoReq deviceInfo);

    /** Failure audit with reason and full device info. */
    void recordFailure(Long userId, String username, AuditLogActionType action, String failReason, DeviceInfoReq deviceInfo);
}
