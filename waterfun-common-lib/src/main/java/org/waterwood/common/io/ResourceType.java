package org.waterwood.common.io;

import lombok.Getter;

import java.io.File;

@Getter
public enum ResourceType {
    UNKNOWN(0),
    IMAGE(1),
    VIDEO(2),
    AUDIO(3),
    TEXT(4),
    DOCUMENT(5),
    ARCHIVE(6),
    EXECUTABLE(7),
    OTHER(99),;

    private final short value;
    ResourceType(final int value) {
        this.value = (short) value;
    }

    public static ResourceType fromCode(Short value) {
        if(value == null) {
            return UNKNOWN;
        }
        return switch (value) {
            case 1 -> IMAGE;
            case 2 -> VIDEO;
            case 3 -> AUDIO;
            case 4 -> OTHER;
            default -> UNKNOWN;
        };
    }

    public boolean isAllowed(FileExtension ext) {
        return FileExtension.isAllowed(ext.getExt(), this);
    }
}
