package org.waterwood.waterfunservicecore.entity.post;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.waterwood.api.VO.OptionVO;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class IdOptionVOPackagedDO<ID extends Serializable> {
    private ID id;
    private OptionVO<ID> optionVo;

    public IdOptionVOPackagedDO(ID id, String slug, String name, Boolean disabled) {
        this.id = id;
        this.optionVo = new OptionVO<>(id, slug, name, disabled);
    }
}
