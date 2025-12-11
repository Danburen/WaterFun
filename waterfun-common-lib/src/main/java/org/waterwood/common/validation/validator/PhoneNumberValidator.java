package org.waterwood.common.validation.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.waterwood.common.validation.PhoneNumber;

import java.util.regex.Pattern;

public class PhoneNumberValidator implements ConstraintValidator<PhoneNumber,String> {
    Pattern phonePattern = Pattern.compile("^1[3-9]\\d{9}$");

    @Override
    public void initialize(PhoneNumber constraintAnnotation) {
        ConstraintValidator.super.initialize(constraintAnnotation);
    }

    @Override
    public boolean isValid(String s, ConstraintValidatorContext constraintValidatorContext) {
        if(s == null || s.isEmpty()){
            return true;
        }
        return phonePattern.matcher(s).matches();
    }
}
