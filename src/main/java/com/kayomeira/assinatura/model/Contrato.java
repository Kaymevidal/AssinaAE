package com.kayomeira.assinatura.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "contratos")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Contrato {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String titulo;
    
    @Column(nullable = false)
    private String descricao;
    
    @Column(nullable = false)
    private String emailProfissional;
    
    @Column(nullable = false)
    private String emailCliente;
    
    @Column(nullable = false)
    private String nomeProfissional;
    
    @Column(nullable = false)
    private String nomeCliente;
    
    @Lob
    @Column(nullable = false)
    private byte[] pdfOriginal;
    
    @Lob
    private byte[] pdfAssinado;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusAssinatura statusProfissional = StatusAssinatura.PENDENTE;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusAssinatura statusCliente = StatusAssinatura.PENDENTE;
    
    @Column(unique = true)
    private String tokenProfissional;
    
    @Column(unique = true)
    private String tokenCliente;
    
    @Column(nullable = false, updatable = false)
    private LocalDateTime dataCriacao = LocalDateTime.now();
    
    private LocalDateTime dataAssinaturaProfissional;
    
    private LocalDateTime dataAssinaturaCliente;
    
    @Transient
    private String assinaturaProfissionalBase64;
    
    @Transient
    private String assinaturaClienteBase64;
    
    public boolean ambosAssinaram() {
        return statusProfissional == StatusAssinatura.ASSINADO && 
               statusCliente == StatusAssinatura.ASSINADO;
    }
}
