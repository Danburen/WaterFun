package org.waterwood.api.VO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;

@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
public class ExpirableOptionVO<T extends Serializable> extends OptionVO<T> implements Serializable {
    private Instant expiresAt;

    public ExpirableOptionVO(T id, String name, String code, Instant expiresAt) {
        super(id, code, name, false);
        this.expiresAt = expiresAt;
    }

    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }

    @Override
    public Boolean getDisabled() {
        return super.getDisabled() || isExpired();
    }

    public static <T extends Serializable> ExpirableOptionVO<T> of(
            T id, String name, String code, Instant expiresAt) {
        ExpirableOptionVO<T> vo = new ExpirableOptionVO<T>();
        vo.setId(id);
        vo.setName(name);
        vo.setCode(code);
        vo.setExpiresAt(expiresAt);
        vo.setDisabled(false);
        return vo;
    }
}
