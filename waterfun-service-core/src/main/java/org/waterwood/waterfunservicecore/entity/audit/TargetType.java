package org.waterwood.waterfunservicecore.entity.audit;

import lombok.Getter;
import org.waterwood.common.io.FileExtension;

import java.util.Locale;
import java.util.Set;

@Getter
public enum TargetType {
    UNKNOWN(0, Set.of()),
    USER_AVATAR(1,
            Set.of(FileExtension.JPG,
                    FileExtension.JPEG,
                    FileExtension.PNG,
                    FileExtension.WEBP)
    ),
    POST(2, Set.of()),
    POST_COVERAGE_IMAGE(3,
            Set.of(FileExtension.JPG,
                    FileExtension.JPEG,
                    FileExtension.PNG,
                    FileExtension.WEBP
            )),
    POST_CONTENT_IMAGE(4,
            Set.of(FileExtension.JPG,
                    FileExtension.JPEG,
                    FileExtension.PNG,
                    FileExtension.WEBP
            )
    ),
    POST_CONTENT(5, Set.of(FileExtension.TXT, FileExtension.MD)),;

    private final short code;
    private final Set<FileExtension> allowedExts;

    TargetType(final int code, Set<FileExtension> allowedExts) {
        this.code = (short) code;
        this.allowedExts = allowedExts;
    }

    public static TargetType fromCode(int code) {
        return switch (code) {
            case 1 -> USER_AVATAR;
            case 2 -> POST_COVERAGE_IMAGE;
            default -> UNKNOWN;
        };
    }

    public String toLowerCase() {
        return name().toLowerCase(Locale.ROOT);
    }

    public boolean isAllowed(FileExtension ext) {
        return allowedExts.contains(ext);
    }

    public boolean isAllowed(String ext) {
        FileExtension fe = FileExtension.fromExt(ext);
        return fe != FileExtension.UNKNOWN && allowedExts.contains(fe);
    }
}
