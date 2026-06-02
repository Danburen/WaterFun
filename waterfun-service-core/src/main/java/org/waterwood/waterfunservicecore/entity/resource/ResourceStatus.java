package org.waterwood.waterfunservicecore.entity.resource;

import lombok.Getter;

@Getter
public enum ResourceStatus {
    UPLOAD_PENDING(0),
    ACTIVE(1),
    ORPHAN(2),
    DELETED(3),;

    private final short value;
    private ResourceStatus(int value) {
        this.value = (short) value;
    }

    public static ResourceStatus valueOf(short value) {
        for (ResourceStatus status : ResourceStatus.values()) {
            if (status.value == value) {
                return status;
            }
        }
        throw new IllegalArgumentException("Resource status don't have value " + value);
    }
}
