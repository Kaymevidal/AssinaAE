package com.kayomeira.assinatura.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profissional_id", nullable = false)
    private Profissional profissional;

    @Column(nullable = false)
    private String emailCliente;

    @Column(nullable = false)
    private String nomeCliente;
    
    // Sem @Lob de propósito: em Postgres, @Lob byte[] pode mapear para OID
    // (large object, exige lo_* e não bate com a coluna BYTEA da migration
    // do Flyway) e em H2 a combinação com o modo Postgres já se provou
    // instável (convertia pra INTEGER). VARBINARY explícito é o mesmo em
    // ambos: bytea no Postgres, binário de verdade no H2.
    @JdbcTypeCode(SqlTypes.VARBINARY)
    @Column(nullable = false)
    private byte[] pdfOriginal;

    @JdbcTypeCode(SqlTypes.VARBINARY)
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

    private LocalDateTime dataVisualizacaoProfissional;

    private LocalDateTime dataVisualizacaoCliente;
    
    @Transient
    private String assinaturaProfissionalBase64;
    
    @Transient
    private String assinaturaClienteBase64;
    
    public boolean ambosAssinaram() {
        return statusProfissional == StatusAssinatura.ASSINADO && 
               statusCliente == StatusAssinatura.ASSINADO;
    }
}
