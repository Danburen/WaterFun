package org.waterwood.waterfunadminservice.infrastructure.validation;

import jakarta.validation.Constraint;
import org.springframework.messaging.handler.annotation.Payload;
import org.waterwood.waterfunadminservice.infrastructure.validation.validator.PostStateValidator;

import java.lang.annotation.*;

@Documented
@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PostStateValidator.class)
public @interface PostState {
    String message() default "{valid.post.status_not_support}";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
