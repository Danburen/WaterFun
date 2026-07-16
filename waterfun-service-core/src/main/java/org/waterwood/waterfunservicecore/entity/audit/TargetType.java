package org.waterwood.waterfunservicecore.entity.audit;

import lombok.Getter;
import org.waterwood.common.io.FileExtension;
import org.waterwood.common.io.ResourceType;

import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

@Getter
public enum TargetType {
    DEFAULT(0, Set.of()),
    USER_AVATAR(1,ResourceType.IMAGE.getAllowExtensions()),
    POST(2, Set.of()),
    POST_COVERAGE_IMAGE(3,ResourceType.IMAGE.getAllowExtensions()),
    POST_CONTENT_IMAGE(4, ResourceType.IMAGE.getAllowExtensions()),
    BANNER_IMAGE(5, ResourceType.IMAGE.getAllowExtensions()),
    COMMENT(6, Set.of()),
    USER_REPORT_ATTACHMENT(7, ResourceType.IMAGE.getAllowExtensions()),
    MODERATION_IMAGE(8, ResourceType.IMAGE.getAllowExtensions()),
    USER(9, Set.of()),;

    private final byte code;
    private final Set<FileExtension> allowedExts;

    TargetType(final int code, Collection<FileExtension> allowedExts) {
        this.code = (byte) code;
        this.allowedExts = new HashSet<>(allowedExts);
    }

    public static TargetType fromCode(byte code) {
        for(int i = 0; i < values().length; i++) {
            if(values()[i].code == code) {
                return values()[i];
            }
        }
        return DEFAULT;
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
