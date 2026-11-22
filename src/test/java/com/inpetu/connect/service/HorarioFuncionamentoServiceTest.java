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
            assertEquals(LocalTime.of(7, 0), result.get(day).abertura());
            assertEquals(LocalTime.of(21, 0), result.get(day).fechamento());
            assertEquals(day >= 5, result.get(day).exigeAprovacao());
        }
    }

    @Test
    void appliesTheSingleWeeklyScheduleAndWeekendApproval() {
        HorarioFuncionamento monday = horario(1, false, false, "07:00", "21:00");
        HorarioFuncionamento saturday = horario(6, false, true, "07:00", "21:00");
        HorarioFuncionamento sunday = horario(7, false, true, "07:00", "21:00");
        when(horarios.findAllByOrderByDiaSemana()).thenReturn(List.of(monday, saturday, sunday));

        List<HorarioFuncionamentoResponse> result = service.listar(false);

        assertEquals(LocalTime.of(7, 0), result.get(0).abertura());
        assertEquals(LocalTime.of(21, 0), result.get(0).fechamento());
        assertEquals(true, result.get(5).exigeAprovacao());
        assertEquals(true, result.get(6).exigeAprovacao());
    }

    @Test
    void acceptsOpeningAndExactClosingBoundaries() {
        when(horarios.findAllByOrderByDiaSemana()).thenReturn(List.of());
        Espaco space = espaco();

        boolean requiresApproval = service.validarReserva(space, LocalDate.of(2026, 11, 16),
                LocalTime.of(7, 0), LocalTime.of(21, 0));
        assertEquals(false, requiresApproval);
    }

    @Test
    void permitsSaturdayAndSundayOnlyWithinHoursAndRequiresApproval() {
        when(horarios.findAllByOrderByDiaSemana()).thenReturn(List.of());
        Espaco space = espaco();

        boolean saturdayRequiresApproval = service.validarReserva(space, LocalDate.of(2026, 11, 14),
                LocalTime.of(7, 0), LocalTime.of(21, 0));
        assertEquals(true, saturdayRequiresApproval);

        boolean sundayRequiresApproval = service.validarReserva(space, LocalDate.of(2026, 11, 15),
                LocalTime.of(7, 0), LocalTime.of(21, 0));
        assertEquals(true, sundayRequiresApproval);

        ResponseStatusException beforeOpening = assertThrows(ResponseStatusException.class,
                () -> service.validarReserva(space, LocalDate.of(2026, 11, 16),
                        LocalTime.of(6, 59), LocalTime.of(7, 30)));
        assertEquals(HttpStatus.BAD_REQUEST, beforeOpening.getStatusCode());
        assertEquals("O horário deve ficar entre 07:00 e 21:00.", beforeOpening.getReason());

        ResponseStatusException afterClosing = assertThrows(ResponseStatusException.class,
                () -> service.validarReserva(space, LocalDate.of(2026, 11, 16),
                        LocalTime.of(20, 30), LocalTime.of(21, 1)));
        assertEquals(HttpStatus.BAD_REQUEST, afterClosing.getStatusCode());
        assertEquals("O horário deve ficar entre 07:00 e 21:00.", afterClosing.getReason());
    }

    @Test
    void keepsGuidedToursSeparateAndOpenBetweenSevenAndTwentyOneEveryDay() {
        Espaco space = espaco();
        space.setTipoEspaco("Visita guiada");

        boolean requiresApproval = service.validarReserva(space, LocalDate.of(2026, 11, 15),
                LocalTime.of(20, 30), LocalTime.of(21, 0));
        assertEquals(false, requiresApproval);

        ResponseStatusException outside = assertThrows(ResponseStatusException.class,
                () -> service.validarReserva(space, LocalDate.of(2026, 11, 15),
                        LocalTime.of(6, 59), LocalTime.of(7, 30)));
        assertEquals("O horário deve ficar entre 07:00 e 21:00.", outside.getReason());
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
