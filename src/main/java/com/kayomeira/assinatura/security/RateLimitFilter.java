package com.kayomeira.assinatura.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Limite simples de tentativas por IP nas rotas de login/registro, contra
 * força bruta. Em memória, por instância — não sobrevive a múltiplas
 * réplicas do backend sem um store compartilhado (Redis), o que não é o
 * caso deste deploy.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Set<String> ROTAS_LIMITADAS = Set.of("/api/auth/login", "/api/auth/registrar");
    private static final int MAX_TENTATIVAS = 10;
    private static final Duration JANELA = Duration.ofMinutes(5);

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final ConcurrentHashMap<String, Janela> tentativasPorChave = new ConcurrentHashMap<>();

    private static class Janela {
        final AtomicInteger contagem = new AtomicInteger(0);
        volatile Instant inicio = Instant.now();
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        if (!ROTAS_LIMITADAS.contains(request.getRequestURI())) {
            filterChain.doFilter(request, response);
            return;
        }

        String chave = obterIp(request) + ":" + request.getRequestURI();
        Janela janela = tentativasPorChave.computeIfAbsent(chave, k -> new Janela());

        boolean excedeu;
        synchronized (janela) {
            if (Duration.between(janela.inicio, Instant.now()).compareTo(JANELA) > 0) {
                janela.inicio = Instant.now();
                janela.contagem.set(0);
            }
            excedeu = janela.contagem.incrementAndGet() > MAX_TENTATIVAS;
        }

        if (excedeu) {
            responderMuitasTentativas(response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void responderMuitasTentativas(HttpServletResponse response) throws IOException {
        response.setStatus(429);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", 429);
        body.put("erro", "Muitas tentativas. Tente novamente em alguns minutos.");

        objectMapper.writeValue(response.getOutputStream(), body);
    }

    private String obterIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
