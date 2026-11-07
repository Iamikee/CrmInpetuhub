package com.inpetu.connect.dto;

import com.inpetu.connect.validation.TelefoneBrasileiro;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.time.LocalTime;

public record ReservaRequest(
    @NotBlank @Size(max = 500) String tituloEvento,
    @NotBlank String descricaoFinalidade,
    @NotNull @FutureOrPresent LocalDate dataReserva,
    @NotNull LocalTime horarioInicio,
    @NotNull LocalTime horarioFim,
    @NotNull Integer idEspaco,
    @Min(1) Integer quantidadeParticipantes,
    @TelefoneBrasileiro String telefoneContato
) {
    public ReservaRequest(String tituloEvento, String descricaoFinalidade, LocalDate dataReserva,
            LocalTime horarioInicio, LocalTime horarioFim, Integer idEspaco,
            Integer quantidadeParticipantes) {
        this(tituloEvento, descricaoFinalidade, dataReserva, horarioInicio, horarioFim,
                idEspaco, quantidadeParticipantes, null);
    }
}
