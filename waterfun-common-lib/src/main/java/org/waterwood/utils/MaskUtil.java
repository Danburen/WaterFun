package org.waterwood.utils;

public class MaskUtil {
    public static String maskPhone(String raw) {
        if (raw == null) return "";
        String digits = raw.replaceAll("\\D", "");
        if (digits.length() != 11) return raw;
        return digits.substring(0, 3) + "****" + digits.substring(7);
    }

    public static String maskEmail(String raw) {
        if (raw == null || !raw.contains("@")) return "";
        int at = raw.indexOf('@');
        String prefix = raw.substring(0, at);
        String suffix = raw.substring(at);
        if (prefix.length() <= 2) {
            return prefix.charAt(0) + "***" + suffix;
        }
        return prefix.charAt(0) + "***"
                + prefix.charAt(prefix.length() - 1) + suffix;
    }
}
