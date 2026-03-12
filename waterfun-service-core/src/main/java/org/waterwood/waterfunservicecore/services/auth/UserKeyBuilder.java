package org.waterwood.waterfunservicecore.services.auth;

import static org.waterwood.common.KeyConstants.*;

public final class UserKeyBuilder {
    private static final String USER_PREFIX = "user:";

    private UserKeyBuilder() {

    }

    public static String user(long userUid) {
        return USER_PREFIX + userUid;
    }

    public static String userDevice(long userUid, String device) {
        return USER_PREFIX + userUid + ":" + DEVICE + ":" + device;
    }

    public static String userRole(long userUid) {
        return USER_PREFIX + userUid + ":" + ROLE;
    }

    public static String userPerm(long userUid) {
        return USER_PREFIX + userUid + ":" + PERM;
    }

    public static String userDevice(long userUid) {
        return USER_PREFIX + userUid + ":" + DEVICE;
    }

    public static String userJti(long userUid, String value) {
        return USER_PREFIX + userUid + ":" + JTI + ":" + value;
    }

    public static String userRefresh(long userUid, String value) {
        return USER_PREFIX + userUid + ":" + REFRESH + ":" + value;
    }
}
