package org.waterwood.waterfunservicecore.entity.security;

import lombok.Getter;
import org.waterwood.waterfunservicecore.entity.BanPermission;

@Getter
public enum PenaltyType {
    UNSPECIFIED(0),
    BAN_LOGIN(1),
    BAN_POST(2),
    BAN_COMMENT(3),
    BAN_UPLOAD(4),
    BAN_CHAT(5),
    BAN_CREATE(6),
    OTHER(99);

    private final short value;

    PenaltyType(int value) {
        this.value = (short) value;
    }

    public BanPermission getBanPermission() {
        try {
            return BanPermission.valueOf(this.name());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static PenaltyType fromValue(short value) {
        for (PenaltyType t : values()) {
            if (t.value == value) return t;
        }
        return OTHER;
    }
}
