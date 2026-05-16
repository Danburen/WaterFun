package org.waterwood.waterfunservicecore.utils;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;

public record BizPayload<T extends Serializable>(String biz, T bizId,@NotNull String type) {
}
