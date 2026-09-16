package com.kayomeira.assinatura.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

/** Uma edição de texto no editor de PDF: cobre o texto original na posição indicada e escreve o texto novo por cima. */
@Data
public class EdicaoTextoDTO {

    @NotNull
    @PositiveOrZero
    private Integer pagina;

    /** Canto superior esquerdo do texto original, em fração da largura da página (0 a 1). */
    @NotNull
    @DecimalMin("0.0")
    @DecimalMax("1.0")
    private Float x;

    /** Canto superior esquerdo do texto original, em fração da altura da página (0 a 1), medida a partir do topo. */
    @NotNull
    @DecimalMin("0.0")
    @DecimalMax("1.0")
    private Float y;

    @NotNull
    @DecimalMin("0.001")
    @DecimalMax("1.0")
    private Float largura;

    @NotNull
    @DecimalMin("0.001")
    @DecimalMax("1.0")
    private Float altura;

    /** Texto novo — pode ser vazio, pra só apagar o texto original. */
    private String texto;
}
