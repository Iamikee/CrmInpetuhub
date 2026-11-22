package com.inpetu.connect.controller;

import com.inpetu.connect.model.DuvidaAtendimento;
import com.inpetu.connect.repository.DuvidaAtendimentoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DuvidaControllerTest {
    @TempDir Path tempDirectory;
    DuvidaAtendimentoRepository duvidas;
    DuvidaController controller;

    @BeforeEach
    void setUp() {
        duvidas = mock(DuvidaAtendimentoRepository.class);
        controller = new DuvidaController(duvidas, tempDirectory.resolve("uploads").toString());
    }

    @Test
    void encerraCriacaoLegadaSemPersistirTicket() {
        ResponseStatusException error = assertThrows(ResponseStatusException.class, controller::criar);

        assertEquals(HttpStatus.GONE, error.getStatusCode());
        verifyNoInteractions(duvidas);
    }

    @Test
    void encerraCriacaoLegadaComAnexo() {
        ResponseStatusException error = assertThrows(ResponseStatusException.class, controller::criarComAnexo);

        assertEquals(HttpStatus.GONE, error.getStatusCode());
        verifyNoInteractions(duvidas);
    }

    @Test
    void encerraRespostaLegadaQueEnviavaEmail() {
        ResponseStatusException error = assertThrows(ResponseStatusException.class, () -> controller.responder(7));

        assertEquals(HttpStatus.GONE, error.getStatusCode());
        verifyNoInteractions(duvidas);
    }

    @Test
    void retornaNotFoundParaSolicitacaoOuAnexoInexistente() {
        when(duvidas.findById(999)).thenReturn(Optional.empty());

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
            () -> controller.baixarAnexo(999));

        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
    }

    @Test
    void permiteConsultarHistoricoLegado() {
        DuvidaAtendimento historic = new DuvidaAtendimento();
        when(duvidas.findAll()).thenReturn(java.util.List.of(historic));

        assertEquals(java.util.List.of(historic), controller.listar());
    }
}
