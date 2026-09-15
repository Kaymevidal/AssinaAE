package com.kayomeira.assinatura.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kayomeira.assinatura.model.Contrato;
import com.kayomeira.assinatura.model.Profissional;
import com.kayomeira.assinatura.repository.ContratoRepository;
import com.kayomeira.assinatura.repository.ProfissionalRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ContratoControllerIT {

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

    private static final String ASSINATURA_BASE64 =
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProfissionalRepository profissionalRepository;

    @Autowired
    private ContratoRepository contratoRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private String registrarLogarEVerificar(String email) throws Exception {
        String resposta = mockMvc.perform(post("/api/auth/registrar")
                        .contentType("application/json")
                        .content("{\"nome\":\"Profissional Teste\",\"email\":\"" + email + "\",\"senha\":\"senhaValida123\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode json = objectMapper.readTree(resposta);
        String token = json.get("token").asText();

        Profissional profissional = profissionalRepository.findByEmail(email).orElseThrow();
        mockMvc.perform(get("/api/auth/verificar-email/{token}", profissional.getTokenVerificacaoEmail()))
                .andExpect(status().isOk());

        return token;
    }

    private MvcResult criarContrato(String tokenAuth, String emailCliente) throws Exception {
        MockMultipartFile pdf = new MockMultipartFile("pdf", "contrato.pdf", "application/pdf", PDF_MINIMO.getBytes());
        MockMultipartFile contrato = new MockMultipartFile(
                "contrato", "", "application/json",
                ("{\"titulo\":\"Contrato IT\",\"descricao\":\"Desc\",\"emailCliente\":\"" + emailCliente + "\",\"nomeCliente\":\"Cliente IT\"}").getBytes());

        return mockMvc.perform(multipart("/api/contratos")
                        .file(pdf).file(contrato)
                        .header("Authorization", "Bearer " + tokenAuth))
                .andExpect(status().isOk())
                .andReturn();
    }

    @Test
    void criarContratoSemEmailVerificadoEhBloqueado() throws Exception {
        String resposta = mockMvc.perform(post("/api/auth/registrar")
                        .contentType("application/json")
                        .content("""
                                {"nome":"Sem Verificar","email":"nao-verificado-it@teste.com","senha":"senhaValida123"}"""))
                .andReturn().getResponse().getContentAsString();
        String token = objectMapper.readTree(resposta).get("token").asText();

        MockMultipartFile pdf = new MockMultipartFile("pdf", "c.pdf", "application/pdf", PDF_MINIMO.getBytes());
        MockMultipartFile contrato = new MockMultipartFile(
                "contrato", "", "application/json",
                "{\"titulo\":\"T\",\"descricao\":\"D\",\"emailCliente\":\"c@teste.com\",\"nomeCliente\":\"C\"}".getBytes());

        mockMvc.perform(multipart("/api/contratos").file(pdf).file(contrato)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void fluxoCompletoDeAssinaturaEIsolamentoEntreContas() throws Exception {
        String tokenA = registrarLogarEVerificar("dono-a-it@teste.com");
        String tokenB = registrarLogarEVerificar("dono-b-it@teste.com");

        MvcResult criacao = criarContrato(tokenA, "cliente-it@teste.com");
        Long contratoId = objectMapper.readTree(criacao.getResponse().getContentAsString()).get("id").asLong();

        // conta B não acessa contrato da conta A
        mockMvc.perform(get("/api/contratos/{id}", contratoId).header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());

        Contrato contrato = contratoRepository.findById(contratoId).orElseThrow();
        String tokenProfissional = contrato.getTokenProfissional();
        String tokenCliente = contrato.getTokenCliente();

        mockMvc.perform(post("/api/contratos/token/{token}/confirmar-leitura", tokenCliente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documentoVisualizado").value(true));

        mockMvc.perform(post("/api/contratos/token/{token}/assinar", tokenCliente)
                        .contentType("application/json")
                        .content("{\"assinaturaBase64\":\"" + ASSINATURA_BASE64 + "\",\"pagina\":0,\"x\":0.1,\"y\":0.8,\"largura\":0.3,\"altura\":0.1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ambosAssinaram").value(false));

        mockMvc.perform(post("/api/contratos/token/{token}/confirmar-leitura", tokenProfissional))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/contratos/token/{token}/assinar", tokenProfissional)
                        .contentType("application/json")
                        .content("{\"assinaturaBase64\":\"" + ASSINATURA_BASE64 + "\",\"pagina\":0,\"x\":0.1,\"y\":0.8,\"largura\":0.3,\"altura\":0.1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ambosAssinaram").value(true));

        mockMvc.perform(get("/api/contratos/token/{token}/download", tokenCliente))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"));
    }

    @Test
    void assinarSemConfirmarLeituraEhBloqueado() throws Exception {
        String tokenA = registrarLogarEVerificar("leitura-dono-it@teste.com");
        MvcResult criacao = criarContrato(tokenA, "leitura-cliente-it@teste.com");
        Long contratoId = objectMapper.readTree(criacao.getResponse().getContentAsString()).get("id").asLong();
        Contrato contrato = contratoRepository.findById(contratoId).orElseThrow();

        mockMvc.perform(post("/api/contratos/token/{token}/assinar", contrato.getTokenCliente())
                        .contentType("application/json")
                        .content("{\"assinaturaBase64\":\"" + ASSINATURA_BASE64 + "\",\"pagina\":0,\"x\":0.1,\"y\":0.8,\"largura\":0.3,\"altura\":0.1}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/contratos/token/{token}/confirmar-leitura", contrato.getTokenCliente()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documentoVisualizado").value(true));

        mockMvc.perform(post("/api/contratos/token/{token}/assinar", contrato.getTokenCliente())
                        .contentType("application/json")
                        .content("{\"assinaturaBase64\":\"" + ASSINATURA_BASE64 + "\",\"pagina\":0,\"x\":0.1,\"y\":0.8,\"largura\":0.3,\"altura\":0.1}"))
                .andExpect(status().isOk());
    }

    @Test
    void recusaDeUmaParteImpedeAOutraDeAssinar() throws Exception {
        String tokenA = registrarLogarEVerificar("recusa-dono-it@teste.com");
        MvcResult criacao = criarContrato(tokenA, "recusa-cliente-it@teste.com");
        Long contratoId = objectMapper.readTree(criacao.getResponse().getContentAsString()).get("id").asLong();

        Contrato contrato = contratoRepository.findById(contratoId).orElseThrow();

        mockMvc.perform(post("/api/contratos/token/{token}/rejeitar", contrato.getTokenCliente()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusPapel").value("REJEITADO"));

        mockMvc.perform(post("/api/contratos/token/{token}/assinar", contrato.getTokenProfissional())
                        .contentType("application/json")
                        .content("{\"assinaturaBase64\":\"" + ASSINATURA_BASE64 + "\",\"pagina\":0,\"x\":0.1,\"y\":0.8,\"largura\":0.3,\"altura\":0.1}"))
                .andExpect(status().isConflict());
    }
}
