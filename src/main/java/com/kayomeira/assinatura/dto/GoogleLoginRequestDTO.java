package com.kayomeira.assinatura.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class GoogleLoginRequestDTO {

    /** ID token (JWT) devolvido pelo Google Identity Services no frontend. */
    @NotBlank
    private String credential;
}
