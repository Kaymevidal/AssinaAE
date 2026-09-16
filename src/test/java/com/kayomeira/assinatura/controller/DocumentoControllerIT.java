package com.kayomeira.assinatura.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A conversão de verdade (via LibreOffice/pdf2docx) não roda no CI — esses
 * binários não estão instalados aqui, só na imagem Docker (ver Dockerfile).
 * O que dá pra cobrir sem eles: autenticação exigida e validação de entrada.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DocumentoControllerIT {

    private static final String PDF_MINIMO = """
            %PDF-1.1
            1 0 obj << /Type /Catalog /Pages 2 0 R >> endobj
            2 0 obj << /Type /Pages /Kids [3 0 R] /Count 1 >> endobj
            3 0 obj << /Type /Page /Parent 2 0 R /MediaBox [0 0 200 200] >> endobj
            xref
            0 4
            0000000000 65535 f\s
            trailer << /Size 4 /Root 1 0 R >>
            startxref
            0
            %%EOF
            """;

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /** DocumentoController só exige estar autenticado — diferente de criar contrato, não checa email verificado. */
    private String registrar(String email) throws Exception {
        String resposta = mockMvc.perform(post("/api/auth/registrar")
                        .contentType("application/json")
                        .content("{\"nome\":\"Profissional Teste\",\"email\":\"" + email + "\",\"senha\":\"senhaValida123\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(resposta).get("token").asText();
    }

    @Test
    void docxParaPdfSemAutenticacaoEhBloqueado() throws Exception {
        MockMultipartFile arquivo = new MockMultipartFile("arquivo", "c.docx", "application/octet-stream", "conteudo".getBytes());

        mockMvc.perform(multipart("/api/documentos/docx-para-pdf").file(arquivo))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void docxParaPdfComArquivoVazioEhRejeitado() throws Exception {
        String tokenAuth = registrar("documento-it@teste.com");
        MockMultipartFile arquivoVazio = new MockMultipartFile("arquivo", "c.docx", "application/octet-stream", new byte[0]);

        mockMvc.perform(multipart("/api/documentos/docx-para-pdf").file(arquivoVazio)
                        .header("Authorization", "Bearer " + tokenAuth))
                .andExpect(status().isBadRequest());
    }

    @Test
    void pdfParaDocxComArquivoVazioEhRejeitado() throws Exception {
        String tokenAuth = registrar("documento-it-2@teste.com");
        MockMultipartFile arquivoVazio = new MockMultipartFile("arquivo", "c.pdf", "application/pdf", new byte[0]);

        mockMvc.perform(multipart("/api/documentos/pdf-para-docx").file(arquivoVazio)
                        .header("Authorization", "Bearer " + tokenAuth))
                .andExpect(status().isBadRequest());
    }

    @Test
    void editarPdfAplicaEdicaoDeTexto() throws Exception {
        String tokenAuth = registrar("documento-it-3@teste.com");
        MockMultipartFile pdf = new MockMultipartFile("pdf", "c.pdf", "application/pdf", PDF_MINIMO.getBytes());
        MockMultipartFile edicoes = new MockMultipartFile(
                "edicoes", "", "application/json",
                "[{\"pagina\":0,\"x\":0.1,\"y\":0.1,\"largura\":0.3,\"altura\":0.05,\"texto\":\"Texto novo\"}]".getBytes());

        MvcResult resultado = mockMvc.perform(multipart("/api/documentos/editar-pdf")
                        .file(pdf).file(edicoes)
                        .header("Authorization", "Bearer " + tokenAuth))
                .andExpect(status().isOk())
                .andReturn();

        byte[] corpo = resultado.getResponse().getContentAsByteArray();
        assertThat(corpo.length).isGreaterThan(0);
        assertThat(new String(corpo, 0, 5)).isEqualTo("%PDF-");
    }
}
