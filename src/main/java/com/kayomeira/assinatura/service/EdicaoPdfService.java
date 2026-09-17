package com.kayomeira.assinatura.service;

import com.kayomeira.assinatura.dto.EdicaoTextoDTO;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.util.Matrix;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * Editor de texto do PDF: cobre a posição original (com a cor de fundo
 * detectada, ou branco) e escreve o texto novo ali, na mesma caixa — não é
 * reflow de texto de verdade (PDF não permite isso de forma geral), é a
 * técnica que editores de PDF usam na prática. Coordenadas em fração da
 * página (0 a 1), mesmo padrão do AssinaturaPDFService.
 *
 * Fonte, negrito/itálico e cores vêm de sinais detectados no frontend (a
 * partir do textContent e dos pixels renderizados do pdf.js) — aproximações,
 * já que PDF embute/recorta fontes que não mapeiam pra um nome de sistema.
 * A largura do texto novo é ajustada (esticada/comprimida) pra caber na
 * caixa original, dentro de um limite, pra não ficar distorcido.
 *
 * Cada edição é isolada e nunca derruba a requisição inteira: dado inválido
 * ou caractere que a fonte não sabe desenhar faz só aquela edição ser
 * ignorada (com log), o restante do documento e das outras edições segue
 * normalmente — o endpoint sempre devolve um PDF válido.
 */
@Service
@Slf4j
public class EdicaoPdfService {

    private static final float FRACAO_FONTE_NA_CAIXA = 0.8f;
    private static final float ESCALA_HORIZONTAL_MINIMA = 0.6f;
    private static final float ESCALA_HORIZONTAL_MAXIMA = 1.6f;

