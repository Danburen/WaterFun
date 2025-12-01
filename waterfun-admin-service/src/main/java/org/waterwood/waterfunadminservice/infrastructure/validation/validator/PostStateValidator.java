package org.waterwood.waterfunadminservice.infrastructure.validation.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.waterwood.waterfunadminservice.infrastructure.validation.PostState;

public class PostStateValidator implements ConstraintValidator<PostState, String> {
    @Override
    public void initialize(PostState constraintAnnotation) {
        ConstraintValidator.super.initialize(constraintAnnotation);
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if(value.equals("DRAFT") || value.equals("PUBLISHED") || value.equals("PENDING")
        || value.equals("HIDDEN") || value.equals("DELETED")){
            return true;
        }
        return false;
    }
}
