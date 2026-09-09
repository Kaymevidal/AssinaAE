package com.kayomeira.assinatura.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kayomeira.assinatura.model.Profissional;
import com.kayomeira.assinatura.repository.ProfissionalRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Testes de integração reais: sobem o contexto Spring inteiro (Security,
 * JPA, controllers) contra um H2 em memória, provando que a fiação — não só
 * cada peça isolada — funciona.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProfissionalRepository profissionalRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void registrarELoginFuncionamDePontaAPonta() throws Exception {
        String corpoRegistro = """
                {"nome":"Ana","email":"ana-it@teste.com","senha":"senhaValida123"}""";

        mockMvc.perform(post("/api/auth/registrar")
                        .contentType("application/json")
                        .content(corpoRegistro))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.email").value("ana-it@teste.com"))
                .andExpect(jsonPath("$.emailVerificado").value(false));

        String corpoLogin = """
                {"email":"ana-it@teste.com","senha":"senhaValida123"}""";

        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(corpoLogin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void registrarComEmailDuplicadoDevolve409() throws Exception {
        String corpo = """
                {"nome":"Ana","email":"duplicado-it@teste.com","senha":"senhaValida123"}""";

        mockMvc.perform(post("/api/auth/registrar").contentType("application/json").content(corpo))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/registrar").contentType("application/json").content(corpo))
                .andExpect(status().isConflict());
    }

    @Test
    void loginComSenhaErradaDevolve401() throws Exception {
        mockMvc.perform(post("/api/auth/registrar")
                        .contentType("application/json")
                        .content("""
                                {"nome":"Ana","email":"senha-errada-it@teste.com","senha":"senhaValida123"}"""))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content("""
                                {"email":"senha-errada-it@teste.com","senha":"senhaTotalmenteErrada"}"""))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void acessarRotaProtegidaSemTokenDevolve401() throws Exception {
        mockMvc.perform(get("/api/contratos/meus"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void verificarEmailComTokenValidoAtivaAConta() throws Exception {
        String resposta = mockMvc.perform(post("/api/auth/registrar")
                        .contentType("application/json")
                        .content("""
                                {"nome":"Ana","email":"verificar-it@teste.com","senha":"senhaValida123"}"""))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode json = objectMapper.readTree(resposta);
        Long id = json.get("id").asLong();
        Profissional profissional = profissionalRepository.findById(id).orElseThrow();
        assertThat(profissional.isEmailVerificado()).isFalse();
        String tokenVerificacao = profissional.getTokenVerificacaoEmail();
        assertThat(tokenVerificacao).isNotBlank();

        mockMvc.perform(get("/api/auth/verificar-email/{token}", tokenVerificacao))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.emailVerificado").value(true));

        assertThat(profissionalRepository.findById(id).orElseThrow().isEmailVerificado()).isTrue();
    }

    @Test
    void redefinirSenhaComTokenValidoTrocaASenhaEfetivamente() throws Exception {
        mockMvc.perform(post("/api/auth/registrar")
                        .contentType("application/json")
                        .content("""
                                {"nome":"Ana","email":"reset-it@teste.com","senha":"senhaAntiga123"}"""))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/esqueci-senha")
                        .contentType("application/json")
                        .content("""
                                {"email":"reset-it@teste.com"}"""))
                .andExpect(status().isOk());

        Profissional profissional = profissionalRepository.findByEmail("reset-it@teste.com").orElseThrow();
        String tokenReset = profissional.getTokenResetSenha();
        assertThat(tokenReset).isNotBlank();

        mockMvc.perform(post("/api/auth/redefinir-senha")
                        .contentType("application/json")
                        .content("{\"token\":\"" + tokenReset + "\",\"novaSenha\":\"senhaNovaSuperSecreta\"}"))
                .andExpect(status().isOk());

        // senha antiga não funciona mais
        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content("""
                                {"email":"reset-it@teste.com","senha":"senhaAntiga123"}"""))
                .andExpect(status().isUnauthorized());

        // senha nova funciona
        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content("""
                                {"email":"reset-it@teste.com","senha":"senhaNovaSuperSecreta"}"""))
                .andExpect(status().isOk());
    }
}
