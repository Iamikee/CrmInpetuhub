package com.inpetu.connect.validation;

import com.inpetu.connect.util.TelefoneUtils;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public final class TelefoneBrasileiroValidator implements ConstraintValidator<TelefoneBrasileiro, String> {
    @Override
    public boolean isValid(String telefone, ConstraintValidatorContext context) {
        return TelefoneUtils.e164Valido(telefone);
    }
}
