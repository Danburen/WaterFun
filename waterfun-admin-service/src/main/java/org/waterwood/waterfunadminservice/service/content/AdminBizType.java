package org.waterwood.waterfunadminservice.service.content;

import lombok.Getter;
import org.waterwood.waterfunservicecore.api.BizType;

@Getter
public enum AdminBizType implements BizType {
    BANNER_IMAGE("banner_image"),
    POST_COVERAGE_IMAGE("post_coverage_image"),
    POST_CONTENT_IMAGE("post_content_image"),;

    private final String code;
    AdminBizType(String code) {
        this.code = code;
    }

    public static AdminBizType fromCode(String code) {
        return BizType.fromCode(AdminBizType.class, code);
    }
}
