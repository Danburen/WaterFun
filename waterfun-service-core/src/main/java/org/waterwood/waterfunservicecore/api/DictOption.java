package org.waterwood.waterfunservicecore.api;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.time.Instant;

@Data
@AllArgsConstructor
public class DictOption implements Serializable {
    private Serializable id;
    private String label;
    private @Nullable String code;
    private @Nullable Instant expiresAt;
}
