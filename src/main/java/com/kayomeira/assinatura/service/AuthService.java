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
import java.util.UUID;

@Service
@Slf4j
public class AuthService {

    private static final int RESET_SENHA_VALIDADE_HORAS = 1;

    private final ProfissionalRepository profissionalRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmailService emailService;
    private final GoogleIdTokenVerifier googleIdTokenVerifier;

    public AuthService(
            ProfissionalRepository profissionalRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            EmailService emailService,
            @Value("${app.google-client-id:}") String googleClientId) {
        this.profissionalRepository = profissionalRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.emailService = emailService;
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
                .emailVerificado(false)
                .tokenVerificacaoEmail(UUID.randomUUID().toString())
                .build();

        Profissional salvo = profissionalRepository.save(profissional);
        emailService.enviarEmailVerificacao(salvo);
        log.info("Profissional registrado: id={}, email={}", salvo.getId(), salvo.getEmail());
        return AuthResponseDTO.de(jwtService.gerarToken(salvo.getId()), salvo);
    }

    /**
     * Confirma o email a partir do link enviado no cadastro. Devolve um
     * token novo para já deixar a pessoa logada depois de confirmar.
     */
    @Transactional
    public AuthResponseDTO verificarEmail(String token) {
        Profissional profissional = profissionalRepository.findByTokenVerificacaoEmail(token)
                .orElseThrow(() -> new BadCredentialsException("Link de verificação inválido ou já usado"));

        profissional.setEmailVerificado(true);
        profissional.setTokenVerificacaoEmail(null);
        Profissional salvo = profissionalRepository.save(profissional);

        return AuthResponseDTO.de(jwtService.gerarToken(salvo.getId()), salvo);
    }

    /**
     * Reenvia o email de verificação. Não revela se o email existe ou já
     * está verificado — quem chama sempre recebe a mesma resposta genérica.
     */
    @Transactional
    public void reenviarVerificacao(String email) {
        profissionalRepository.findByEmail(email)
                .filter(p -> !p.isEmailVerificado())
                .ifPresent(profissional -> {
                    profissional.setTokenVerificacaoEmail(UUID.randomUUID().toString());
                    profissionalRepository.save(profissional);
                    emailService.enviarEmailVerificacao(profissional);
                });
    }

    /**
     * Início da recuperação de senha. Resposta sempre genérica (não revela
     * se o email existe nem se a conta usa senha ou só login Google) —
     * evita que alguém use este endpoint pra descobrir contas cadastradas.
     */
    @Transactional
    public void esqueciSenha(String email) {
        profissionalRepository.findByEmail(email)
                .filter(p -> p.getSenhaHash() != null)
                .ifPresent(profissional -> {
                    profissional.setTokenResetSenha(UUID.randomUUID().toString());
                    profissional.setTokenResetExpiracao(LocalDateTime.now().plusHours(RESET_SENHA_VALIDADE_HORAS));
                    profissionalRepository.save(profissional);
                    emailService.enviarEmailResetSenha(profissional);
                });
    }

    @Transactional
    public void redefinirSenha(String token, String novaSenha) {
        Profissional profissional = profissionalRepository.findByTokenResetSenha(token)
                .orElseThrow(() -> new BadCredentialsException("Link de redefinição inválido ou expirado"));

        if (profissional.getTokenResetExpiracao() == null
                || profissional.getTokenResetExpiracao().isBefore(LocalDateTime.now())) {
            throw new BadCredentialsException("Link de redefinição inválido ou expirado");
        }

        profissional.setSenhaHash(passwordEncoder.encode(novaSenha));
        profissional.setTokenResetSenha(null);
        profissional.setTokenResetExpiracao(null);
        profissionalRepository.save(profissional);
        log.info("Senha redefinida: id={}", profissional.getId());
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
        // O Google já confirmou a posse do email — não precisa do fluxo de verificação por link.
        profissional.setEmailVerificado(true);
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
