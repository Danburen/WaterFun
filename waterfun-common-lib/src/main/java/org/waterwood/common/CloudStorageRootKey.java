package org.waterwood.common;


import lombok.Getter;

@Getter
public enum CloudStorageRootKey {
    UPLOADS("uploads"),
    TEMP("temp"), MODERATION("moderation");

    private final String key;
    CloudStorageRootKey(String key) {
        this.key = key;
    }

}
