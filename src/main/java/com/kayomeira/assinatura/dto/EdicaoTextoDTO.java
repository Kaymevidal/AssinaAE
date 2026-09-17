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

    /** "serif" | "monospace" | "sans" (ou nulo) — detectado no frontend a partir do textContent do pdf.js. */
    private String familiaFonte;

    private Boolean negrito;

    private Boolean italico;

    /** Cor do texto (0-255), amostrada do PDF renderizado. Nula usa preto. */
    private Integer corR;
    private Integer corG;
    private Integer corB;

    /** Cor de fundo pra cobrir o texto original (0-255), amostrada do PDF renderizado. Nula usa branco. */
    private Integer fundoR;
    private Integer fundoG;
    private Integer fundoB;
}
