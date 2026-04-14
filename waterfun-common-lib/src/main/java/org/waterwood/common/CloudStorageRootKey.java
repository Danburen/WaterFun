package org.waterwood.common;

public enum CloudStorageRootKey {
    UPLOADS("uploads"),
    TEMP("temp");

    private final String key;
    CloudStorageRootKey(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }
}
