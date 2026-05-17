package org.waterwood.waterfunservice.service.resource;

import java.util.Set;

public class LegalResourceConstants {
    // 文件类型白名单
    public static final Set<String> VALID_TYPES = Set.of("licence", "privacy");

    // 语言白名单
    public static final Set<String> VALID_LANGS = Set.of("zh_CN", "en_US");

    // 允许的MIME类型
    public static final Set<String> ALLOWED_MIME_TYPES = Set.of("text/plain", "text/html");

    // 私有构造方法防止实例化
    private LegalResourceConstants() {}
}
