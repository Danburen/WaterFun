package org.waterwood.waterfunservicecore.infrastructure.utils.context;


import org.waterwood.common.context.LoginUser;

/**
 * ThreadLocal Util
 *
 * Use Spring Security instance
 */
@Deprecated
public class ThreadLocalUtil {
    private static final ThreadLocal<LoginUser> THREAD_LOCAL = new ThreadLocal<>();
    public static LoginUser get(){
        return THREAD_LOCAL.get();
    }

    /**
     * Set values to ThreadLocal
     */
    public static void set(LoginUser context){
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
}
