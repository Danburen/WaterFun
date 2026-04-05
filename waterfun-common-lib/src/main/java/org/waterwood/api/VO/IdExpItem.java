package org.waterwood.api.VO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
/**
 * A class which contains ids and expireAt, used for batch delete or batch expire
 */
public class IdExpItem<T> implements Serializable {
    private T id;
    private Instant expireAt;
}
