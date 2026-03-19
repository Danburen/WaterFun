package org.waterwood.waterfunservicecore.infrastructure.utils.context;


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
    public static Long getUserUid(){
        return get().getUserUid();
    }
    /**
     * Set values to ThreadLocal
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
}
