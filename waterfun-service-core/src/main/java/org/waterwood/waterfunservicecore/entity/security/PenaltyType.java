package org.waterwood.waterfunservicecore.entity.security;

import lombok.Getter;
import org.waterwood.waterfunservicecore.entity.BanPermission;

@Getter
public enum PenaltyType {
    UNSPECIFIED(0, null),
    BAN_LOGIN(1, BanPermission.BAN_LOGIN),
    BAN_POST(2, BanPermission.BAN_POST),
    BAN_COMMENT(3, BanPermission.BAN_COMMENT),
    BAN_UPLOAD(4, BanPermission.BAN_UPLOAD),
    BAN_CHAT(5, BanPermission.BAN_CHAT),
    BAN_CREATE(6, BanPermission.BAN_CREATE),
    OTHER(99, null);

    private final short value;
    private final BanPermission banPermission;

    PenaltyType(int value, BanPermission banPermission) {
        this.value = (short) value;
        this.banPermission = banPermission;
    }

    public static PenaltyType fromValue(short value) {
        for (PenaltyType t : values()) {
            if (t.value == value) return t;
        }
        return OTHER;
    }
}
