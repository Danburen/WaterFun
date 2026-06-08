package org.waterwood.waterfunadminservice.service.content;

import lombok.Getter;
import org.waterwood.waterfunservicecore.api.BizType;
import org.waterwood.waterfunservicecore.entity.audit.TargetType;

@Getter
public enum AdminBizType implements BizType {
    BANNER_IMAGE("banner_image"),;

    private final String code;
    AdminBizType(String code) {
        this.code = code;
    }
}
