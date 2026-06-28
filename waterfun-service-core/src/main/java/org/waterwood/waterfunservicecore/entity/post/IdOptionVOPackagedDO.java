package org.waterwood.waterfunservicecore.entity.post;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.waterwood.api.VO.OptionVO;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class IdOptionVOPackagedDO<ID extends Serializable, OID extends Serializable>{
    private ID id;
    private OptionVO<OID> optionVo;
    private Long usageCount;

    public IdOptionVOPackagedDO(ID id, OID optionID, String slug, String name, Boolean disabled) {
        this.id = id;
        this.optionVo = new OptionVO<>(optionID, slug, name, disabled);
    }

    public IdOptionVOPackagedDO(ID id, OID optionID, String slug, String name, Boolean disabled, Long usageCount) {
        this.id = id;
        this.optionVo = new OptionVO<>(optionID, slug, name, disabled);
        this.optionVo.setUsageCount(usageCount);
        this.usageCount = usageCount;
    }
}
