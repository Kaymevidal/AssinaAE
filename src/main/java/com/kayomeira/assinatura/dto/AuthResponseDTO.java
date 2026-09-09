package com.kayomeira.assinatura.dto;

import com.kayomeira.assinatura.model.Profissional;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthResponseDTO {

    private String token;
    private Long id;
    private String nome;
    private String email;
    private boolean emailVerificado;

    public static AuthResponseDTO de(String token, Profissional profissional) {
        return AuthResponseDTO.builder()
                .token(token)
                .id(profissional.getId())
                .nome(profissional.getNome())
                .email(profissional.getEmail())
                .emailVerificado(profissional.isEmailVerificado())
                .build();
    }
}
