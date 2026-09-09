package com.kayomeira.assinatura.dto;

import com.kayomeira.assinatura.model.Contrato;
import com.kayomeira.assinatura.model.StatusAssinatura;
import lombok.Builder;
import lombok.Data;

/**
 * View exposta na página pública de assinatura (via token). Não inclui o
 * token da outra parte nem os bytes do PDF.
 */
@Data
@Builder
public class ContratoAssinaturaDTO {

    private Long id;
    private String titulo;
    private String descricao;
    private String nomeProfissional;
    private String nomeCliente;
    private String papel;
    private StatusAssinatura statusPapel;
    private boolean ambosAssinaram;

    public static ContratoAssinaturaDTO fromEntity(Contrato contrato, String papel) {
        boolean isProfissional = "PROFISSIONAL".equals(papel);
        return ContratoAssinaturaDTO.builder()
                .id(contrato.getId())
                .titulo(contrato.getTitulo())
                .descricao(contrato.getDescricao())
                .nomeProfissional(contrato.getNomeProfissional())
                .nomeCliente(contrato.getNomeCliente())
                .papel(papel)
                .statusPapel(isProfissional ? contrato.getStatusProfissional() : contrato.getStatusCliente())
                .ambosAssinaram(contrato.ambosAssinaram())
                .build();
    }
}
