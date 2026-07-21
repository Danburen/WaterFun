package org.waterwood.common.constratin;

import org.waterwood.common.RedisKeyPrefix;
import org.waterwood.common.cache.RedisKeyBuilder;

public final class UserKeyBuilder {

    private UserKeyBuilder() {}

    public static String userAccessDevice(long userUid, String deviceId) {
        return RedisKeyBuilder.build(
                RedisKeyPrefix.USER,
                String.valueOf(userUid),
                "device",
                deviceId,
                "access"
        );
    }
}
