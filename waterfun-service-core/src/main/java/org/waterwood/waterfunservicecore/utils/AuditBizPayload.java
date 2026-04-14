package org.waterwood.waterfunservicecore.utils;

import org.jetbrains.annotations.Nullable;

import java.io.Serializable;

public record AuditBizPayload(String biz, Serializable bizId,@Nullable String type) {
}
