package com.inpetu.connect.dto;

import com.inpetu.connect.validation.TelefoneBrasileiro;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;

public record ReservaEdicaoRequest(
        @NotBlank @Size(max = 500) String tituloEvento,
        @NotBlank String descricaoFinalidade,
        @NotNull LocalDate dataReserva,
        @NotNull LocalTime horarioInicio,
        @NotNull LocalTime horarioFim,
        @NotNull Integer idEspaco,
        Integer quantidadeParticipantes,
        @TelefoneBrasileiro String telefoneContato) {
    public ReservaEdicaoRequest(String tituloEvento, String descricaoFinalidade, LocalDate dataReserva,
            LocalTime horarioInicio, LocalTime horarioFim, Integer idEspaco,
            Integer quantidadeParticipantes) {
        this(tituloEvento, descricaoFinalidade, dataReserva, horarioInicio, horarioFim,
                idEspaco, quantidadeParticipantes, null);
    }
}
