package com.inpetu.connect.service;

import com.inpetu.connect.dto.HorarioFuncionamentoResponse;
import com.inpetu.connect.model.Espaco;
import com.inpetu.connect.model.HorarioFuncionamento;
import com.inpetu.connect.repository.HorarioFuncionamentoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HorarioFuncionamentoServiceTest {
    @Mock
    HorarioFuncionamentoRepository horarios;

    @InjectMocks
    HorarioFuncionamentoService service;

    @Test
    void usesSafeDefaultForEveryMissingDay() {
        when(horarios.findAllByOrderByDiaSemana()).thenReturn(List.of());

        List<HorarioFuncionamentoResponse> result = service.listar(false);

        assertEquals(7, result.size());
        for (int day = 0; day < 7; day++) {
            assertEquals(false, result.get(day).fechado());
            assertEquals(LocalTime.of(8, 0), result.get(day).abertura());
            assertEquals(LocalTime.of(22, 0), result.get(day).fechamento());
            assertEquals(day == 6, result.get(day).exigeAprovacao());
        }
    }

    @Test
    void appliesTheSingleWeeklyScheduleAndSundayApproval() {
        HorarioFuncionamento monday = horario(1, false, false, "08:00", "22:00");
        HorarioFuncionamento sunday = horario(7, false, true, "08:00", "22:00");
        when(horarios.findAllByOrderByDiaSemana()).thenReturn(List.of(monday, sunday));

        List<HorarioFuncionamentoResponse> result = service.listar(false);

        assertEquals(LocalTime.of(8, 0), result.get(0).abertura());
        assertEquals(LocalTime.of(22, 0), result.get(0).fechamento());
        assertEquals(true, result.get(6).exigeAprovacao());
        assertEquals(false, result.get(5).exigeAprovacao());
    }

    @Test
    void acceptsOpeningAndExactClosingBoundaries() {
        when(horarios.findAllByOrderByDiaSemana()).thenReturn(List.of());
        Espaco space = espaco();

        boolean requiresApproval = service.validarReserva(space, LocalDate.of(2026, 11, 16),
                LocalTime.of(8, 0), LocalTime.of(22, 0));
        assertEquals(false, requiresApproval);
    }

    @Test
    void permitsSundayOnlyWithinHoursAndRequiresApproval() {
        when(horarios.findAllByOrderByDiaSemana()).thenReturn(List.of());
        Espaco space = espaco();

        boolean requiresApproval = service.validarReserva(space, LocalDate.of(2026, 11, 15),
                LocalTime.of(8, 0), LocalTime.of(22, 0));
        assertEquals(true, requiresApproval);

        ResponseStatusException beforeOpening = assertThrows(ResponseStatusException.class,
                () -> service.validarReserva(space, LocalDate.of(2026, 11, 16),
                        LocalTime.of(7, 59), LocalTime.of(8, 30)));
        assertEquals(HttpStatus.BAD_REQUEST, beforeOpening.getStatusCode());
        assertEquals("O horário deve ficar entre 08:00 e 22:00.", beforeOpening.getReason());

        ResponseStatusException afterClosing = assertThrows(ResponseStatusException.class,
                () -> service.validarReserva(space, LocalDate.of(2026, 11, 16),
                        LocalTime.of(21, 30), LocalTime.of(22, 1)));
        assertEquals(HttpStatus.BAD_REQUEST, afterClosing.getStatusCode());
        assertEquals("O horário deve ficar entre 08:00 e 22:00.", afterClosing.getReason());
    }

    @Test
    void keepsGuidedToursSeparateAndOpenBetweenEightAndTwentyTwoEveryDay() {
        Espaco space = espaco();
        space.setTipoEspaco("Visita guiada");

        boolean requiresApproval = service.validarReserva(space, LocalDate.of(2026, 11, 15),
                LocalTime.of(21, 30), LocalTime.of(22, 0));
        assertEquals(false, requiresApproval);

        ResponseStatusException outside = assertThrows(ResponseStatusException.class,
                () -> service.validarReserva(space, LocalDate.of(2026, 11, 15),
                        LocalTime.of(7, 59), LocalTime.of(8, 30)));
        assertEquals("O horário deve ficar entre 08:00 e 22:00.", outside.getReason());
    }

    private HorarioFuncionamento horario(int day, boolean closed, boolean approval, String open, String close) {
        HorarioFuncionamento item = new HorarioFuncionamento();
        item.setDiaSemana(day);
        item.setFechado(closed);
        item.setExigeAprovacao(approval);
        item.setHorarioAbertura(open == null ? null : LocalTime.parse(open));
        item.setHorarioFechamento(close == null ? null : LocalTime.parse(close));
        return item;
    }

    private Espaco espaco() {
        Espaco space = new Espaco();
        space.setIdEspaco(2);
        space.setTipoEspaco("Sala de reunião");
        return space;
    }
}
