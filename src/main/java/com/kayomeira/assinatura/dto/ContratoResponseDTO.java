package com.kayomeira.assinatura.dto;

import com.kayomeira.assinatura.model.Contrato;
import com.kayomeira.assinatura.model.StatusAssinatura;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ContratoResponseDTO {

    private Long id;
    private String titulo;
    private String descricao;
    private String nomeProfissional;
    private String nomeCliente;
    private StatusAssinatura statusProfissional;
    private StatusAssinatura statusCliente;
    private boolean ambosAssinaram;
    private LocalDateTime dataCriacao;
    private LocalDateTime dataAssinaturaProfissional;
    private LocalDateTime dataAssinaturaCliente;

    public static ContratoResponseDTO fromEntity(Contrato contrato) {
        return ContratoResponseDTO.builder()
                .id(contrato.getId())
                .titulo(contrato.getTitulo())
                .descricao(contrato.getDescricao())
                .nomeProfissional(contrato.getNomeProfissional())
                .nomeCliente(contrato.getNomeCliente())
                .statusProfissional(contrato.getStatusProfissional())
                .statusCliente(contrato.getStatusCliente())
                .ambosAssinaram(contrato.ambosAssinaram())
                .dataCriacao(contrato.getDataCriacao())
                .dataAssinaturaProfissional(contrato.getDataAssinaturaProfissional())
                .dataAssinaturaCliente(contrato.getDataAssinaturaCliente())
                .build();
    }
}
