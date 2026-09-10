package com.kayomeira.assinatura.service;

import com.kayomeira.assinatura.dto.LoginRequestDTO;
import com.kayomeira.assinatura.dto.RegistrarRequestDTO;
import com.kayomeira.assinatura.exception.EmailJaCadastradoException;
import com.kayomeira.assinatura.model.Profissional;
import com.kayomeira.assinatura.repository.ProfissionalRepository;
import com.kayomeira.assinatura.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuthServiceTest {

    @Mock
    private ProfissionalRepository profissionalRepository;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final JwtService jwtService = new JwtService("segredo-de-teste-com-pelo-menos-32-caracteres-0123456789", 7);

    private AuthService authService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        authService = new AuthService(profissionalRepository, passwordEncoder, jwtService, new EmailService(), "");
        when(profissionalRepository.save(any(Profissional.class))).thenAnswer(inv -> {
            Profissional p = inv.getArgument(0);
            p.setId(1L);
            return p;
        });
    }

    @Test
    void registrarNuncaSalvaASenhaEmTextoPuro() {
        RegistrarRequestDTO dto = new RegistrarRequestDTO();
        dto.setNome("Ana");
        dto.setEmail("ana@teste.com");
        dto.setSenha("minhaSenhaSecreta123");

        when(profissionalRepository.findByEmail("ana@teste.com")).thenReturn(Optional.empty());

        authService.registrar(dto);

        ArgumentCaptor<Profissional> captor = ArgumentCaptor.forClass(Profissional.class);
        verify(profissionalRepository).save(captor.capture());
        Profissional salvo = captor.getValue();

        assertThat(salvo.getSenhaHash()).isNotEqualTo("minhaSenhaSecreta123");
        assertThat(salvo.getSenhaHash()).doesNotContain("minhaSenhaSecreta123");
        assertThat(salvo.getSenhaHash()).startsWith("$2"); // prefixo padrão de hash bcrypt
        assertThat(passwordEncoder.matches("minhaSenhaSecreta123", salvo.getSenhaHash())).isTrue();
    }

    @Test
    void registrarComEmailJaExistenteEhRejeitado() {
        RegistrarRequestDTO dto = new RegistrarRequestDTO();
        dto.setNome("Ana");
        dto.setEmail("ana@teste.com");
        dto.setSenha("qualquerSenha123");

        when(profissionalRepository.findByEmail("ana@teste.com"))
                .thenReturn(Optional.of(new Profissional()));

        assertThatThrownBy(() -> authService.registrar(dto))
                .isInstanceOf(EmailJaCadastradoException.class);
        verify(profissionalRepository, never()).save(any());
    }

    @Test
    void loginComSenhaErradaEhRejeitado() {
        Profissional existente = Profissional.builder()
                .id(1L).nome("Ana").email("ana@teste.com")
                .senhaHash(passwordEncoder.encode("senhaCorreta123"))
                .build();
        when(profissionalRepository.findByEmail("ana@teste.com")).thenReturn(Optional.of(existente));

        LoginRequestDTO dto = new LoginRequestDTO();
        dto.setEmail("ana@teste.com");
        dto.setSenha("senhaErrada456");

        assertThatThrownBy(() -> authService.login(dto)).isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void loginComEmailInexistenteEhRejeitadoComMensagemGenerica() {
        when(profissionalRepository.findByEmail("fantasma@teste.com")).thenReturn(Optional.empty());

        LoginRequestDTO dto = new LoginRequestDTO();
        dto.setEmail("fantasma@teste.com");
        dto.setSenha("qualquerCoisa123");

        // A mensagem não deve revelar se o problema foi o email ou a senha (evita enumeração de contas).
        assertThatThrownBy(() -> authService.login(dto))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Email ou senha inválidos");
    }

    @Test
    void loginComContaSoDeGoogleEhRejeitadoMesmoComSenhaQualquer() {
        Profissional contaSoGoogle = Profissional.builder()
                .id(1L).nome("Ana").email("ana@teste.com")
                .senhaHash(null)
                .googleId("google-123")
                .build();
        when(profissionalRepository.findByEmail("ana@teste.com")).thenReturn(Optional.of(contaSoGoogle));

        LoginRequestDTO dto = new LoginRequestDTO();
        dto.setEmail("ana@teste.com");
        dto.setSenha("qualquerCoisa123");

        assertThatThrownBy(() -> authService.login(dto)).isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void loginComCredenciaisCorretasDevolveTokenValido() {
        Profissional existente = Profissional.builder()
                .id(9L).nome("Ana").email("ana@teste.com")
                .senhaHash(passwordEncoder.encode("senhaCorreta123"))
                .build();
        when(profissionalRepository.findByEmail("ana@teste.com")).thenReturn(Optional.of(existente));

        LoginRequestDTO dto = new LoginRequestDTO();
        dto.setEmail("ana@teste.com");
        dto.setSenha("senhaCorreta123");

        var resposta = authService.login(dto);

        assertThat(resposta.getToken()).isNotBlank();
        assertThat(jwtService.validarEExtrairProfissionalId(resposta.getToken())).isEqualTo(9L);
    }
}
