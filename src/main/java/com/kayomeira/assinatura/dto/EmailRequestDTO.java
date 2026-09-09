package com.kayomeira.assinatura.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** Usado por reenviar-verificação e esqueci-senha — ambos só precisam do email. */
@Data
public class EmailRequestDTO {

    @NotBlank
    @Email
    private String email;
}
