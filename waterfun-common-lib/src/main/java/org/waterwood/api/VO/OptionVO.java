package org.waterwood.api.VO;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;

@Data
@NoArgsConstructor
public class OptionVO<ID extends Serializable> {
    private ID id;
    private String code;
    private String name;
    private Boolean disabled = false;
    private Long usageCount;

    public OptionVO(ID id, String code, String name, Boolean disabled) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.disabled = disabled;
    }

    public OptionVO(ID id, String code, String name, Boolean disabled, Long usageCount) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.disabled = disabled;
        this.usageCount = usageCount;
    }

    public static <ID extends Serializable> OptionVO<ID> of(ID id, String name, String code, boolean b) {
        OptionVO<ID> vo = new OptionVO<>();
        vo.id = id;
        vo.code = code;
        vo.name = name;
        vo.disabled = b;
        return vo;
    }

    public static OptionVO<Integer> ofInteger(Integer id, String name, String code, boolean b) {
        return of(id, name, code, b);
    }

    public ExpirableOptionVO<ID> toExpirableOption(Instant expireAt) {
        return new ExpirableOptionVO<>(getId(), getName(), getCode(), expireAt);
    }
}