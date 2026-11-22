package com.inpetu.connect.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentoUtilsTest {
    @Test
    void aceitaCpfValidoComOuSemMascara() {
        assertTrue(DocumentoUtils.cpfValido("529.982.247-25"));
        assertTrue(DocumentoUtils.cpfValido("52998224725"));
    }

    @Test
    void cpfSemMascaraValido() {
        assertTrue(DocumentoUtils.cpfValido("52998224725"));
    }

    @Test
    void rejeitaCpfComDvErrado() {
        assertFalse(DocumentoUtils.cpfValido("529.982.247-24"));
    }

    @Test
    void rejeitaSequenciaCpfRepetida() {
        assertFalse(DocumentoUtils.cpfValido("111.111.111-11"));
    }

    @Test
    void cpfSequenciaRepetidaRecusado() {
        assertFalse(DocumentoUtils.cpfValido("11111111111"));
    }

    @Test
    void aceitaCnpjNumericoValidoComOuSemMascara() {
        assertTrue(DocumentoUtils.cnpjValido("11.222.333/0001-81"));
        assertTrue(DocumentoUtils.cnpjValido("11222333000181"));
    }

    @Test
    void rejeitaCnpjComDvErrado() {
        assertFalse(DocumentoUtils.cnpjValido("11.222.333/0001-80"));
    }

    @Test
    void rejeitaSequenciaCnpjRepetida() {
        assertFalse(DocumentoUtils.cnpjValido("11.111.111/1111-11"));
    }

    @Test
    void cnpjAlfanumericoRecusado() {
        assertFalse(DocumentoUtils.cnpjValido("12ABC34501DE35"));
    }
}
