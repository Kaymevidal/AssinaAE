package com.kayomeira.assinatura.controller;

import com.kayomeira.assinatura.dto.AuthResponseDTO;
import com.kayomeira.assinatura.dto.EmailRequestDTO;
import com.kayomeira.assinatura.dto.GoogleLoginRequestDTO;
import com.kayomeira.assinatura.dto.LoginRequestDTO;
import com.kayomeira.assinatura.dto.RedefinirSenhaRequestDTO;
import com.kayomeira.assinatura.dto.RegistrarRequestDTO;
import com.kayomeira.assinatura.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/registrar")
    public ResponseEntity<AuthResponseDTO> registrar(@Valid @RequestBody RegistrarRequestDTO dto) {
        return ResponseEntity.ok(authService.registrar(dto));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody LoginRequestDTO dto) {
        return ResponseEntity.ok(authService.login(dto));
    }

    @PostMapping("/google")
    public ResponseEntity<AuthResponseDTO> loginGoogle(@Valid @RequestBody GoogleLoginRequestDTO dto) {
        return ResponseEntity.ok(authService.loginGoogle(dto));
    }

    @GetMapping("/verificar-email/{token}")
    public ResponseEntity<AuthResponseDTO> verificarEmail(@PathVariable String token) {
        return ResponseEntity.ok(authService.verificarEmail(token));
    }

    @PostMapping("/reenviar-verificacao")
    public ResponseEntity<Void> reenviarVerificacao(@Valid @RequestBody EmailRequestDTO dto) {
        authService.reenviarVerificacao(dto.getEmail());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/esqueci-senha")
    public ResponseEntity<Void> esqueciSenha(@Valid @RequestBody EmailRequestDTO dto) {
        authService.esqueciSenha(dto.getEmail());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/redefinir-senha")
    public ResponseEntity<Void> redefinirSenha(@Valid @RequestBody RedefinirSenhaRequestDTO dto) {
        authService.redefinirSenha(dto.getToken(), dto.getNovaSenha());
        return ResponseEntity.ok().build();
    }
}
