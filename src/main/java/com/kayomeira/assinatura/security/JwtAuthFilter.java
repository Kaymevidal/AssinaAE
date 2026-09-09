package com.kayomeira.assinatura.security;

import com.kayomeira.assinatura.model.Profissional;
import com.kayomeira.assinatura.repository.ProfissionalRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final ProfissionalRepository profissionalRepository;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            Long profissionalId = jwtService.validarEExtrairProfissionalId(header.substring(7));

            if (profissionalId != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                profissionalRepository.findById(profissionalId).ifPresent(this::autenticar);
            }
        }

        filterChain.doFilter(request, response);
    }

    private void autenticar(Profissional profissional) {
        var authentication = new UsernamePasswordAuthenticationToken(profissional, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
