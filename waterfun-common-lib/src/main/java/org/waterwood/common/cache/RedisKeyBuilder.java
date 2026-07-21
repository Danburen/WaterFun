package org.waterwood.common.cache;

public class RedisKeyBuilder {
    
    private RedisKeyBuilder() {
    }

    public static String build(Object... segments) {
        if (segments == null || segments.length == 0) return "";
        if (segments.length == 1) return segments[0] == null ? "" : segments[0].toString();
        StringBuilder sb = new StringBuilder(64);
        for (int i = 0; i < segments.length; i++) {
            if (i > 0) sb.append(':');
            if (segments[i] != null) sb.append(segments[i].toString());
        }
        return sb.toString();
    }
}
