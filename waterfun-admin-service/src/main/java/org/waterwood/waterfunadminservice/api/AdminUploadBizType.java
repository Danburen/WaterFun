package org.waterwood.waterfunadminservice.api;

import lombok.Getter;
import org.waterwood.waterfunservicecore.api.BizType;

@Getter
public enum AdminUploadBizType implements BizType {
    BANNER_IMAGE("banner_image"),;

    private final String code;
    AdminUploadBizType(String code) {
        this.code = code;
    }
}
