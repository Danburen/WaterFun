package org.waterwood.waterfunservicecore.api.moderation;

import org.waterwood.waterfunservicecore.entity.audit.AuditContentFormat;

public interface AuditPayload {
    String toJson();
    AuditContentFormat getFormat();
}
