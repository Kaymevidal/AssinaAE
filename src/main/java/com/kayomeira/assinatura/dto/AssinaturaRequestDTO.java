package com.kayomeira.assinatura.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AssinaturaRequestDTO {

    /** Imagem da assinatura (PNG) em Base64, sem o prefixo "data:image/png;base64,". */
    @NotBlank
    private String assinaturaBase64;
}
