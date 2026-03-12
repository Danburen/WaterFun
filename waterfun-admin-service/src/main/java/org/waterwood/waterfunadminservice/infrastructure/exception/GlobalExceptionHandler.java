package org.waterwood.waterfunadminservice.infrastructure.exception;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
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
import org.waterwood.api.ErrorResponse;
import org.waterwood.api.BaseResponseCode;
import org.waterwood.common.exceptions.AuthException;
import org.waterwood.common.exceptions.BizException;

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
     * @return the {@link ResponseEntity} of {@link ErrorResponse}
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
     * @return the {@link ResponseEntity} of {@link ErrorResponse} body segment {@link HttpStatus} 400.
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
        log.info(String.valueOf(errors));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Validate the parameter constraint e.g.{@link NotBlank} in {@link RequestParam}
     * @param ex constraint violation exception
     * @return the {@link ResponseEntity} of {@link ErrorResponse} body segment {@link HttpStatus} 400.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<?> handleConstraintViolation(ConstraintViolationException ex){
        List<String> errors = new ArrayList<>();
        for(ConstraintViolation<?> violation : ex.getConstraintViolations()){
            String msg = msgSrc.getMessage(
                    violation.getMessageTemplate(),
                    violation.getExecutableParameters(),
                    violation.getMessage(),
                    LOCALE
            );
            errors.add(violation.getPropertyPath() + ": " + msg);
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
        log.info(String.valueOf(errors));

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
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
                        "Invalid values for field {0}, values {1} is not one of {2}",
                        LOCALE
                );
                ErrorResponse res = new ErrorResponse(BaseResponseCode.VALIDATION_ERROR.getCode(), msg);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(res);
            }
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                new ErrorResponse(
                        BaseResponseCode.VALIDATION_ERROR.getCode(),
                        ex.getMessage(),
                        null,
                        new Date()
                )
        );
    }

    /**
     * Handle auth exception
     * @param ex auth exception
     * @return the {@link ResponseEntity} of {@link ErrorResponse} body segment {@link}
     */
//    @ExceptionHandler(AuthException.class)
    public ResponseEntity<?> handleAuthException(AuthException ex){
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
     * @return the {@link ResponseEntity} of {@link ErrorResponse} body segment {@link}
     */
//    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<?> handleBusinessException(BizException ex){
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