package org.waterwood.waterfunservicecore.utils;

import org.jetbrains.annotations.Nullable;

import java.io.Serializable;

public record AuditBizPayload<T>(String biz, T bizId,@Nullable String type) {
}
