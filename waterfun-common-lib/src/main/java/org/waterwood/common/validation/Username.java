package org.waterwood.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import org.waterwood.common.validation.validator.UsernameValidator;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = UsernameValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface Username {
    String message() default "{user.username.pattern}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
