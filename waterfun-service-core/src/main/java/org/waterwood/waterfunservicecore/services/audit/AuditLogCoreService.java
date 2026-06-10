package org.waterwood.waterfunservicecore.services.audit;

import org.waterwood.waterfunservicecore.entity.audit.AuditLogActionType;
import org.waterwood.waterfunservicecore.entity.audit.AuditLogStatusType;

public interface AuditLogCoreService {

    void record(Long userId, String username, AuditLogActionType action);

    void record(Long userId, String username, AuditLogActionType action, AuditLogStatusType status, String failReason);
}
