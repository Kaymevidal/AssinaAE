package com.kayomeira.assinatura.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private static final String SEGREDO = "segredo-de-teste-com-pelo-menos-32-caracteres-0123456789";

    private final JwtService jwtService = new JwtService(SEGREDO, 7);

    @Test
    void gerarTokenEValidarDevolveOMesmoId() {
        String token = jwtService.gerarToken(42L);

        assertThat(jwtService.validarEExtrairProfissionalId(token)).isEqualTo(42L);
    }

    @Test
    void tokenComAssinaturaAdulteradaEhRejeitado() {
        String token = jwtService.gerarToken(1L);
        // Troca o último caractere da assinatura, invalidando-a sem alterar o formato do JWT.
        String adulterado = token.substring(0, token.length() - 1) + (token.endsWith("A") ? "B" : "A");

        assertThat(jwtService.validarEExtrairProfissionalId(adulterado)).isNull();
    }

    @Test
    void tokenAssinadoComOutroSegredoEhRejeitado() {
        JwtService outroServico = new JwtService("outro-segredo-completamente-diferente-0123456789ABCDE", 7);
        String tokenDeOutraOrigem = outroServico.gerarToken(1L);

        assertThat(jwtService.validarEExtrairProfissionalId(tokenDeOutraOrigem)).isNull();
    }

    @Test
    void tokenExpiradoEhRejeitado() {
        SecretKey chave = Keys.hmacShaKeyFor(SEGREDO.getBytes(StandardCharsets.UTF_8));
        Instant passado = Instant.now().minusSeconds(3600);
        String tokenExpirado = Jwts.builder()
                .subject("7")
                .issuedAt(Date.from(passado.minusSeconds(10)))
                .expiration(Date.from(passado))
                .signWith(chave)
                .compact();

        assertThat(jwtService.validarEExtrairProfissionalId(tokenExpirado)).isNull();
    }

    @Test
    void textoAleatorioNaoQuebraAValidacao() {
        assertThat(jwtService.validarEExtrairProfissionalId("isso-nao-e-um-jwt")).isNull();
        assertThat(jwtService.validarEExtrairProfissionalId("")).isNull();
    }
}
