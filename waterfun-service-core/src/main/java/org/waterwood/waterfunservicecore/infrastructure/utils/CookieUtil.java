package org.waterwood.waterfunservicecore.infrastructure.utils;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jetbrains.annotations.Nullable;
import org.springframework.http.ResponseCookie;
import org.waterwood.api.TokenPair;

import java.util.Arrays;

public final class CookieUtil {
    private static final String COOKIE_SAME_SITE_CONFIG = "Strict";
    private static final boolean COOKIE_SECURE = false;
    public static void setTokenCookie(HttpServletResponse response, TokenPair tokenPair) {
        //setAccessTokenCookie(response, tokenPair.tokenValue(), tokenPair.accessExp()); // Cookie AccessToken
        setRefreshTokenCookie(response, tokenPair.refreshToken(), (long) (7 * 24 * 60 * 60));
    }

    public static void setAccessTokenCookie(HttpServletResponse response, String accessToken, Long expireIn) {
        ResponseCookie accessCookie = ResponseCookie.from("ACCESS_TOKEN",accessToken)
                .httpOnly(true)
                .secure(COOKIE_SECURE) // only https
                .sameSite(COOKIE_SAME_SITE_CONFIG)
                .maxAge(expireIn)
                .path("/")
                .build();
        response.addHeader("Set-Cookie",accessCookie.toString());
    }

    public static void setRefreshTokenCookie(HttpServletResponse response, String refreshToken, Long expireIn) {
        ResponseCookie refreshCookie = ResponseCookie.from("REFRESH_TOKEN",refreshToken)
                .httpOnly(true)
                .secure(COOKIE_SECURE)
                .sameSite(COOKIE_SAME_SITE_CONFIG)
                .maxAge(expireIn)  // same with jwt refresh tokenValue
                .path("/api/auth")
                .build();
        response.addHeader("Set-Cookie", refreshCookie.toString());
    }

    public static void cleanTokenCookie(HttpServletResponse response) {
//        ResponseCookie accessCookie = ResponseCookie.from("ACCESS_TOKEN", "")
//                .httpOnly(true)
//                .secure(COOKIE_SECURE)
//                .sameSite(COOKIE_SAME_SITE_CONFIG)
//                .maxAge(0)  // Instance expired
//                .path("/")
//                .build();
//        response.addHeader("Set-Cookie", accessCookie.toString());
        ResponseCookie refreshCookie = ResponseCookie.from("REFRESH_TOKEN", "")
                .httpOnly(true)
                .secure(COOKIE_SECURE)
                .sameSite(COOKIE_SAME_SITE_CONFIG)
                .maxAge(0)  // Instance expired
                .path("/api/auth")
                .build();

        response.addHeader("Set-Cookie", refreshCookie.toString());
    }

    /**
     * Get target cookie values by key from a cookies array.
     * @param cookies cookies array
     * @param key target cookie key
     * @return the values of the target cookie, or null if not found
     */
    public @Nullable static String getCookieValue(Cookie[] cookies, String key) {
        if(cookies == null) return null;
        return Arrays.stream(cookies)
                .filter(c->key.equals(c.getName()))
                .findFirst()
                .map(Cookie::getValue)
                .orElse(null);
    }

    public @Nullable static String getCookieValue(HttpServletRequest request, String key){
        return request.getCookies() == null ? null : getCookieValue(request.getCookies(), key);
    }
}
