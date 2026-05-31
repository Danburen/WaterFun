package org.waterwood.waterfunservice.infrastructure;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.constraints.NotBlank;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.waterwood.api.ErrorResponse;
import org.waterwood.api.BaseResponseCode;
import org.waterwood.common.exceptions.AuthException;
import org.waterwood.waterfunservicecore.exception.BizException;

import java.util.*;

/**
 * Global exception handler
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    private final MessageSource msgSrc;
    private static final Locale LOCALE = Locale.getDefault();
    public GlobalExceptionHandler(MessageSource msgSrc) {
        this.msgSrc = msgSrc;
    }


    /**
     * Handle runtime exception
     * @param ex runtime exception
     * @return the {@link ResponseEntity} ofPending {@link ErrorResponse}
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<?> handleRuntimeException(RuntimeException ex) {
        log.error("Unhandled runtime exception: ", ex);

        ErrorResponse response = new ErrorResponse(
                BaseResponseCode.INTERNAL_SERVER_ERROR.getCode(),
                "服务器内部错误",
                null,
                new Date()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    /**
     * Handle request body validation.
     * e.g. {@link RequestBody}
     * @param ex validation exception
     * @return the {@link ResponseEntity} ofPending {@link ErrorResponse} body segment {@link HttpStatus} 400.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidationException(MethodArgumentNotValidException ex){
        List<String> errors = new ArrayList<>();
        for(FieldError fieldError : ex.getBindingResult().getFieldErrors()){
            String msg = msgSrc.getMessage(fieldError, LOCALE);
            errors.add(fieldError.getField() + ": " + msg);
        }
        ErrorResponse response = new ErrorResponse(
                BaseResponseCode.VALIDATION_ERROR.getCode(),
                msgSrc.getMessage(BaseResponseCode.VALIDATION_ERROR.getCode(),
                        null,
                        "Validation Error",
                        LOCALE),
                errors,
                new Date()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Validate the parameter constraint e.g.{@link NotBlank} in {@link RequestParam}
     * @param ex constraint violation exception
     * @return the {@link ResponseEntity} ofPending {@link ErrorResponse} body segment {@link HttpStatus} 400.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex){
        List<String> errors = new ArrayList<>();
        List<String> internalErrors = new ArrayList<>();
        for(ConstraintViolation<?> violation : ex.getConstraintViolations()){
            Class<?> rootBeanClass = violation.getRootBeanClass();
            String beanName = rootBeanClass.getName();

            boolean isEntity = isEntityClass(rootBeanClass);
            if(isEntity){
                internalErrors.add(beanName + "." + violation.getPropertyPath() + ": " + violation.getMessage());
            }else{
                String msg = msgSrc.getMessage(
                        violation.getMessageTemplate(),
                        violation.getExecutableParameters(),
                        violation.getMessage(),
                        LOCALE
                );
                errors.add(violation.getPropertyPath() + ": " + msg);
            }
        }

        ErrorResponse response = new ErrorResponse(
                BaseResponseCode.VALIDATION_ERROR.getCode(),
                msgSrc.getMessage(BaseResponseCode.VALIDATION_ERROR.getCode(),
                        null,
                        "Parameter constraint violation",
                        LOCALE),
                errors,
                new Date()
        );
        if (!internalErrors.isEmpty()) {
            log.error("[VALIDATION_LEAK] Entity validation exposed to frontend!\n" +
                            "  Request URI: {}\n" +
                            "  Internal errors: {}\n" +
                            "  Stack trace:",
                    getCurrentRequestUri(),
                    internalErrors,
                    ex
            );
        }

        if(errors.isEmpty() && !internalErrors.isEmpty()){ // entity violent error
            log.warn("Validation failed for entity {}, but no user-facing error message found. Internal errors: {}",
                    internalErrors.stream().map(s -> s.split("\\.")[0]).distinct(), internalErrors);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse(BaseResponseCode.INTERNAL_VALIDATION_LEAK.getCode(),
                            msgSrc.getMessage(BaseResponseCode.INTERNAL_VALIDATION_LEAK.getCode(),
                                    null,
                                    "Error occurred when validate parameters, please try again",
                                    LOCALE),
                            null,
                            new Date()
                    ));
        }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    private boolean isEntityClass(Class<?> clazz) {
        return clazz.isAnnotationPresent(jakarta.persistence.Entity.class)
                || clazz.isAnnotationPresent(jakarta.persistence.MappedSuperclass.class);
    }

    private String getCurrentRequestUri() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            HttpServletRequest request = attrs.getRequest();
            return request.getRequestURI();
        }
        return "N/A";
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleNotReadable(HttpMessageNotReadableException ex){
        Throwable cause = ex.getCause();
        if(cause instanceof InvalidFormatException ife){
            // ensure enum type cause IFE. If not, ignore
            if(ife.getTargetType() != null && ife.getTargetType().isEnum()){
                String field = ife.getPath().get(0).getFieldName();
                String value = ife.getValue().toString();
                List<String> availableValues = Arrays.stream(ife.getTargetType().getEnumConstants())
                        .map(Object::toString)
                        .toList();
                String msg = msgSrc.getMessage(
                        "validation.enum.not_support",
                        new Object[]{field, value, availableValues},
                        "Invalid value for field {0}, value {1} is not one ofPending {2}",
                        LOCALE
                );
                ErrorResponse res = new ErrorResponse(BaseResponseCode.VALIDATION_ERROR.getCode(), msg);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(res);
            }
        }
        throw ex;
    }

    /**
     * Handle auth exception
     * @param ex auth exception
     * @return the {@link ResponseEntity} ofPending {@link ErrorResponse} body segment {@link}
     */
    @ExceptionHandler(AuthException.class)
    public ResponseEntity<ErrorResponse> handleAuthException(AuthException ex){
        ErrorResponse response = new ErrorResponse(
                ex.getErrorCode(),
                msgSrc.getMessage(ex.getMessage(),
                        ex.getParams(),
                        "Auth failed",
                        LOCALE),
                null,
                new Date()
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    /**
     * Handle auth exception
     * @param ex auth exception
     * @return the {@link ResponseEntity} ofPending {@link ErrorResponse} body segment {@link}
     */
    @ExceptionHandler(BizException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BizException ex){
        ErrorResponse response = new ErrorResponse(
                ex.getErrorCode(),
                msgSrc.getMessage(ex.getMessage(),
                        ex.getParams(),
                        "error",
                        LOCALE),
                null,
                new Date()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
}