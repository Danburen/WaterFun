package org.waterwood.waterfunservicecore.services.sys.storage;

public enum CloudResType {
    AVATAR,
    COVER,;

    public String getLocalCase() {
        return name().toLowerCase();
    }
}
