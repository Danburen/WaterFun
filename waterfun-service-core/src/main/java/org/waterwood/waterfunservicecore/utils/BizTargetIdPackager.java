package org.waterwood.waterfunservicecore.utils;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.waterwood.api.BaseResponseCode;
import org.waterwood.common.KeyConstants;
import org.waterwood.common.exceptions.BizException;

import java.io.Serializable;

public final class BizTargetIdPackager {
    public static String ofUser(Long userUid,@NotNull String domain){
        return KeyConstants.USER + ":" + userUid + ":" + domain.toLowerCase();
    }

    public static BizPayload<Long> parseLongBiz(String key) {
        String[] parts = key.split(":");

        if (parts.length < 2 || parts.length > 3) {
            throw new BizException(BaseResponseCode.INVALID_KEY);
        }

        return new BizPayload<>(
                parts[0],           // biz = "user"
                Long.parseLong(parts[1]), // userUid = 10086
                parts[2]
        );
    }

    public static <T extends Serializable> BizPayload<T> parseBiz(String key, Class<T> idType) {
        String[] parts = key.split(":");

        if (parts.length < 2 || parts.length > 3) {
            throw new BizException(BaseResponseCode.INVALID_KEY);
        }

        T bizId;
        try {
            if (idType == Long.class) {
                bizId = idType.cast(Long.parseLong(parts[1]));
            } else if (idType == Integer.class) {
                bizId = idType.cast(Integer.parseInt(parts[1]));
            } else if (idType == String.class) {
                bizId = idType.cast(parts[1]);
            } else {
                throw new BizException(BaseResponseCode.UNSUPPORTED_ID_TYPE, parts[1]);
            }
        } catch (NumberFormatException e) {
            throw new BizException(BaseResponseCode.INVALID_KEY, "Failed to parse bizId: " + e.getMessage());
        }

        return new BizPayload<>(
                parts[0],           // biz
                bizId,             // bizId
                parts[2]
        );
    }

    public static  <T extends Serializable> String fromPayload(BizPayload<T> payload) {
        return String.join(":",
                payload.biz(),
                payload.bizId().toString(),
                payload.type()
        );
    }
}
