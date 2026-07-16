package org.waterwood.common.io;

import lombok.Getter;

import java.util.List;

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

    private final byte value;
    ResourceType(final int value) {
        this.value = (byte) value;
    }

    public static ResourceType fromCode(Byte value) {
        for (ResourceType type : ResourceType.values()) {
            if (type.value == value) {
                return type;
            }
        }
        throw new IllegalArgumentException();
    }

    public boolean isAllowed(FileExtension ext) {
        return isAllowed(ext.getExt());
    }

    public boolean isAllowed(String ext) {
        return FileExtension.isAllowed(ext, this);
    }

    public List<String> getAllowExtensionString() {
        return FileExtension.getAllowExtensionString(this);
    }

    public List<FileExtension> getAllowExtensions() {
        return FileExtension.getAllowExtensions(this);
    }

    public List<String> getAllowMimeTypes() {
        return FileExtension.getAllowMimeTypes(this);
    }
}
