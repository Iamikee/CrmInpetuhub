package com.inpetu.connect.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = NomeCompletoValidoValidator.class)
public @interface NomeCompletoValido {
    String message() default "Informe nome e sobrenome";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
