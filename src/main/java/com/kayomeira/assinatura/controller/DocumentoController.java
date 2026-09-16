package com.kayomeira.assinatura.controller;

import com.kayomeira.assinatura.dto.EdicaoTextoDTO;
import com.kayomeira.assinatura.exception.PdfInvalidoException;
import com.kayomeira.assinatura.service.ConversaoDocxService;
import com.kayomeira.assinatura.service.EdicaoPdfService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/documentos")
@RequiredArgsConstructor
public class DocumentoController {

    private static final MediaType DOCX = MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document");

    private final ConversaoDocxService conversaoDocxService;
    private final EdicaoPdfService edicaoPdfService;

    @PostMapping(value = "/docx-para-pdf", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<byte[]> docxParaPdf(@RequestPart("arquivo") MultipartFile arquivo) throws IOException {
        validarArquivo(arquivo);
        byte[] pdf = conversaoDocxService.paraPdf(arquivo.getBytes());
        return comoAnexo(pdf, "convertido.pdf", MediaType.APPLICATION_PDF);
    }

    @PostMapping(value = "/pdf-para-docx", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<byte[]> pdfParaDocx(@RequestPart("arquivo") MultipartFile arquivo) throws IOException {
        validarArquivo(arquivo);
        byte[] docx = conversaoDocxService.paraDocx(arquivo.getBytes());
        return comoAnexo(docx, "convertido.docx", DOCX);
    }

    @PostMapping(value = "/editar-pdf", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<byte[]> editarPdf(
            @RequestPart("pdf") MultipartFile pdf,
            @RequestPart("edicoes") List<EdicaoTextoDTO> edicoes) throws IOException {
        validarArquivo(pdf);
        byte[] pdfEditado = edicaoPdfService.aplicarEdicoes(pdf.getBytes(), edicoes);
        return comoAnexo(pdfEditado, "editado.pdf", MediaType.APPLICATION_PDF);
    }

    private void validarArquivo(MultipartFile arquivo) {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new PdfInvalidoException("Selecione um arquivo para converter");
        }
    }

    private ResponseEntity<byte[]> comoAnexo(byte[] conteudo, String nomeArquivo, MediaType tipo) {
        return ResponseEntity.ok()
                .contentType(tipo)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nomeArquivo + "\"")
                .body(conteudo);
    }
}
