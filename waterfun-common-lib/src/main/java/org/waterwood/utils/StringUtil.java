package org.waterwood.utils;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class StringUtil {
    public StringUtil() {}
    private static final Pattern RES_PATTERN = Pattern.compile("res://([a-fA-F0-9\\-]+)");
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

    public static void isBlankThen(String string, Runnable runnable){
        if(isBlank(string)){
            runnable.run();
        }
    }

    public static boolean isNotBlank(String string){
        return ! isBlank(string);
    }

    public static String buildPath(char delimiter, String a,String b) {
        return a + delimiter + b;
    }

    public static String buildPath(char delimiter, String a, String b, String c) {
        return a + delimiter + b + delimiter + c;
    }

    public static String buildPath(char delim, String... segs) {
        if (segs == null || segs.length == 0) return "";
        if (segs.length == 1) return segs[0] == null ? "" : segs[0];

        StringBuilder sb = new StringBuilder(64);
        for (int i = 0; i < segs.length; i++) {
            if (i > 0) sb.append(delim);
            if (segs[i] != null) sb.append(segs[i]);
        }
        return sb.toString();
    }

    public static String noDashUUIDString(UUID resourceUUID) {
        return resourceUUID.toString().replace("-", "");
    }

    public static String noDashRandomUUIDString() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * Extra resource placeholders in format of res://{uuid} from content, and return the uuid set
     * @param content raw content
     * @return set of uuids
     */
    public static Set<String> extraResPlaceholders(String content) {
        if(isBlank(content)){
            return Collections.emptySet();
        }

        Set<String> result = new HashSet<>();
        Matcher m = RES_PATTERN.matcher(content);
        while (m.find()) {
            result.add(m.group(1));
        }
        return result;
    }

    /**
     * Extra resource urls in format of res://xxx
     * @param content raw content
     * @return set of urls
     */
    public static Set<String> extractResUrls(String content) {
        if (content == null || content.isEmpty()) {
            return Set.of();
        }
        Set<String> result = new HashSet<>();
        Matcher matcher = RES_PATTERN.matcher(content);
        while (matcher.find()) {
            result.add(matcher.group(0));
        }
        return result;
    }

    /**
     * Replaces all {@code res://<uuid>} placeholders in the given content with their corresponding URLs.
     *
     * <p>Scans the content incrementally using regex matching. For each matched {@code res://<<uuid>},
     * looks up the UUID in the provided map and substitutes it with the associated URL.
     * If a UUID is not found in the map, the original placeholder is preserved as fallback.
     *
     * <p>Uses {@link Matcher#quoteReplacement} to safely escape special characters ({@code $} or {@code \})
     * in replacement URLs, preventing regex substitution syntax errors.
     *
     * @param content   the raw content containing {@code res://<<uuid>} placeholders;
     *                  may be null or blank, returned as-is
     * @param uuidToUrl a map of UUID strings to their resolved URLs; missing keys are ignored
     * @return the content with all resolvable placeholders replaced by URLs,
     *         or the original string if no placeholders exist
     *
     * @see Matcher#appendReplacement(StringBuffer, String)
     * @see Matcher#appendTail(StringBuffer)
     */
    /**
     * Generate a fallback summary from post content when no explicit summary is provided.
     * Image placeholders (markdown or res://) are replaced with "【图片】",
     * remaining markdown syntax is stripped, HTML tags are removed, and text is truncated to maxLen.
     *
     * @param summary explicit summary (returned as-is if non-blank)
     * @param content raw post content
     * @param maxLen  max character length for auto-generated summary
     * @return summary if non-blank, else auto-generated fallback, else empty string
     */
    public static String fallbackSummary(String summary, String content, int maxLen) {
        if (isNotBlank(summary)) return summary;
        if (isBlank(content)) return "";
        // 1. markdown images → 【图片】
        String text = content.replaceAll("!\\[.*?\\]\\(.*?\\)", " 【图片】 ");
        // 2. res:// placeholders → 【图片】
        text = text.replaceAll("res://[a-fA-F0-9\\-]+", " 【图片】 ");
        // 3. inline links → keep only text
        text = text.replaceAll("\\[(.*?)\\]\\(.*?\\)", "$1");
        // 4. code fences
        text = text.replaceAll("```[\\s\\S]*?```", " ");
        // 5. inline code
        text = text.replaceAll("`[^`]*`", " ");
        // 6. heading markers
        text = text.replaceAll("^#{1,6}\\s+", "");
        // 7. HTML tags
        text = text.replaceAll("<[^>]+>", "");
        // 8. horizontal rules / blockquote / list markers
        text = text.replaceAll("^[>\\-*_]\\s*", "");
        // 9. collapse whitespace
        text = text.replaceAll("\\s+", " ").trim();
        if (text.length() <= maxLen) return text;
        return text.substring(0, maxLen).trim();
    }

    public static String replaceResPlaceholders(String content, Map<String, String> uuidToUrl) {
        if(isBlank(content)) return content;
        StringBuffer sb = new StringBuffer();
        Matcher matcher = RES_PATTERN.matcher(content);
        while (matcher.find()) {
            String uuid = matcher.group(1);
            String url = uuidToUrl.getOrDefault(uuid, matcher.group(0));
            matcher.appendReplacement(sb, Matcher.quoteReplacement(url));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }
}
