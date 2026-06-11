package org.waterwood.waterfunservicecore.api.moderation;

import org.waterwood.api.Mappable;
import org.waterwood.common.io.ResourceType;
import org.waterwood.utils.JsonUtil;
import org.waterwood.waterfunservicecore.entity.audit.AuditContentFormat;

public interface AuditPayload {
    String toJson();
    AuditContentFormat getFormat();
}
