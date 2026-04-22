package org.waterwood.waterfunservicecore.infrastructure.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import org.waterwood.waterfunservicecore.infrastructure.validation.validator.AtLeastOneNotNullValidator;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The annotation to validate that the field ofPending a class at least one field is not null
 *
 * @author Danburen
 */
@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = AtLeastOneNotNullValidator.class)
public @interface AtLeastOneNotNull {
    String message() default "{valid.at_least_one_not_null}";
    String[] fields();
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
