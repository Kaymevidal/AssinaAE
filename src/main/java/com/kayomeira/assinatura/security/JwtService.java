package com.kayomeira.assinatura.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey chave;
    private final long expiracaoDias;

    public JwtService(
            @Value("${app.jwt-secret}") String segredo,
            @Value("${app.jwt-expiracao-dias:7}") long expiracaoDias) {
        this.chave = Keys.hmacShaKeyFor(segredo.getBytes(StandardCharsets.UTF_8));
        this.expiracaoDias = expiracaoDias;
    }

    public String gerarToken(Long profissionalId) {
        Instant agora = Instant.now();
        return Jwts.builder()
                .subject(profissionalId.toString())
                .issuedAt(Date.from(agora))
                .expiration(Date.from(agora.plus(expiracaoDias, ChronoUnit.DAYS)))
                .signWith(chave)
                .compact();
    }

    /** Retorna o id do profissional (subject) se o token for válido, ou null caso contrário. */
    public Long validarEExtrairProfissionalId(String token) {
        try {
            String subject = Jwts.parser()
                    .verifyWith(chave)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getSubject();
            return Long.valueOf(subject);
        } catch (Exception e) {
            return null;
        }
    }
}
