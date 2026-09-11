package com.kayomeira.assinatura.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

@Data
public class AssinaturaRequestDTO {

    /** Imagem da assinatura (PNG) em Base64, sem o prefixo "data:image/png;base64,". */
    @NotBlank
    private String assinaturaBase64;

    /** Página do PDF (0-indexada) onde a assinatura deve ser desenhada. */
    @NotNull
    @PositiveOrZero
    private Integer pagina;

    /** Posição horizontal do canto superior esquerdo da assinatura, em fração da largura da página (0 a 1). */
    @NotNull
    @DecimalMin("0.0")
    @DecimalMax("1.0")
    private Float x;

    /** Posição vertical do canto superior esquerdo da assinatura, em fração da altura da página (0 a 1), medida a partir do topo. */
    @NotNull
    @DecimalMin("0.0")
    @DecimalMax("1.0")
    private Float y;

    /** Largura da assinatura, em fração da largura da página (0 a 1). */
    @NotNull
    @DecimalMin("0.001")
    @DecimalMax("1.0")
    private Float largura;

    /** Altura da assinatura, em fração da altura da página (0 a 1). */
    @NotNull
    @DecimalMin("0.001")
    @DecimalMax("1.0")
    private Float altura;
}
