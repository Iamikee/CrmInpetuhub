package com.inpetu.connect.service;

import com.inpetu.connect.dto.HorarioFuncionamentoResponse;
import com.inpetu.connect.model.Espaco;
import com.inpetu.connect.model.HorarioFuncionamento;
import com.inpetu.connect.repository.HorarioFuncionamentoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class HorarioFuncionamentoService {
    private static final LocalTime ABERTURA_PADRAO = LocalTime.of(8, 0);
    private static final LocalTime FECHAMENTO_PADRAO = LocalTime.of(22, 0);
    private static final Map<DayOfWeek, String> NOMES_DIAS = Map.of(
            DayOfWeek.MONDAY, "segundas-feiras",
            DayOfWeek.TUESDAY, "terças-feiras",
            DayOfWeek.WEDNESDAY, "quartas-feiras",
            DayOfWeek.THURSDAY, "quintas-feiras",
            DayOfWeek.FRIDAY, "sextas-feiras",
            DayOfWeek.SATURDAY, "sábados",
            DayOfWeek.SUNDAY, "domingos");

    private final HorarioFuncionamentoRepository horarios;

    public HorarioFuncionamentoService(HorarioFuncionamentoRepository horarios) {
        this.horarios = horarios;
    }

    @Transactional(readOnly = true)
    public List<HorarioFuncionamentoResponse> listar(boolean visitaGuiada) {
        List<HorarioFuncionamento> registros = visitaGuiada
                ? List.of()
                : horarios.findAllByOrderByDiaSemana();
        List<HorarioFuncionamentoResponse> resultado = new ArrayList<>(DayOfWeek.values().length);
        for (DayOfWeek dia : DayOfWeek.values()) {
            if (visitaGuiada) {
                resultado.add(new HorarioFuncionamentoResponse(dia.getValue(), false, false,
                        ABERTURA_PADRAO, FECHAMENTO_PADRAO));
                continue;
            }
            HorarioFuncionamento horario = registros.stream()
                    .filter(item -> item.getDiaSemana() == dia.getValue())
                    .findFirst()
                    .orElse(null);
            resultado.add(horario == null ? padrao(dia) : response(horario, dia));
        }
        return List.copyOf(resultado);
    }

    public boolean validarReserva(Espaco espaco, LocalDate data, LocalTime inicio, LocalTime fim) {
        boolean visitaGuiada = "visita guiada".equalsIgnoreCase(espaco.getTipoEspaco());
        HorarioFuncionamentoResponse horario = listar(visitaGuiada)
                .get(data.getDayOfWeek().getValue() - 1);
        if (horario.fechado()) {
            String preposicao = data.getDayOfWeek() == DayOfWeek.SATURDAY
                    || data.getDayOfWeek() == DayOfWeek.SUNDAY ? "aos " : "às ";
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "O espaço não funciona " + preposicao + NOMES_DIAS.get(data.getDayOfWeek()) + ".");
        }
        if (inicio.isBefore(horario.abertura()) || fim.isAfter(horario.fechamento())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "O horário deve ficar entre " + horario.abertura() + " e " + horario.fechamento() + ".");
        }
        return horario.exigeAprovacao();
    }

    private HorarioFuncionamentoResponse response(HorarioFuncionamento horario, DayOfWeek dia) {
        if (Boolean.TRUE.equals(horario.getFechado())) {
            return new HorarioFuncionamentoResponse(dia.getValue(), true,
                    Boolean.TRUE.equals(horario.getExigeAprovacao()), null, null);
        }
        if (horario.getHorarioAbertura() == null || horario.getHorarioFechamento() == null
                || !horario.getHorarioFechamento().isAfter(horario.getHorarioAbertura())) {
            return padrao(dia);
        }
        return new HorarioFuncionamentoResponse(dia.getValue(), false,
                Boolean.TRUE.equals(horario.getExigeAprovacao()),
                horario.getHorarioAbertura(), horario.getHorarioFechamento());
    }

    private HorarioFuncionamentoResponse padrao(DayOfWeek dia) {
        boolean exigeAprovacao = dia == DayOfWeek.SUNDAY;
        return new HorarioFuncionamentoResponse(dia.getValue(), false, exigeAprovacao,
                ABERTURA_PADRAO, FECHAMENTO_PADRAO);
    }
}
