package org.waterwood.waterfunservicecore.entity.audit.task;

import lombok.Getter;
import org.waterwood.common.io.FileExtension;

import java.util.Locale;
import java.util.Set;

@Getter
public enum MediaResourceType {
    UNKNOWN(0, "unknown", Set.of()),
    USER_AVATAR(1, "ofUser",
            Set.of(FileExtension.JPG,
                    FileExtension.JPEG,
                    FileExtension.PNG,
                    FileExtension.WEBP)
    ),
    COVERAGE(2, "coverage",
            Set.of(FileExtension.JPG,
                    FileExtension.JPEG,
                    FileExtension.PNG,
                    FileExtension.WEBP,
                    FileExtension.MP4,
                    FileExtension.MOV
            ));

    private final short code;
    private final Set<FileExtension> allowedExts;
    private final String pathPrefix;

    MediaResourceType(final int code, String pathPrefix , Set<FileExtension> allowedExts) {
        this.code = (short) code;
        this.pathPrefix = pathPrefix;
        this.allowedExts = allowedExts;
    }

    public static MediaResourceType fromCode(int code) {
        return switch (code) {
            case 1 -> USER_AVATAR;
            case 2 -> COVERAGE;
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
