package org.waterwood.api.VO;

import java.io.Serializable;
import java.time.Instant;

public interface ToExpirableDictOption <T extends Serializable> extends ToOptionVO<T>{
    T getId();
    String  getName();
    String  getCode();
    Instant getExpiresAt();
    default ExpirableOptionVO<T> toExpirableOptionVO() {
        return new ExpirableOptionVO<>(getId(), getName(), getCode(), getExpiresAt());
    }
}
