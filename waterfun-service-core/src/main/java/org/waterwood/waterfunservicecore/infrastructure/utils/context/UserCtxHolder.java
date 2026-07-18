package org.waterwood.waterfunservicecore.infrastructure.utils.context;


import org.jetbrains.annotations.Nullable;
import org.waterwood.waterfunservicecore.entity.user.UserSetting;

import java.util.Locale;
import java.util.Optional;

/**
 * ThreadLocal Util
 *
 * Use Spring Security instance
 */
public class UserCtxHolder {
    private static final ThreadLocal<AuthContext> THREAD_LOCAL = new ThreadLocal<>();
    public static AuthContext get(){
        return THREAD_LOCAL.get();
    }
    public static Optional<Long> safeGetUserId() {
        return Optional.ofNullable(get()).map(AuthContext::getUserUid);
    }
    public static Optional<AuthContext> safeGet() {
        return Optional.ofNullable(get());
    }

    @Nullable
    public static Long getUserUid(){
        return get() == null ? null : get().getUserUid();
    }
    /**
     * Set value to ThreadLocal
     */
    public static void set(AuthContext context){
        THREAD_LOCAL.set(context);
    }



    /**
     * Get jwt identify.
     * @return the java web token id.
     */
    public static String getJti(){
        return get().getJti();
    }

    /**
     * Remoce ThreadLocal to avoid memory leak.
     */
    public static void remove(){
        THREAD_LOCAL.remove();
    }

    public static Locale getLocale() {
        return get().getLocale();
    }

    public static String getClientIp() {
        return get().getClientIp();
    }

    public static @Nullable UserSetting getUserSetting() {
        AuthContext ctx = get();
        return ctx != null ? ctx.getUserSetting() : null;
    }
}
