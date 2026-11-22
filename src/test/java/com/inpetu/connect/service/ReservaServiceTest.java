package com.inpetu.connect.service;

import com.inpetu.connect.dto.ReservaRequest;
import com.inpetu.connect.dto.ReservaEdicaoRequest;
import com.inpetu.connect.model.LogAuditoria;
import com.inpetu.connect.model.Espaco;
import com.inpetu.connect.model.Reserva;
import com.inpetu.connect.model.Usuario;
import com.inpetu.connect.repository.*;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.YearMonth;
import java.time.DayOfWeek;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservaServiceTest {
    @Mock
    ReservaRepository reservas;
    @Mock
    EspacoRepository espacos;
    @Mock
    HorarioFuncionamentoService horarios;
    @Mock
    UsuarioRepository usuarios;
    @Mock
    CancelamentoReservaRepository cancelamentos;
    @Mock
    LogAuditoriaRepository auditoria;
    @Mock
    NotificacaoService notificacoes;
    @InjectMocks
    ReservaService service;

    @Test
    void rejeitaEdicaoComInicioNoPassadoNoFusoDeSaoPaulo() {
        LocalDate ontemEmSaoPaulo = LocalDate.now(ZoneId.of("America/Sao_Paulo")).minusDays(1);
        ReservaEdicaoRequest request = new ReservaEdicaoRequest("Reunião", "Teste", ontemEmSaoPaulo,
                LocalTime.of(10, 0), LocalTime.of(11, 0), 2, null);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.editar(14, request, "equipe@inpetu.ufsc.br"));

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        assertEquals("Não é possível editar a reserva para uma data ou horário que já passou.", error.getReason());
        verifyNoInteractions(reservas, espacos, usuarios, cancelamentos, auditoria, notificacoes);
    }

    @Test
    void rejeitaCriacaoComInicioNoPassadoNoFusoDeSaoPaulo() {
        LocalDate ontemEmSaoPaulo = LocalDate.now(ZoneId.of("America/Sao_Paulo")).minusDays(1);
        ReservaRequest request = new ReservaRequest("Reunião", "Teste", ontemEmSaoPaulo,
                LocalTime.of(10, 0), LocalTime.of(11, 0), 2, null);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.criar(request, "visitante@exemplo.com"));

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        assertEquals("A data e o horário de início não podem estar no passado.", error.getReason());
        verifyNoInteractions(reservas, espacos, usuarios, cancelamentos, auditoria, notificacoes);
    }

    @Test
    void rejeitaCriacaoQuandoHorarioSemanalNaoPermiteReserva() {
        Espaco espaco = new Espaco();
        espaco.setIdEspaco(2);
        espaco.setAtivo(true);
        when(espacos.bloquearParaReserva(2)).thenReturn(Optional.of(espaco));
        doThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "O espaço não funciona aos domingos."))
                .when(horarios).validarReserva(eq(espaco), any(LocalDate.class), any(LocalTime.class), any(LocalTime.class));
        LocalDate sunday = LocalDate.now(ZoneId.of("America/Sao_Paulo"))
                .with(TemporalAdjusters.next(DayOfWeek.SUNDAY));
        ReservaRequest request = new ReservaRequest("Reunião", "Teste", sunday,
                LocalTime.of(9, 0), LocalTime.of(10, 0), 2, null);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.criar(request, "visitante@exemplo.com"));

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        assertEquals("O espaço não funciona aos domingos.", error.getReason());
        verify(reservas, never()).save(any());
    }

    @Test
    void rejeitaEdicaoQuandoHorarioSemanalNaoPermiteReserva() {
        Espaco espaco = new Espaco();
        espaco.setIdEspaco(2);
        espaco.setAtivo(true);
        when(reservas.findById(14)).thenReturn(Optional.of(new Reserva()));
        Usuario responsavel = new Usuario();
        responsavel.setAtivo(true);
        when(usuarios.findByEmail("equipe@inpetu.ufsc.br")).thenReturn(Optional.of(responsavel));
        when(espacos.bloquearParaReserva(2)).thenReturn(Optional.of(espaco));
        doThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "O horário deve ficar entre 07:00 e 21:00."))
                .when(horarios).validarReserva(eq(espaco), any(LocalDate.class), any(LocalTime.class), any(LocalTime.class));
        LocalDate monday = LocalDate.now(ZoneId.of("America/Sao_Paulo"))
                .with(TemporalAdjusters.next(DayOfWeek.MONDAY));
        ReservaEdicaoRequest request = new ReservaEdicaoRequest("Reunião", "Teste", monday,
                LocalTime.of(7, 59), LocalTime.of(8, 30), 2, null);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.editar(14, request, "equipe@inpetu.ufsc.br"));

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        assertEquals("O horário deve ficar entre 07:00 e 21:00.", error.getReason());
        verify(reservas, never()).save(any());
    }

    @Test
    void rejeitaReservaComSobreposicaoComConflito409() {
        Espaco espaco = new Espaco();
        espaco.setIdEspaco(2);
        espaco.setAtivo(true);
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(7);
        usuario.setAtivo(true);
        when(espacos.bloquearParaReserva(2)).thenReturn(Optional.of(espaco));
        LocalDate sunday = LocalDate.now(ZoneId.of("America/Sao_Paulo"))
                .with(TemporalAdjusters.next(DayOfWeek.SUNDAY));
        when(horarios.validarReserva(eq(espaco), eq(sunday), any(LocalTime.class), any(LocalTime.class)))
                .thenReturn(true);
        when(usuarios.findByEmail("visitante@exemplo.com")).thenReturn(Optional.of(usuario));
        when(usuarios.bloquearParaCota(7)).thenReturn(Optional.of(usuario));
        when(reservas.existeConflito(eq(2), any(LocalDate.class), any(LocalTime.class), any(LocalTime.class)))
                .thenReturn(true);
        ReservaRequest request = new ReservaRequest("Oficina", "Encontro de teste", sunday,
                LocalTime.of(9, 0), LocalTime.of(10, 0), 2, null);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.criar(request, "visitante@exemplo.com"));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        verify(reservas, never()).save(any());
    }

    @Test
    void informaMotivoQuandoEspacoEstaIndisponivel() {
        Espaco espaco = new Espaco();
        espaco.setIdEspaco(2);
        espaco.setAtivo(false);
        espaco.setMotivoIndisponibilidade("Manutenção do projetor");
        when(espacos.bloquearParaReserva(2)).thenReturn(Optional.of(espaco));
        ReservaRequest request = new ReservaRequest("Oficina", "Encontro de teste", LocalDate.now().plusDays(1),
                LocalTime.of(9, 0), LocalTime.of(10, 0), 2, null);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.criar(request, "visitante@exemplo.com"));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        assertEquals("Este espaço está indisponível: Manutenção do projetor", error.getReason());
        verifyNoInteractions(usuarios);
        verify(reservas, never()).save(any());
    }

    @Test
    void exigeMotivoAntesDeCancelar() {
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.cancelar(14, "  ", "equipe@inpetu.ufsc.br"));
        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        verifyNoInteractions(usuarios, reservas, cancelamentos, auditoria);
    }

    @Test
    void novaSolicitacaoDeReservaNotificaSomenteEquipe() {
        Espaco espaco = new Espaco();
        espaco.setIdEspaco(2);
        espaco.setAtivo(true);
        espaco.setExigeAprovacaoManual(false);
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(7);
        usuario.setAtivo(true);
        usuario.setIdPersona(null);
        when(espacos.bloquearParaReserva(2)).thenReturn(Optional.of(espaco));
        when(usuarios.findByEmail("visitante@exemplo.com")).thenReturn(Optional.of(usuario));
        when(usuarios.bloquearParaCota(7)).thenReturn(Optional.of(usuario));
        Usuario administrador = new Usuario();
        administrador.setIdUsuario(2);
        administrador.setAtivo(true);
        administrador.setIdPerfil(1);
        when(usuarios.findAllByAtivoTrueAndIdPerfilLessThan(4)).thenReturn(List.of(administrador));
        when(reservas.existeConflito(eq(2), any(LocalDate.class), any(LocalTime.class), any(LocalTime.class)))
                .thenReturn(false);
        when(reservas.save(any(Reserva.class))).thenAnswer(invocation -> invocation.getArgument(0));
        ReservaRequest request = new ReservaRequest("Oficina", "Encontro de teste", LocalDate.now().plusDays(1),
                LocalTime.of(9, 0), LocalTime.of(10, 0), 2, null, "48999999999");

        Reserva reserva = service.criar(request, "visitante@exemplo.com");

        assertEquals("Pendente", reserva.getStatusReserva());
        assertEquals("+5548999999999", reserva.getTelefoneContato());
        verify(reservas).save(reserva);
        verify(notificacoes).enviar(eq(List.of(administrador)), eq("RESERVA_CRIADA"),
                eq("Solicitação de reserva recebida"), anyString());
    }

    @Test
    void solicitanteP6RecebeReservaPendenteMesmoComEspacoSemAprovacaoManual() {
        Espaco espaco = new Espaco();
        espaco.setIdEspaco(2);
        espaco.setAtivo(true);
        espaco.setExigeAprovacaoManual(false);
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(7);
        usuario.setAtivo(true);
        usuario.setIdPersona(6);
        when(espacos.bloquearParaReserva(2)).thenReturn(Optional.of(espaco));
        when(usuarios.findByEmail("visitante@exemplo.com")).thenReturn(Optional.of(usuario));
        when(usuarios.bloquearParaCota(7)).thenReturn(Optional.of(usuario));
        when(reservas.existeConflito(eq(2), any(LocalDate.class), any(LocalTime.class), any(LocalTime.class)))
                .thenReturn(false);
        when(reservas.save(any(Reserva.class))).thenAnswer(invocation -> invocation.getArgument(0));
        ReservaRequest request = new ReservaRequest("Oficina", "Encontro de teste", LocalDate.now().plusDays(1),
                LocalTime.of(9, 0), LocalTime.of(10, 0), 2, null);

        Reserva reserva = service.criar(request, "visitante@exemplo.com");

        assertEquals("Pendente", reserva.getStatusReserva());
        verify(reservas).save(reserva);
    }

    @Test
    void personaNaoP6RecebeConfirmacaoAutomaticaQuandoNaoHaOutraExcecao() {
        Espaco espaco = new Espaco();
        espaco.setIdEspaco(2);
        espaco.setAtivo(true);
        espaco.setExigeAprovacaoManual(false);
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(7);
        usuario.setAtivo(true);
        usuario.setIdPersona(2);
        when(espacos.bloquearParaReserva(2)).thenReturn(Optional.of(espaco));
        when(usuarios.findByEmail("visitante@exemplo.com")).thenReturn(Optional.of(usuario));
        when(usuarios.bloquearParaCota(7)).thenReturn(Optional.of(usuario));
        when(reservas.existeConflito(eq(2), any(LocalDate.class), any(LocalTime.class), any(LocalTime.class)))
                .thenReturn(false);
        when(reservas.findByIdSolicitanteAndDataReservaBetweenAndStatusReservaIn(
                eq(7), any(LocalDate.class), any(LocalDate.class), anyList())).thenReturn(List.of());
        when(reservas.save(any(Reserva.class))).thenAnswer(invocation -> invocation.getArgument(0));
        ReservaRequest request = new ReservaRequest("Oficina", "Encontro de teste", LocalDate.now().plusDays(1),
                LocalTime.of(9, 0), LocalTime.of(10, 0), 2, null);

        Reserva reserva = service.criar(request, "visitante@exemplo.com");

        assertEquals("Confirmada", reserva.getStatusReserva());
        verify(reservas).save(reserva);
    }

    @Test
    void coordenadorRecebeReservaDeDomingoPendenteMesmoSemSinalDeAprovacaoSemanal() {
        assertReservaDeFimDeSemanaPendenteParaPerfil(1, DayOfWeek.SUNDAY);
    }

    @Test
    void coordenadorRecebeReservaDeSabadoPendenteMesmoSemSinalDeAprovacaoSemanal() {
        assertReservaDeFimDeSemanaPendenteParaPerfil(1, DayOfWeek.SATURDAY);
    }

    @Test
    void gestorDeEspacosRecebeReservaDeDomingoPendenteMesmoSemSinalDeAprovacaoSemanal() {
        assertReservaDeFimDeSemanaPendenteParaPerfil(2, DayOfWeek.SUNDAY);
    }

    @Test
    void staffRecebeReservaDeDomingoPendenteMesmoSemSinalDeAprovacaoSemanal() {
        assertReservaDeFimDeSemanaPendenteParaPerfil(3, DayOfWeek.SUNDAY);
    }

    private void assertReservaDeFimDeSemanaPendenteParaPerfil(int perfil, DayOfWeek diaDaSemana) {
        Espaco espaco = new Espaco();
        espaco.setIdEspaco(2);
        espaco.setAtivo(true);
        espaco.setExigeAprovacaoManual(false);
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(7);
        usuario.setAtivo(true);
        usuario.setIdPerfil(perfil);
        usuario.setIdPersona(1);
        LocalDate dataFimDeSemana = LocalDate.now(ZoneId.of("America/Sao_Paulo"))
                .with(TemporalAdjusters.next(diaDaSemana));
        when(espacos.bloquearParaReserva(2)).thenReturn(Optional.of(espaco));
        when(horarios.validarReserva(eq(espaco), eq(dataFimDeSemana), any(LocalTime.class), any(LocalTime.class)))
                .thenReturn(false);
        when(usuarios.findByEmail("visitante@exemplo.com")).thenReturn(Optional.of(usuario));
        when(usuarios.bloquearParaCota(7)).thenReturn(Optional.of(usuario));
        when(reservas.existeConflito(eq(2), eq(dataFimDeSemana), any(LocalTime.class), any(LocalTime.class)))
                .thenReturn(false);
        when(usuarios.findAllByAtivoTrueAndIdPerfilLessThan(4)).thenReturn(List.of());
        when(reservas.save(any(Reserva.class))).thenAnswer(invocation -> invocation.getArgument(0));
        ReservaRequest request = new ReservaRequest("Oficina", "Encontro de teste", dataFimDeSemana,
                LocalTime.of(10, 0), LocalTime.of(11, 0), 2, null);

        Reserva reserva = service.criar(request, "visitante@exemplo.com");

        assertEquals("Pendente", reserva.getStatusReserva());
        verify(horarios).validarReserva(espaco, dataFimDeSemana, LocalTime.of(10, 0), LocalTime.of(11, 0));
        verify(reservas).save(reserva);
    }

    @Test
    void editarReservaDeFimDeSemanaRequerAprovacaoEParaDiaUtilNaoConfirmaSozinha() {
        LocalDate sunday = LocalDate.now(ZoneId.of("America/Sao_Paulo"))
                .with(TemporalAdjusters.next(DayOfWeek.SUNDAY));
        LocalDate saturday = sunday.minusDays(1);
        LocalDate monday = sunday.plusDays(1);
        Usuario staff = new Usuario();
        staff.setIdUsuario(2);
        staff.setAtivo(true);
        staff.setNomeCompleto("Equipe");
        Usuario solicitante = new Usuario();
        solicitante.setIdUsuario(7);
        solicitante.setAtivo(true);
        solicitante.setIdPersona(1);
        Espaco espaco = new Espaco();
        espaco.setIdEspaco(2);
        espaco.setAtivo(true);
        Reserva reserva = new Reserva();
        reserva.setIdReserva(14);
        reserva.setIdSolicitante(7);
        reserva.setIdEspaco(2);
        reserva.setTituloEvento("Reunião");
        reserva.setDescricaoFinalidade("Teste");
        reserva.setDataReserva(sunday.minusDays(2));
        reserva.setHorarioInicio(LocalTime.of(10, 0));
        reserva.setHorarioFim(LocalTime.of(11, 0));
        reserva.setStatusReserva("Confirmada");
        reserva.setIdUsuarioAprovador(3);
        reserva.setDataAprovacao(java.time.LocalDateTime.now());
        when(reservas.findById(14)).thenReturn(Optional.of(reserva));
        when(usuarios.findByEmail("equipe@inpetu.ufsc.br")).thenReturn(Optional.of(staff));
        when(usuarios.bloquearParaCota(7)).thenReturn(Optional.of(solicitante));
        when(espacos.bloquearParaReserva(2)).thenReturn(Optional.of(espaco));
        when(horarios.validarReserva(eq(espaco), eq(sunday), any(LocalTime.class), any(LocalTime.class)))
                .thenReturn(false);
        when(horarios.validarReserva(eq(espaco), eq(saturday), any(LocalTime.class), any(LocalTime.class)))
                .thenReturn(false);
        when(reservas.existeConflitoParaAtualizacao(eq(2), eq(14), eq(sunday),
                any(LocalTime.class), any(LocalTime.class))).thenReturn(false);
        when(reservas.existeConflitoParaAtualizacao(eq(2), eq(14), eq(saturday),
                any(LocalTime.class), any(LocalTime.class))).thenReturn(false);
        when(reservas.existeConflitoParaAtualizacao(eq(2), eq(14), eq(monday),
                any(LocalTime.class), any(LocalTime.class))).thenReturn(false);
        when(reservas.save(any(Reserva.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReservaEdicaoRequest saturdayEdit = new ReservaEdicaoRequest("Reunião", "Teste", saturday,
                LocalTime.of(10, 0), LocalTime.of(11, 0), 2, null);
        Reserva saturdayReservation = service.editar(14, saturdayEdit, "equipe@inpetu.ufsc.br");

        assertEquals("Pendente", saturdayReservation.getStatusReserva());
        assertNull(saturdayReservation.getIdUsuarioAprovador());
        assertNull(saturdayReservation.getDataAprovacao());

        reserva.setStatusReserva("Confirmada");
        reserva.setIdUsuarioAprovador(3);
        reserva.setDataAprovacao(java.time.LocalDateTime.now());
        ReservaEdicaoRequest sundayEdit = new ReservaEdicaoRequest("Reunião", "Teste", sunday,
                LocalTime.of(10, 0), LocalTime.of(11, 0), 2, null);
        Reserva sundayReservation = service.editar(14, sundayEdit, "equipe@inpetu.ufsc.br");

        assertEquals("Pendente", sundayReservation.getStatusReserva());
        assertNull(sundayReservation.getIdUsuarioAprovador());
        assertNull(sundayReservation.getDataAprovacao());

        ReservaEdicaoRequest mondayEdit = new ReservaEdicaoRequest("Reunião", "Teste", monday,
                LocalTime.of(10, 0), LocalTime.of(11, 0), 2, null);
        Reserva mondayReservation = service.editar(14, mondayEdit, "equipe@inpetu.ufsc.br");

        assertEquals("Pendente", mondayReservation.getStatusReserva());
    }

    @Test
    void espacoComAprovacaoManualContinuaPendenteParaPersonaNaoP6() {
        Espaco espaco = new Espaco();
        espaco.setIdEspaco(2);
        espaco.setAtivo(true);
        espaco.setExigeAprovacaoManual(true);
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(7);
        usuario.setAtivo(true);
        usuario.setIdPersona(2);
        when(espacos.bloquearParaReserva(2)).thenReturn(Optional.of(espaco));
        when(usuarios.findByEmail("visitante@exemplo.com")).thenReturn(Optional.of(usuario));
        when(usuarios.bloquearParaCota(7)).thenReturn(Optional.of(usuario));
        when(reservas.existeConflito(eq(2), any(LocalDate.class), any(LocalTime.class), any(LocalTime.class)))
                .thenReturn(false);
        when(reservas.findByIdSolicitanteAndDataReservaBetweenAndStatusReservaIn(
                eq(7), any(LocalDate.class), any(LocalDate.class), anyList())).thenReturn(List.of());
        when(reservas.save(any(Reserva.class))).thenAnswer(invocation -> invocation.getArgument(0));
        ReservaRequest request = new ReservaRequest("Oficina", "Encontro de teste", LocalDate.now().plusDays(1),
                LocalTime.of(9, 0), LocalTime.of(10, 0), 2, null);

        Reserva reserva = service.criar(request, "visitante@exemplo.com");

        assertEquals("Pendente", reserva.getStatusReserva());
        verify(reservas).save(reserva);
    }

    @Test
    void reservaQueExcedeCotaMensalFicaPendenteEAlertaEquipe() {
        Espaco espaco = new Espaco();
        espaco.setIdEspaco(2);
        espaco.setAtivo(true);
        espaco.setExigeAprovacaoManual(false);
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(7);
        usuario.setNomeCompleto("Joana Silva");
        usuario.setIdPersona(2);
        usuario.setAtivo(true);
        YearMonth mes = YearMonth.from(LocalDate.now(ZoneId.of("America/Sao_Paulo"))).plusMonths(1);
        List<Reserva> existentes = List.of(reservaComCincoHoras(40), reservaComCincoHoras(41),
                reservaComCincoHoras(42), reservaComCincoHoras(43), reservaComCincoHoras(44),
                reservaComCincoHoras(45));
        ReservaRequest request = new ReservaRequest("Oficina", "Encontro de teste", mes.atDay(20),
                LocalTime.of(9, 0), LocalTime.of(11, 0), 2, null);
        when(espacos.bloquearParaReserva(2)).thenReturn(Optional.of(espaco));
        when(usuarios.findByEmail("visitante@exemplo.com")).thenReturn(Optional.of(usuario));
        when(usuarios.bloquearParaCota(7)).thenReturn(Optional.of(usuario));
        when(usuarios.findAllByAtivoTrueAndIdPerfilLessThan(4)).thenReturn(List.of());
        when(reservas.existeConflito(2, request.dataReserva(), request.horarioInicio(), request.horarioFim()))
                .thenReturn(false);
        when(reservas.findByIdSolicitanteAndDataReservaBetweenAndStatusReservaIn(
                7, mes.atDay(1), mes.atEndOfMonth(), List.of("Pendente", "Confirmada")))
                .thenReturn(existentes);
        when(reservas.save(any(Reserva.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Reserva reserva = service.criar(request, "visitante@exemplo.com");

        assertTrue(reserva.getExcedeCota());
        assertEquals("Pendente", reserva.getStatusReserva());
        ArgumentCaptor<String> notificationMessage = ArgumentCaptor.forClass(String.class);
        verify(notificacoes).enviar(anyList(), eq("COTA_EXCEDIDA"), eq("Reserva excede a cota mensal"),
                notificationMessage.capture());
        assertTrue(notificationMessage.getValue().length() <= 255);
        assertTrue(notificationMessage.getValue().contains("cota mensal de 30.0 horas"));
        assertTrue(notificationMessage.getValue().contains("Negociação manual necessária"));
        ArgumentCaptor<LogAuditoria> log = ArgumentCaptor.forClass(LogAuditoria.class);
        verify(auditoria).save(log.capture());
        assertEquals("CRIACAO", log.getValue().getAcao());
        assertNotNull(log.getValue().getDataHora());
    }

    @Test
    void excedenteNaoPodeSerAprovadoSemNegociacaoRegistrada() {
        Usuario equipe = new Usuario();
        equipe.setIdUsuario(2);
        equipe.setIdPerfil(1);
        equipe.setAtivo(true);
        Reserva reserva = new Reserva();
        reserva.setIdReserva(19);
        reserva.setStatusReserva("Pendente");
        reserva.setExcedeCota(true);
        when(usuarios.findByEmail("coord@inpetu.ufsc.br")).thenReturn(Optional.of(equipe));
        when(reservas.findById(19)).thenReturn(Optional.of(reserva));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.aprovar(19, "coord@inpetu.ufsc.br"));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        assertTrue(error.getReason().contains("negociação"));
        verify(reservas, never()).save(any());
        verifyNoInteractions(auditoria, notificacoes);
    }

    @Test
    void aprovacaoNotificaSomenteSolicitante() {
        Usuario equipe = new Usuario();
        equipe.setIdUsuario(2);
        equipe.setIdPerfil(1);
        equipe.setAtivo(true);
        Usuario solicitante = new Usuario();
        solicitante.setIdUsuario(7);
        solicitante.setEmail("solicitante@example.test");
        solicitante.setAtivo(true);
        Espaco espaco = new Espaco();
        espaco.setIdEspaco(3);
        Reserva reserva = new Reserva();
        reserva.setIdReserva(22);
        reserva.setIdSolicitante(7);
        reserva.setIdEspaco(3);
        reserva.setDataReserva(LocalDate.now(ZoneId.of("America/Sao_Paulo"))
                .with(TemporalAdjusters.next(DayOfWeek.SUNDAY)));
        reserva.setHorarioInicio(LocalTime.of(10, 0));
        reserva.setHorarioFim(LocalTime.of(11, 0));
        reserva.setStatusReserva("Pendente");
        when(usuarios.findByEmail("coord@example.test")).thenReturn(Optional.of(equipe));
        when(reservas.findById(22)).thenReturn(Optional.of(reserva));
        when(espacos.bloquearParaReserva(3)).thenReturn(Optional.of(espaco));
        when(reservas.existeReservaAnteriorConflitante(anyInt(), anyInt(), any(), any(), any(), any()))
                .thenReturn(false);
        when(reservas.save(reserva)).thenReturn(reserva);
        when(usuarios.findById(7)).thenReturn(Optional.of(solicitante));

        service.aprovar(22, "coord@example.test");

        assertEquals("Confirmada", reserva.getStatusReserva());
        verifyNoInteractions(horarios);
        verify(notificacoes).enviar(eq(List.of(solicitante)), eq("RESERVA_APROVADA"),
                eq("Reserva aprovada"),
                contains("foi aprovada"));
    }

    @Test
    void visitaGuiadaExigeQuantidadeEAnaliseManualMesmoParaCategoriaAutomatica() {
        Espaco espaco = new Espaco();
        espaco.setIdEspaco(6);
        espaco.setTipoEspaco("Visita guiada");
        espaco.setCapacidadePessoas(20);
        espaco.setAtivo(true);
        espaco.setExigeAprovacaoManual(false);
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(7);
        usuario.setAtivo(true);
        usuario.setIdPersona(6);
        when(espacos.bloquearParaReserva(6)).thenReturn(Optional.of(espaco));
        when(usuarios.findByEmail("visitante@exemplo.com")).thenReturn(Optional.of(usuario));
        when(usuarios.bloquearParaCota(7)).thenReturn(Optional.of(usuario));
        when(reservas.existeConflito(eq(6), any(LocalDate.class), any(LocalTime.class), any(LocalTime.class)))
                .thenReturn(false);
        when(reservas.save(any(Reserva.class))).thenAnswer(invocation -> invocation.getArgument(0));
        ReservaRequest request = new ReservaRequest("Visita do laboratório", "Visita para equipe",
                LocalDate.now().plusDays(1), LocalTime.of(9, 0), LocalTime.of(10, 0), 6, 12);

        Reserva reserva = service.criar(request, "visitante@exemplo.com");

        assertEquals("Pendente", reserva.getStatusReserva());
        assertEquals(12, reserva.getQuantidadeParticipantes());
        verify(reservas).save(reserva);
        verify(reservas).existeConflito(6, request.dataReserva(), request.horarioInicio(), request.horarioFim());
    }

    @Test
    void rejeitaSobreposicaoDeVisitaGuiadaAntesDeCriarPedido() {
        Espaco espaco = new Espaco();
        espaco.setIdEspaco(6);
        espaco.setTipoEspaco("Visita guiada");
        espaco.setCapacidadePessoas(20);
        espaco.setAtivo(true);
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(7);
        usuario.setAtivo(true);
        when(espacos.bloquearParaReserva(6)).thenReturn(Optional.of(espaco));
        when(usuarios.findByEmail("visitante@exemplo.com")).thenReturn(Optional.of(usuario));
        when(usuarios.bloquearParaCota(7)).thenReturn(Optional.of(usuario));
        when(reservas.existeConflito(6, LocalDate.now().plusDays(1), LocalTime.of(9, 0), LocalTime.of(10, 0)))
                .thenReturn(true);
        ReservaRequest request = new ReservaRequest("Visita do laboratório", "Visita para equipe",
                LocalDate.now().plusDays(1), LocalTime.of(9, 0), LocalTime.of(10, 0), 6, 12);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.criar(request, "visitante@exemplo.com"));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        assertTrue(error.getReason().contains("ordem de solicitação"));
        verify(reservas, never()).save(any());
    }

    @Test
    void rejeitaVisitaGuiadaAcimaDaCapacidade() {
        Espaco espaco = new Espaco();
        espaco.setIdEspaco(6);
        espaco.setTipoEspaco("Visita guiada");
        espaco.setCapacidadePessoas(20);
        espaco.setAtivo(true);
        when(espacos.bloquearParaReserva(6)).thenReturn(Optional.of(espaco));
        ReservaRequest request = new ReservaRequest("Visita do laboratório", "Visita para equipe",
                LocalDate.now().plusDays(1), LocalTime.of(9, 0), LocalTime.of(10, 0), 6, 21);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.criar(request, "visitante@exemplo.com"));

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        assertEquals("A quantidade de visitantes excede a capacidade da visita.", error.getReason());
        verifyNoInteractions(usuarios);
        verify(reservas, never()).save(any());
    }

    @Test
    void exigeQuantidadeDeVisitantesParaVisitaGuiada() {
        Espaco espaco = new Espaco();
        espaco.setIdEspaco(6);
        espaco.setTipoEspaco("Visita guiada");
        espaco.setCapacidadePessoas(20);
        espaco.setAtivo(true);
        when(espacos.bloquearParaReserva(6)).thenReturn(Optional.of(espaco));
        ReservaRequest request = new ReservaRequest("Visita do laboratório", "Visita para equipe",
                LocalDate.now().plusDays(1), LocalTime.of(9, 0), LocalTime.of(10, 0), 6, null);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.criar(request, "visitante@exemplo.com"));

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        assertEquals("Informe a quantidade de visitantes.", error.getReason());
        verifyNoInteractions(usuarios);
        verify(reservas, never()).save(any());
    }

    @Test
    void naoAprovaReservaMaisNovaSeJaExisteReservaAnteriorNoHorario() {
        Reserva reserva = new Reserva();
        reserva.setIdReserva(18);
        reserva.setIdEspaco(6);
        reserva.setDataReserva(LocalDate.now().plusDays(1));
        reserva.setHorarioInicio(LocalTime.of(9, 0));
        reserva.setHorarioFim(LocalTime.of(10, 0));
        reserva.setDataSolicitacao(LocalDate.now().atStartOfDay());
        reserva.setStatusReserva("Pendente");
        Espaco espaco = new Espaco();
        espaco.setIdEspaco(6);
        espaco.setTipoEspaco("Visita guiada");
        Usuario aprovador = new Usuario();
        aprovador.setIdUsuario(2);
        aprovador.setAtivo(true);
        when(usuarios.findByEmail("coordenador@inpetu.ufsc.br")).thenReturn(Optional.of(aprovador));
        when(reservas.findById(18)).thenReturn(Optional.of(reserva));
        when(espacos.bloquearParaReserva(6)).thenReturn(Optional.of(espaco));
        when(reservas.existeReservaAnteriorConflitante(6, 18, reserva.getDataReserva(), reserva.getHorarioInicio(),
                reserva.getHorarioFim(), reserva.getDataSolicitacao())).thenReturn(true);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.aprovar(18, "coordenador@inpetu.ufsc.br"));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        assertTrue(error.getReason().contains("reserva mais antiga mantém a prioridade"));
        verify(reservas, never()).save(any());
        verifyNoInteractions(auditoria);
    }

    @Test
    void solicitantePodeCancelarComQuinzeDiasEAuditoriaUsaInstanteDoCancelamento() {
        Usuario solicitante = new Usuario();
        solicitante.setIdUsuario(7);
        solicitante.setIdPerfil(4);
        solicitante.setAtivo(true);
        Reserva reserva = new Reserva();
        reserva.setIdReserva(14);
        reserva.setIdEspaco(2);
        reserva.setIdSolicitante(7);
        reserva.setDataReserva(LocalDate.now().plusDays(15));
        reserva.setHorarioInicio(LocalTime.of(9, 0));
        reserva.setHorarioFim(LocalTime.of(10, 0));
        reserva.setStatusReserva("Pendente");
        when(usuarios.findByEmail("visitante@exemplo.com")).thenReturn(Optional.of(solicitante));
        when(usuarios.findById(7)).thenReturn(Optional.of(solicitante));
        when(usuarios.findAllByAtivoTrueAndIdPerfilLessThan(4)).thenReturn(List.of());
        when(reservas.findById(14)).thenReturn(Optional.of(reserva));
        when(reservas.save(any(Reserva.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Reserva atualizada = service.cancelar(14, "Mudança de agenda", "visitante@exemplo.com");

        assertSame(reserva, atualizada);
        assertEquals("Cancelada", reserva.getStatusReserva());
        ArgumentCaptor<com.inpetu.connect.model.LogAuditoria> logCaptor =
                ArgumentCaptor.forClass(com.inpetu.connect.model.LogAuditoria.class);
        verify(auditoria).save(logCaptor.capture());
        assertNotNull(logCaptor.getValue().getDataHora());
        verify(cancelamentos).save(argThat(cancelamento ->
                cancelamento.getDataCancelamento().equals(logCaptor.getValue().getDataHora())));
        verify(notificacoes).enviar(anyList(), eq("RESERVA_CANCELADA"), eq("Reserva cancelada"), contains("Mudança de agenda"));
    }

    private Reserva reservaComCincoHoras(Integer id) {
        Reserva reserva = new Reserva();
        reserva.setIdReserva(id);
        reserva.setHorarioInicio(LocalTime.of(8, 0));
        reserva.setHorarioFim(LocalTime.of(13, 0));
        return reserva;
    }

    @Test
    void solicitanteNaoPodeCancelarComMenosDeQuinzeDias() {
        Usuario solicitante = new Usuario();
        solicitante.setIdUsuario(7);
        solicitante.setIdPerfil(4);
        solicitante.setAtivo(true);
        Reserva reserva = new Reserva();
        reserva.setIdReserva(14);
        reserva.setIdSolicitante(7);
        reserva.setDataReserva(LocalDate.now().plusDays(14));
        reserva.setStatusReserva("Confirmada");
        when(usuarios.findByEmail("visitante@exemplo.com")).thenReturn(Optional.of(solicitante));
        when(reservas.findById(14)).thenReturn(Optional.of(reserva));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.cancelar(14, "Mudança de agenda", "visitante@exemplo.com"));

        assertEquals(HttpStatus.FORBIDDEN, error.getStatusCode());
        verify(reservas, never()).save(any());
        verifyNoInteractions(cancelamentos, auditoria, notificacoes);
    }

    @Test
    void cancelarReservaAlheiaComoPerfil5NaoLancaErro() {
        Usuario desenvolvedor = new Usuario();
        desenvolvedor.setIdUsuario(5);
        desenvolvedor.setIdPerfil(5);
        desenvolvedor.setAtivo(true);
        Usuario solicitante = new Usuario();
        solicitante.setIdUsuario(7);
        solicitante.setIdPerfil(4);
        solicitante.setAtivo(true);
        Reserva reserva = new Reserva();
        reserva.setIdReserva(14);
        reserva.setIdEspaco(2);
        reserva.setIdSolicitante(7);
        reserva.setDataReserva(LocalDate.now().plusDays(15));
        reserva.setHorarioInicio(LocalTime.of(9, 0));
        reserva.setHorarioFim(LocalTime.of(10, 0));
        reserva.setStatusReserva("Confirmada");
        when(usuarios.findByEmail("desenvolvedor@inpetu.ufsc.br")).thenReturn(Optional.of(desenvolvedor));
        when(usuarios.findById(7)).thenReturn(Optional.of(solicitante));
        when(usuarios.findAllByAtivoTrueAndIdPerfilLessThan(4)).thenReturn(List.of());
        when(reservas.findById(14)).thenReturn(Optional.of(reserva));
        when(reservas.save(any(Reserva.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertDoesNotThrow(() ->
                service.cancelar(14, "Cancelamento pela equipe", "desenvolvedor@inpetu.ufsc.br"));

        assertEquals("Cancelada", reserva.getStatusReserva());
        verify(reservas).save(reserva);
    }

    @Test
    void cancelarReservaAlheiaComoPerfil4RecebeForbidden() {
        Usuario coordenador = new Usuario();
        coordenador.setIdUsuario(4);
        coordenador.setIdPerfil(4);
        coordenador.setAtivo(true);
        Reserva reserva = new Reserva();
        reserva.setIdReserva(14);
        reserva.setIdSolicitante(7);
        reserva.setDataReserva(LocalDate.now().plusDays(15));
        reserva.setStatusReserva("Confirmada");
        when(usuarios.findByEmail("coordenador@inpetu.ufsc.br")).thenReturn(Optional.of(coordenador));
        when(reservas.findById(14)).thenReturn(Optional.of(reserva));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.cancelar(14, "Cancelamento solicitado", "coordenador@inpetu.ufsc.br"));

        assertEquals(HttpStatus.FORBIDDEN, error.getStatusCode());
        assertEquals("Você não pode cancelar esta reserva.", error.getReason());
        verify(reservas, never()).save(any());
    }

    @Test
    void equipePodeCancelarComMenosDeQuinzeDias() {
        Usuario equipe = new Usuario();
        equipe.setIdUsuario(2);
        equipe.setIdPerfil(2);
        equipe.setAtivo(true);
        Usuario solicitante = new Usuario();
        solicitante.setIdUsuario(7);
        solicitante.setIdPerfil(4);
        solicitante.setAtivo(true);
        Reserva reserva = new Reserva();
        reserva.setIdReserva(14);
        reserva.setIdEspaco(2);
        reserva.setIdSolicitante(7);
        reserva.setDataReserva(LocalDate.now().plusDays(14));
        reserva.setStatusReserva("Confirmada");
        when(usuarios.findByEmail("gestor@inpetu.ufsc.br")).thenReturn(Optional.of(equipe));
        when(usuarios.findById(7)).thenReturn(Optional.of(solicitante));
        when(usuarios.findAllByAtivoTrueAndIdPerfilLessThan(4)).thenReturn(List.of(equipe));
        when(reservas.findById(14)).thenReturn(Optional.of(reserva));
        when(reservas.save(any(Reserva.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertDoesNotThrow(() -> service.cancelar(14, "Exceção autorizada", "gestor@inpetu.ufsc.br"));

        assertEquals("Cancelada", reserva.getStatusReserva());
        verify(auditoria).save(any());
        verify(notificacoes).enviar(anyList(), eq("RESERVA_CANCELADA"), anyString(), anyString());
    }
}
