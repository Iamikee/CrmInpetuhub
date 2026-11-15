package com.inpetu.connect.dto;

import java.time.LocalTime;

public record HorarioFuncionamentoResponse(
        int diaSemana,
        boolean fechado,
        boolean exigeAprovacao,
        LocalTime abertura,
        LocalTime fechamento) {
}
