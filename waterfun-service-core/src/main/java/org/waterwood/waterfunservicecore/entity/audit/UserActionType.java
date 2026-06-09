package org.waterwood.waterfunservicecore.entity.audit;

import lombok.Getter;

@Getter
public enum UserActionType {
    UNKNOWN(0),
    CREATE(1),
    DELETED(2),
    UPDATED(3),
    INTERACTIVE(4),
    REPORT(5),;

    private final short value;
    UserActionType(final int value) {
        this.value = (short) value;
    }

    public static UserActionType fromValue(final int value) {
        for(final UserActionType type : UserActionType.values()) {
            if(type.value == value) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown user action type: " + value);
    }
}
