package com.kayomeira.assinatura.controller;

import com.kayomeira.assinatura.dto.AssinaturaRequestDTO;
import com.kayomeira.assinatura.dto.ContratoAssinaturaDTO;
import com.kayomeira.assinatura.dto.ContratoRequestDTO;
import com.kayomeira.assinatura.dto.ContratoResponseDTO;
import com.kayomeira.assinatura.exception.ContratoNotFoundException;
import com.kayomeira.assinatura.model.Contrato;
import com.kayomeira.assinatura.service.ContratoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/contratos")
@RequiredArgsConstructor
public class ContratoController {

    private final ContratoService contratoService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ContratoResponseDTO> criar(
            @Valid @RequestPart("contrato") ContratoRequestDTO dto,
            @RequestPart("pdf") MultipartFile pdf) throws IOException {
        Contrato contrato = contratoService.criarContrato(dto, pdf);
        return ResponseEntity.ok(ContratoResponseDTO.fromEntity(contrato));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ContratoResponseDTO> buscarPorId(@PathVariable Long id) {
        Contrato contrato = contratoService.buscarPorId(id);
        return ResponseEntity.ok(ContratoResponseDTO.fromEntity(contrato));
    }

    @GetMapping("/token/{token}")
    public ResponseEntity<ContratoAssinaturaDTO> buscarPorToken(@PathVariable String token) {
        return ResponseEntity.ok(contratoService.buscarPorToken(token));
    }

    @PostMapping("/token/{token}/assinar")
    public ResponseEntity<ContratoAssinaturaDTO> assinar(
            @PathVariable String token,
            @Valid @RequestBody AssinaturaRequestDTO dto) throws IOException {
        return ResponseEntity.ok(contratoService.assinar(token, dto.getAssinaturaBase64()));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> download(@PathVariable Long id) {
        Contrato contrato = contratoService.buscarPorId(id);

        if (!contrato.ambosAssinaram()) {
            throw new ContratoNotFoundException("O contrato ainda não foi assinado por ambas as partes");
        }

        String nomeArquivo = "contrato-" + id + "-assinado.pdf";
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nomeArquivo + "\"")
                .body(contrato.getPdfAssinado());
    }
}
