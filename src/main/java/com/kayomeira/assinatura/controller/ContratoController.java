package com.kayomeira.assinatura.controller;

import com.kayomeira.assinatura.dto.AssinaturaRequestDTO;
import com.kayomeira.assinatura.dto.ContratoAssinaturaDTO;
import com.kayomeira.assinatura.dto.ContratoRequestDTO;
import com.kayomeira.assinatura.dto.ContratoResponseDTO;
import com.kayomeira.assinatura.exception.ContratoNotFoundException;
import com.kayomeira.assinatura.model.Contrato;
import com.kayomeira.assinatura.model.Profissional;
import com.kayomeira.assinatura.service.ContratoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/contratos")
@RequiredArgsConstructor
public class ContratoController {

    private final ContratoService contratoService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ContratoResponseDTO> criar(
            @Valid @RequestPart("contrato") ContratoRequestDTO dto,
            @RequestPart("pdf") MultipartFile pdf,
            @AuthenticationPrincipal Profissional profissional) throws IOException {
        Contrato contrato = contratoService.criarContrato(dto, pdf, profissional);
        return ResponseEntity.ok(ContratoResponseDTO.fromEntity(contrato));
    }

    /** Painel do profissional: todos os contratos que ele criou. */
    @GetMapping("/meus")
    public ResponseEntity<List<ContratoResponseDTO>> meusContratos(@AuthenticationPrincipal Profissional profissional) {
        List<ContratoResponseDTO> contratos = contratoService.buscarPorProfissional(profissional.getId()).stream()
                .map(ContratoResponseDTO::fromEntity)
                .toList();
        return ResponseEntity.ok(contratos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ContratoResponseDTO> buscarPorId(@PathVariable Long id, @AuthenticationPrincipal Profissional profissional) {
        Contrato contrato = contratoService.buscarPorIdDoProfissional(id, profissional.getId());
        return ResponseEntity.ok(ContratoResponseDTO.fromEntity(contrato));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> download(@PathVariable Long id, @AuthenticationPrincipal Profissional profissional) {
        Contrato contrato = contratoService.buscarPorIdDoProfissional(id, profissional.getId());

        if (!contrato.ambosAssinaram()) {
            throw new ContratoNotFoundException("O contrato ainda não foi assinado por ambas as partes");
        }

        return pdfComoAnexo(contrato.getPdfAssinado(), "contrato-" + id + "-assinado.pdf");
    }

    /** Acesso público do cliente/profissional pelo próprio link de assinatura, sem login. */
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

    @GetMapping("/token/{token}/download")
    public ResponseEntity<byte[]> downloadPorToken(@PathVariable String token) {
        byte[] pdf = contratoService.baixarPdfPorToken(token);
        return pdfComoAnexo(pdf, "contrato-assinado.pdf");
    }

    private ResponseEntity<byte[]> pdfComoAnexo(byte[] pdf, String nomeArquivo) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nomeArquivo + "\"")
                .body(pdf);
    }
}
