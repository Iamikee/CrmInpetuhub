package com.inpetu.connect.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = TelefoneBrasileiroValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface TelefoneBrasileiro {
    String message() default "Informe um celular brasileiro válido no formato E.164 (+55).";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
