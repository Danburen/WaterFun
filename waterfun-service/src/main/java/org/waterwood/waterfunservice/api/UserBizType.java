package org.waterwood.waterfunservice.api;

import lombok.Getter;
import org.waterwood.waterfunservicecore.api.BizType;
import org.waterwood.waterfunservicecore.entity.audit.TargetType;

@Getter
public enum UserBizType implements BizType {
    AVATAR("avatar"),
    POST_COVERAGE_IMAGE("post_coverage_image"),
    POST_CONTENT_IMAGE("post_content_image"),;
    private final String code;
    UserBizType(String code) {
        this.code = code;
    }

    public static UserBizType fromCode(String code) {
        return BizType.fromCode(UserBizType.class, code);
    }
}
