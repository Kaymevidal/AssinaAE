package com.kayomeira.assinatura.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kayomeira.assinatura.model.Contrato;
import com.kayomeira.assinatura.model.Profissional;
import com.kayomeira.assinatura.model.StatusAssinatura;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Trava de regressão: os DTOs que saem pela API nunca podem carregar o hash
 * da senha, o id do Google, ou os bytes do PDF — nem por um campo novo
 * adicionado sem querer, nem pelo JSON de fato serializado.
 */
class DtoSemDadosSensiveisTest {

    private static final Set<String> CAMPOS_PROIBIDOS = Set.of(
            "senha", "senhaHash", "googleId", "pdfOriginal", "pdfAssinado",
            "assinaturaProfissionalBase64", "assinaturaClienteBase64");

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void authResponseDtoNaoTemCamposSensiveis() {
        verificarClasse(AuthResponseDTO.class);

        Profissional profissional = Profissional.builder()
                .id(1L).nome("Ana").email("ana@teste.com")
                .senhaHash("$2a$10$hashSecretoQueNuncaPodeVazar")
                .googleId("google-secreto-123")
                .build();

        String json = serializar(AuthResponseDTO.de("token-fake", profissional));

        assertThat(json).doesNotContain("hashSecretoQueNuncaPodeVazar", "google-secreto-123", "senhaHash", "googleId");
    }

    @Test
    void contratoResponseDtoNaoTemCamposSensiveis() throws Exception {
        verificarClasse(ContratoResponseDTO.class);

        Profissional profissional = Profissional.builder().id(1L).nome("Ana").email("ana@teste.com").build();
        Contrato contrato = Contrato.builder()
                .id(1L).titulo("T").descricao("D")
                .profissional(profissional)
                .nomeCliente("Cliente")
                .pdfOriginal("PDF-ORIGINAL-SECRETO".getBytes())
                .pdfAssinado("PDF-ASSINADO-SECRETO".getBytes())
                .statusProfissional(StatusAssinatura.PENDENTE)
                .statusCliente(StatusAssinatura.PENDENTE)
                .build();

        String json = serializar(ContratoResponseDTO.fromEntity(contrato));

        assertThat(json).doesNotContain("PDF-ORIGINAL-SECRETO", "PDF-ASSINADO-SECRETO", "pdfOriginal", "pdfAssinado");
    }

    @Test
    void contratoAssinaturaDtoNaoTemCamposSensiveis() {
        verificarClasse(ContratoAssinaturaDTO.class);

        Profissional profissional = Profissional.builder().id(1L).nome("Ana").email("ana@teste.com").build();
        Contrato contrato = Contrato.builder()
                .id(1L).titulo("T").descricao("D")
                .profissional(profissional)
                .nomeCliente("Cliente")
                .pdfOriginal("PDF-ORIGINAL-SECRETO".getBytes())
                .statusProfissional(StatusAssinatura.PENDENTE)
                .statusCliente(StatusAssinatura.PENDENTE)
                .build();

        String json = serializar(ContratoAssinaturaDTO.fromEntity(contrato, "PROFISSIONAL"));

        assertThat(json).doesNotContain("PDF-ORIGINAL-SECRETO", "tokenProfissional", "tokenCliente");
    }

    private void verificarClasse(Class<?> dto) {
        Set<String> camposDaClasse = Arrays.stream(dto.getDeclaredFields())
                .map(Field::getName)
                .collect(java.util.stream.Collectors.toSet());

        assertThat(camposDaClasse)
                .as("Campos declarados em %s", dto.getSimpleName())
                .doesNotContainAnyElementsOf(CAMPOS_PROIBIDOS);
    }

    private String serializar(Object dto) {
        try {
            return objectMapper.writeValueAsString(dto);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
