package org.waterwood.waterfunservicecore.entity.user;

import lombok.Getter;

@Getter
public enum UserType {
    COMMON(0),
    ADMIN(1),
    BOT(2),
    MODERATOR(3),
    VIP(4),;

    private final short value;
    UserType(final int value) {
        this.value = (short) value;
    }

    public static UserType fromValue(Short value) {
        for (UserType userType : UserType.values()) {
            if (userType.value == value) {
                return userType;
            }
        }
        throw new IllegalArgumentException("Can't find User Type of" +value);
    }
}
