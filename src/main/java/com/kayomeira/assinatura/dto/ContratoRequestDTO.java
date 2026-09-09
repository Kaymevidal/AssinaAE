package com.kayomeira.assinatura.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ContratoRequestDTO {

    @NotBlank
    private String titulo;

    @NotBlank
    private String descricao;

    @NotBlank
    @Email
    private String emailProfissional;

    @NotBlank
    @Email
    private String emailCliente;

    @NotBlank
    private String nomeProfissional;

    @NotBlank
    private String nomeCliente;
}
