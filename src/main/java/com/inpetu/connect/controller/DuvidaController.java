package com.inpetu.connect.controller;

import com.inpetu.connect.model.DuvidaAtendimento;
import com.inpetu.connect.repository.DuvidaAtendimentoRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@RestController
@RequestMapping("/api/v1/duvidas")
public class DuvidaController {
    private final DuvidaAtendimentoRepository duvidas;
    private final Path uploadDirectory;

    public DuvidaController(DuvidaAtendimentoRepository duvidas,
                            @Value("${app.upload.dir:uploads/inpetu}") String uploadDirectory) {
        this.duvidas = duvidas;
        this.uploadDirectory = Path.of(uploadDirectory).toAbsolutePath().normalize();
    }

    @PostMapping
    public void criar() {
        throw legacyWriteDisabled();
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public void criarComAnexo() {
        throw legacyWriteDisabled();
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('COORDENADOR','STAFF')")
    public List<DuvidaAtendimento> listar() {
        return duvidas.findAll();
    }

    @GetMapping("/{id}/anexo")
    @PreAuthorize("hasAnyRole('COORDENADOR','STAFF')")
    public ResponseEntity<Resource> baixarAnexo(@PathVariable Integer id) {
        DuvidaAtendimento duvida = duvidas.findById(id).orElseThrow(() ->
            new ResponseStatusException(HttpStatus.NOT_FOUND, "Solicitação de atendimento não encontrada."));
        if (duvida.getCaminhoAnexo() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Esta dúvida não possui anexo.");
        }
        Path file = uploadDirectory.resolve(duvida.getCaminhoAnexo()).normalize();
        if (!file.getParent().equals(uploadDirectory) || !Files.isRegularFile(file)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Anexo não encontrado.");
        }
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_OCTET_STREAM)
            .header(HttpHeaders.CONTENT_DISPOSITION,
                ContentDisposition.attachment().filename(duvida.getCaminhoAnexo()).build().toString())
            .body(new FileSystemResource(file));
    }

    @PostMapping("/{id}/responder")
    @PreAuthorize("hasAnyRole('COORDENADOR','STAFF')")
    public void responder(@PathVariable Integer id) {
        throw legacyWriteDisabled();
    }

    private ResponseStatusException legacyWriteDisabled() {
        return new ResponseStatusException(HttpStatus.GONE,
            "Este fluxo legado foi encerrado. Use o Help Desk para criar e responder tickets no site.");
    }
}
