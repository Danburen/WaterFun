package org.waterwood.waterfunservicecore.utils;

import org.waterwood.api.BaseResponseCode;
import org.waterwood.common.exceptions.BizException;

public final class AuditTargetKeyGenerator {
    public static String avatar(Long userUid){
        return "user:" + userUid + ":avatar";
    }

    public static AuditBizPayload<Long> parseLongBiz(String key) {
        String[] parts = key.split(":");

        if (parts.length < 2 || parts.length > 3) {
            throw new BizException(BaseResponseCode.INVALID_KEY);
        }

        return new AuditBizPayload<>(
                parts[0],           // biz = "user"
                Long.parseLong(parts[1]),  // userUid = 10086
                parts.length == 3 ? parts[2] : null      // type = "avatar"
        );
    }
}
