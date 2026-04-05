package org.waterwood.api.VO;

import java.io.Serializable;
import java.time.Instant;


public interface ToOptionVO <T extends Serializable> {
    T getId();
    String  getName();
    String  getCode();
    default OptionVO<T> toOption() {
        return OptionVO.of(getId(), getName(), getCode(), false);
    }

    default ExpirableOptionVO<T> toExpirableOption(Instant expiration) {
        return new ExpirableOptionVO<>(getId(), getName(), getCode(), expiration);
    }
}
