package org.waterwood.waterfunservicecore.infrastructure.aspect;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {
    int permits() default 10;
    String key() default "user";
    /**
     * Windows in seconds
     */
    int window() default 60;
    String message() default "Request limit exceeded";
}
