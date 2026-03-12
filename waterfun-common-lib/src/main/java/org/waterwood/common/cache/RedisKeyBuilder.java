package org.waterwood.common.cache;

import org.waterwood.utils.StringUtil;

public class RedisKeyBuilder {
    
    private RedisKeyBuilder() {
    }

    public static String buildKey(String... segments) {
       return StringUtil.fasterBuildPath(':', segments);
    }
}
