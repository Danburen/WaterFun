package org.waterwood.waterfunservicecore.utils;

import jakarta.servlet.http.Cookie;
import org.waterwood.waterfunservicecore.api.auth.VerifyChannel;
import org.waterwood.waterfunservicecore.infrastructure.utils.CookieUtil;

public class CookieKeyGetter {
    public static String getChannelVerifyCodeKey(VerifyChannel channel, Cookie[] cookies){
        return CookieUtil.getCookieValue(cookies, channel.name().toUpperCase() + "_CODE_KEY");
    }
}
