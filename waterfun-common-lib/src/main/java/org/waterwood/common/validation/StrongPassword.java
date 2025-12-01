package org.waterwood.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import org.waterwood.common.validation.validator.StrongPasswordValidator;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = {StrongPasswordValidator.class})
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface StrongPassword {
    String message() default "{user.password.pattern}";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
