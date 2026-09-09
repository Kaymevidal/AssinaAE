package com.kayomeira.assinatura.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.kayomeira.assinatura.dto.AuthResponseDTO;
import com.kayomeira.assinatura.dto.GoogleLoginRequestDTO;
import com.kayomeira.assinatura.dto.LoginRequestDTO;
import com.kayomeira.assinatura.dto.RegistrarRequestDTO;
import com.kayomeira.assinatura.exception.EmailJaCadastradoException;
import com.kayomeira.assinatura.model.Profissional;
import com.kayomeira.assinatura.repository.ProfissionalRepository;
import com.kayomeira.assinatura.security.JwtService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class AuthService {

    private final ProfissionalRepository profissionalRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final GoogleIdTokenVerifier googleIdTokenVerifier;

    public AuthService(
            ProfissionalRepository profissionalRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            @Value("${app.google-client-id:}") String googleClientId) {
        this.profissionalRepository = profissionalRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.googleIdTokenVerifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance())
                .setAudience(List.of(googleClientId))
                .build();
    }

    @Transactional
    public AuthResponseDTO registrar(RegistrarRequestDTO dto) {
        if (profissionalRepository.findByEmail(dto.getEmail()).isPresent()) {
            throw new EmailJaCadastradoException("Já existe uma conta com este email");
        }

        Profissional profissional = Profissional.builder()
                .nome(dto.getNome())
                .email(dto.getEmail())
                .senhaHash(passwordEncoder.encode(dto.getSenha()))
                .dataCriacao(LocalDateTime.now())
                .build();

        Profissional salvo = profissionalRepository.save(profissional);
        log.info("Profissional registrado: id={}, email={}", salvo.getId(), salvo.getEmail());
        return AuthResponseDTO.de(jwtService.gerarToken(salvo.getId()), salvo);
    }

    @Transactional(readOnly = true)
    public AuthResponseDTO login(LoginRequestDTO dto) {
        Profissional profissional = profissionalRepository.findByEmail(dto.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Email ou senha inválidos"));

        if (profissional.getSenhaHash() == null || !passwordEncoder.matches(dto.getSenha(), profissional.getSenhaHash())) {
            throw new BadCredentialsException("Email ou senha inválidos");
        }

        return AuthResponseDTO.de(jwtService.gerarToken(profissional.getId()), profissional);
    }

    @Transactional
    public AuthResponseDTO loginGoogle(GoogleLoginRequestDTO dto) {
        GoogleIdToken.Payload payload = verificarIdToken(dto.getCredential());

        String googleId = payload.getSubject();
        String email = payload.getEmail();
        String nome = String.valueOf(payload.get("name"));

        Profissional profissional = profissionalRepository.findByGoogleId(googleId)
                .or(() -> profissionalRepository.findByEmail(email))
                .orElseGet(() -> Profissional.builder()
                        .nome(nome)
                        .email(email)
                        .dataCriacao(LocalDateTime.now())
                        .build());

        profissional.setGoogleId(googleId);
        Profissional salvo = profissionalRepository.save(profissional);

        return AuthResponseDTO.de(jwtService.gerarToken(salvo.getId()), salvo);
    }

    private GoogleIdToken.Payload verificarIdToken(String credential) {
        try {
            GoogleIdToken idToken = googleIdTokenVerifier.verify(credential);
            if (idToken == null) {
                throw new BadCredentialsException("Token do Google inválido ou expirado");
            }
            return idToken.getPayload();
        } catch (GeneralSecurityException | java.io.IOException | IllegalArgumentException e) {
            // IllegalArgumentException cobre um token mal formado (não é nem um JWT válido),
            // que o verify() do Google não trata como falha de segurança/IO.
            log.error("Erro ao verificar token do Google", e);
            throw new BadCredentialsException("Não foi possível verificar o login com Google");
        }
    }
}
