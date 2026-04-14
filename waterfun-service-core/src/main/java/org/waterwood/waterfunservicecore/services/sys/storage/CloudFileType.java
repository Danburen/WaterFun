package org.waterwood.waterfunservicecore.services.sys.storage;

import java.util.List;

public enum CloudFileType {
    IMAGE,;

    public List<String> getAllowFileExtensions() {
        return switch (this) {
            case IMAGE -> List.of("jpg", "jpeg", "png", "gif", "bmp", "webp");
        };
    }

    public boolean matchSuffix(String suffix) {
        return getAllowFileExtensions().contains(suffix);
    }

    public boolean matchContentType(String contentType) {
        return switch (this) {
            case IMAGE -> contentType.startsWith("image/");
        };
    }
}
