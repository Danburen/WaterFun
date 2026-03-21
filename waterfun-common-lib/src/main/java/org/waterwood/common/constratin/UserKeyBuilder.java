package org.waterwood.common.constratin;

import static org.waterwood.common.KeyConstants.*;

public final class UserKeyBuilder {
    private static final String USER_PREFIX = "user:";

    private UserKeyBuilder() {

    }

    public static String user(long userUid) {
        return USER_PREFIX + userUid;
    }

    public static String userAccessDevice(long userUid, String deviceId) {
        return USER_PREFIX + userUid + ":device:" + deviceId + ":access";
    }

    public static String userRole(long userUid) {
        return USER_PREFIX + userUid + ":" + ROLE;
    }

    public static String userPerm(long userUid) {
        return USER_PREFIX + userUid + ":" + PERM;
    }

    public static String userDevices(long userUid) {
        return USER_PREFIX + userUid + ":" + DEVICES;
    }

    public static String userJti(long userUid, String value) {
        return USER_PREFIX + userUid + ":" + JTI + ":" + value;
    }

    public static String userRefresh(long userUid, String deviceId, String family) {
        return USER_PREFIX + userUid + "device:" + deviceId + ":ref:" + family;
    }

}
