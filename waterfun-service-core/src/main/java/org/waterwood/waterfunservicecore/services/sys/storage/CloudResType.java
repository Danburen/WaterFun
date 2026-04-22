package org.waterwood.waterfunservicecore.services.sys.storage;

@Deprecated(since = "2026-04", forRemoval = false)
public enum CloudResType {
    AVATAR,
    COVER,;

    public String getLocalCase() {
        return name().toLowerCase();
    }
}
