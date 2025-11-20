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
}
