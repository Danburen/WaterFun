package org.waterwood.waterfunservicecore.api;

import java.time.Instant;

public interface ToDictOption{
    Long    getId();
    String  getName();
    String  getCode();
    Instant getExpiresAt();
    default DictOption toDictOption() {
        return new DictOption(getId(), getName(), getCode(), getExpiresAt());
    }
}
