package org.waterwood.waterfunadminservice.service.content;

import lombok.Getter;
import org.waterwood.waterfunservicecore.api.BizType;

@Getter
public enum AdminBizType implements BizType {
    BANNER_COVERAGE("banner_coverage"),;

    private final String code;
    AdminBizType(String code) {
        this.code = code;
    }
}
