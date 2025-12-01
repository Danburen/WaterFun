package org.waterwood.common.validation.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.waterwood.common.validation.Username;

import java.util.regex.Pattern;

public class UsernameValidator implements ConstraintValidator<Username,String> {
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]+$");
    @Override
    public void initialize(Username constraintAnnotation) {
        ConstraintValidator.super.initialize(constraintAnnotation);
    }

    @Override
    public boolean isValid(String s, ConstraintValidatorContext constraintValidatorContext) {
        return USERNAME_PATTERN.matcher(s).matches();
    }
}
