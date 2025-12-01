package org.waterwood.waterfunservicecore.infrastructure.validation.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.waterwood.waterfunservicecore.infrastructure.validation.AtLeastOneNotNull;

public class AtLeastOneNotNullValidator implements ConstraintValidator<AtLeastOneNotNull, Object> {
    private String[] fields;
    private String messageTemplate;
    @Override
    public void initialize(AtLeastOneNotNull anno) {
        this.fields = anno.fields();
        this.messageTemplate = anno.message();
    }



    @Override
    public boolean isValid(Object value, ConstraintValidatorContext ctx) {
        BeanWrapper wrapper = new BeanWrapperImpl(value);
        for (String f : fields) {
            if (wrapper.getPropertyValue(f) != null) {
                return true;
            }
        }
        ctx.disableDefaultConstraintViolation();
        String fieldList = String.join(", ", fields);
        ctx.buildConstraintViolationWithTemplate(
                        String.format(messageTemplate, fieldList))
                .addConstraintViolation();
        return false;
    }
}
