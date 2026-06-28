package org.waterwood.utils;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class StringUtil {
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
