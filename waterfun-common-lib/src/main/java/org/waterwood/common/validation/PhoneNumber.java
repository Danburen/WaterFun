package org.waterwood.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import org.waterwood.common.validation.validator.PhoneNumberValidator;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = PhoneNumberValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface PhoneNumber {
    String message() default "{verification.phone.invalid}";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
