package org.waterwood.waterfunservicecore.infrastructure.utils;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

@Component
public class ResponseUtil {
    public static void setCookieAndNoCache(HttpServletResponse response, String cookieName, String cookieValue, int maxAge) {
        Cookie cookie = new Cookie(cookieName, cookieValue);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(maxAge);
        response.addCookie(cookie);
        response.setContentType("application/json");
        response.setHeader("Pragma", "No-cache");
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setDateHeader("Expires", 0);
    }


    public static String getContentType(String fileType){
        return switch (fileType) {
            case "html", "htm" -> "text/html; charset=UTF-8";
            case "txt" -> "text/plain; charset=UTF-8";
            case "json" -> "application/json; charset=UTF-8";
            case "pdf" -> "application/pdf";
            case "png" -> "image/png";
            case "jpg", "jpeg" -> "image/jpeg";
            case "gif" -> "image/gif";
            case "css" -> "text/css; charset=UTF-8";
            case "js" -> "application/javascript";
            default -> "application/octet-stream";
        };
    }

}