    public byte[] aplicarEdicoes(byte[] pdfBytes, List<EdicaoTextoDTO> edicoes) throws IOException {
        PDDocument document = Loader.loadPDF(pdfBytes);
        try {
            if (edicoes != null && document.getNumberOfPages() > 0) {
                for (EdicaoTextoDTO edicao : edicoes) {
                    aplicarUmaEdicao(document, edicao);
                }
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            document.save(outputStream);
            return outputStream.toByteArray();
        } finally {
            document.close();
        }
    }

    private void aplicarUmaEdicao(PDDocument document, EdicaoTextoDTO edicao) {
        try {
            if (!edicaoValida(edicao)) {
                log.warn("Edição de PDF ignorada: dados incompletos ({})", edicao);
                return;
            }

            int indicePagina = edicao.getPagina() >= 0 && edicao.getPagina() < document.getNumberOfPages()
                    ? edicao.getPagina()
                    : document.getNumberOfPages() - 1;
            PDPage page = document.getPage(indicePagina);
            PDRectangle mediaBox = page.getMediaBox();

            float larguraPagina = mediaBox.getWidth();
            float alturaPagina = mediaBox.getHeight();

            float larguraCaixa = edicao.getLargura() * larguraPagina;
            float alturaCaixa = edicao.getAltura() * alturaPagina;
            float posicaoX = clamp(edicao.getX() * larguraPagina, 0, larguraPagina - larguraCaixa);
            float posicaoY = clamp(
                    alturaPagina - (edicao.getY() * alturaPagina) - alturaCaixa,
                    0,
                    alturaPagina - alturaCaixa);

            PDType1Font fonte = escolherFonte(edicao);

            try (PDPageContentStream contentStream = new PDPageContentStream(
                    document, page, PDPageContentStream.AppendMode.APPEND, true, true)) {

                Color corFundo = corOuPadrao(edicao.getFundoR(), edicao.getFundoG(), edicao.getFundoB(), Color.WHITE);
                contentStream.setNonStrokingColor(corFundo);
                contentStream.addRect(posicaoX, posicaoY, larguraCaixa, alturaCaixa);
                contentStream.fill();

                String texto = sanitizarParaFonte(fonte, edicao.getTexto());
                if (texto != null && !texto.isBlank()) {
                    float tamanhoFonte = alturaCaixa * FRACAO_FONTE_NA_CAIXA;
                    float larguraNatural = fonte.getStringWidth(texto) / 1000f * tamanhoFonte;
                    // estica/comprime horizontalmente o texto novo pra caber na caixa original,
                    // dentro de um limite — fora dele é melhor deixar não caber perfeitamente do
                    // que distorcer a ponto de ficar ilegível
                    float escalaHorizontal = larguraNatural > 0
                            ? clamp(larguraCaixa / larguraNatural, ESCALA_HORIZONTAL_MINIMA, ESCALA_HORIZONTAL_MAXIMA)
                            : 1f;

                    Color corTexto = corOuPadrao(edicao.getCorR(), edicao.getCorG(), edicao.getCorB(), Color.BLACK);
                    contentStream.setNonStrokingColor(corTexto);
                    contentStream.beginText();
                    contentStream.setFont(fonte, tamanhoFonte);
                    // baseline perto do fundo da caixa, com uma folga pequena pra descendentes (g, j, p...)
                    contentStream.setTextMatrix(new Matrix(
                            escalaHorizontal, 0, 0, 1,
                            posicaoX, posicaoY + alturaCaixa * 0.08f));
                    contentStream.showText(texto);
                    contentStream.endText();
                }
            }
        } catch (Exception e) {
            log.warn("Edição de PDF ignorada por falha ao aplicar (página {}): {}", edicao.getPagina(), e.getMessage());
        }
    }

    /**
     * Mapeia os sinais detectados no frontend (família + negrito/itálico) pra
     * uma das 14 fontes padrão do PDF — não é a fonte original (PDF embute/
     * recorta fontes que não mapeiam pra um nome de sistema), é a mais
     * parecida disponível.
     */
    private PDType1Font escolherFonte(EdicaoTextoDTO edicao) {
        boolean negrito = Boolean.TRUE.equals(edicao.getNegrito());
        boolean italico = Boolean.TRUE.equals(edicao.getItalico());
        String familia = edicao.getFamiliaFonte();

        Standard14Fonts.FontName nome;
        if ("serif".equals(familia)) {
            nome = negrito && italico ? Standard14Fonts.FontName.TIMES_BOLD_ITALIC
                    : negrito ? Standard14Fonts.FontName.TIMES_BOLD
                    : italico ? Standard14Fonts.FontName.TIMES_ITALIC
                    : Standard14Fonts.FontName.TIMES_ROMAN;
        } else if ("monospace".equals(familia)) {
            nome = negrito && italico ? Standard14Fonts.FontName.COURIER_BOLD_OBLIQUE
                    : negrito ? Standard14Fonts.FontName.COURIER_BOLD
                    : italico ? Standard14Fonts.FontName.COURIER_OBLIQUE
                    : Standard14Fonts.FontName.COURIER;
        } else {
            nome = negrito && italico ? Standard14Fonts.FontName.HELVETICA_BOLD_OBLIQUE
                    : negrito ? Standard14Fonts.FontName.HELVETICA_BOLD
                    : italico ? Standard14Fonts.FontName.HELVETICA_OBLIQUE
                    : Standard14Fonts.FontName.HELVETICA;
        }
        return new PDType1Font(nome);
    }

    private Color corOuPadrao(Integer r, Integer g, Integer b, Color padrao) {
        if (r == null || g == null || b == null) return padrao;
        return new Color(clampCor(r), clampCor(g), clampCor(b));
    }

    private int clampCor(int valor) {
        return Math.max(0, Math.min(255, valor));
    }

    private boolean edicaoValida(EdicaoTextoDTO edicao) {
        return edicao.getPagina() != null && edicao.getPagina() >= 0
                && edicao.getX() != null && edicao.getY() != null
                && edicao.getLargura() != null && edicao.getLargura() > 0
                && edicao.getAltura() != null && edicao.getAltura() > 0;
    }

    /** Troca por "?" qualquer caractere que a fonte não sabe desenhar, em vez de deixar a edição inteira falhar. */
    private String sanitizarParaFonte(PDType1Font fonte, String texto) {
        if (texto == null) return null;
        StringBuilder resultado = new StringBuilder(texto.length());
        for (int i = 0; i < texto.length(); i++) {
            char c = texto.charAt(i);
            try {
                fonte.encode(String.valueOf(c));
                resultado.append(c);
            } catch (Exception e) {
                resultado.append('?');
            }
        }
        return resultado.toString();
    }

    private static float clamp(float valor, float min, float max) {
        if (max < min) return min;
        return Math.max(min, Math.min(max, valor));
    }
}
