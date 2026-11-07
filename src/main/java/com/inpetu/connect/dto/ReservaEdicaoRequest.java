package com.inpetu.connect.dto;

import com.inpetu.connect.util.TelefoneUtils;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
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
        @Size(max = 16) @Pattern(regexp = TelefoneUtils.E164_PATTERN,
                message = "Informe DDI, DDD e número válidos no formato internacional.") String telefoneContato) {
    public ReservaEdicaoRequest(String tituloEvento, String descricaoFinalidade, LocalDate dataReserva,
            LocalTime horarioInicio, LocalTime horarioFim, Integer idEspaco,
            Integer quantidadeParticipantes) {
        this(tituloEvento, descricaoFinalidade, dataReserva, horarioInicio, horarioFim,
                idEspaco, quantidadeParticipantes, null);
    }
}
