package org.waterwood.waterfunservicecore.services.auth;

import static org.waterwood.common.KeyConstants.*;

public final class VerifyKeyBuilder {
    private static final String VERIFY_PREFIX = "verify:";

    private VerifyKeyBuilder() {
    }

    public static String captcha(String value){
        return VERIFY_PREFIX + CAPTCHA + ":" + value;
    }

    public static String sms(String value){
        return VERIFY_PREFIX + SMS + ":" + value;
    }

    public static String email(String value){
        return VERIFY_PREFIX + EMAIL + ":" + value;
    }
}
