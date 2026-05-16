package org.waterwood.api.VO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OptionVO<ID extends Serializable> {
    private ID id;
    private String code;
    private String name;
    @Builder.Default
    private Boolean disabled = false;

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