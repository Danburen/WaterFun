package org.waterwood.utils;

import java.util.Arrays;
import java.util.Objects;

public final class StringUtil {
    /**
     * Get non null string array
     * @param strings strings
     * @return non null string array
     */
    public static String[] noNullStringArray(String... strings) {
        return Arrays.stream(strings)
                .filter(Objects::nonNull)
                .toArray(String[]::new);
    }

    public static boolean isBlank(String string){
        return string == null || string.trim().isEmpty();
    }

    public static boolean isNotBlank(String string){
        return ! isBlank(string);
    }

    public static String buildPath(char delimiter, String... segments) {
        StringBuilder sb = new StringBuilder();
        for (String segment : segments) {
            sb.append(segment).append(delimiter);
        }
        return sb.substring(0, sb.length() - 1);
    }

    public static String fasterBuildPath(char delim, String... segs) {
        if (segs == null || segs.length == 0) return "";
        if (segs.length == 1) return segs[0] == null ? "" : segs[0];
        // 1. 先算总长度
        int total = 0;
        for (String s : segs) {
            if (s == null) s = "";
            total += s.length();
        }
        total += (segs.length - 1);          // 分隔符个数
        // 2. 一次性申请缓冲区
        char[] buf = new char[total];
        int pos = 0;
        // 3. 拼第一段（前面不带分隔符）
        String s = segs[0];
        if (s != null && !s.isEmpty()) {
            s.getChars(0, s.length(), buf, pos);
            pos += s.length();
        }
        // 4. 拼后续段（前面带分隔符）
        for (int i = 1; i < segs.length; i++) {
            buf[pos++] = delim;
            s = segs[i];
            if (s != null && !(s.isEmpty())) {
                s.getChars(0, s.length(), buf, pos);
                pos += s.length();
            }
        }
        // 5.  new String 内部直接共用 buf，不拷贝
        return new String(buf);
    }
}
