package com.inpetu.connect.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;

public final class NomeCompletoValidoValidator implements ConstraintValidator<NomeCompletoValido, String> {
    private static final Set<String> CONECTIVOS = Set.of("de", "da", "do", "dos", "das", "e");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) return true;
        String[] palavras = value.trim().split(" +");
        long palavrasPrincipais = Arrays.stream(palavras)
                .filter(palavra -> !CONECTIVOS.contains(palavra.toLowerCase(Locale.ROOT)))
                .count();
        return palavrasPrincipais >= 2 && Arrays.stream(palavras)
                .allMatch(palavra -> CONECTIVOS.contains(palavra.toLowerCase(Locale.ROOT))
                        || palavra.codePoints().filter(Character::isLetter).count() >= 2);
    }
}
