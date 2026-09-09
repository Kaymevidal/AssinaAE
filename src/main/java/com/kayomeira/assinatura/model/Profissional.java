package com.kayomeira.assinatura.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "profissionais")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Profissional {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false, unique = true)
    private String email;

    /** Null quando a conta só tem login via Google. */
    private String senhaHash;

    /** Null quando a conta nunca fez login via Google. */
    @Column(unique = true)
    private String googleId;

    @Column(nullable = false, updatable = false)
    private LocalDateTime dataCriacao = LocalDateTime.now();

    /** Contas via Google já nascem verificadas (o Google confirma o email); via senha, precisa confirmar. */
    @Column(nullable = false)
    @Builder.Default
    private boolean emailVerificado = false;

    @Column(unique = true)
    private String tokenVerificacaoEmail;

    @Column(unique = true)
    private String tokenResetSenha;

    private LocalDateTime tokenResetExpiracao;
}
