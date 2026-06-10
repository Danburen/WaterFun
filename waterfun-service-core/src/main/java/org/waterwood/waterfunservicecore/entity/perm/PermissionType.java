package org.waterwood.waterfunservicecore.entity.perm;

import lombok.Getter;

@Getter
public enum PermissionType {
    API(0),
    MENU(1),
    BUTTON(2),
    DATA(3),
    BAM(4),;

    private final short value;
    PermissionType(final int value) {
        this.value = (short) value;
    }

    public static PermissionType fromValue(final int value) {
        for (PermissionType type : PermissionType.values()) {
            if (type.value == value) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown permission type: " + value);
    }
}
