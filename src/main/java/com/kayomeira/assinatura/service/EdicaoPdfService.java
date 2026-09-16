package com.kayomeira.assinatura.service;

import com.kayomeira.assinatura.dto.EdicaoTextoDTO;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * Editor de texto do PDF: cobre a posição original (branco por cima) e escreve
 * o texto novo ali, na mesma caixa — não é reflow de texto de verdade (PDF não
 * permite isso de forma geral), é a técnica que editores de PDF usam na prática.
 * Coordenadas em fração da página (0 a 1), mesmo padrão do AssinaturaPDFService.
 *
 * Cada edição é isolada e nunca derruba a requisição inteira: dado inválido ou
 * caractere que a fonte não sabe desenhar faz só aquela edição ser ignorada
 * (com log), o restante do documento e das outras edições segue normalmente —
 * o endpoint sempre devolve um PDF válido.
 */
@Service
@Slf4j
public class EdicaoPdfService {

    private static final float FRACAO_FONTE_NA_CAIXA = 0.8f;

    public byte[] aplicarEdicoes(byte[] pdfBytes, List<EdicaoTextoDTO> edicoes) throws IOException {
        PDDocument document = Loader.loadPDF(pdfBytes);
        try {
            if (edicoes != null && document.getNumberOfPages() > 0) {
                PDType1Font fonte = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
                for (EdicaoTextoDTO edicao : edicoes) {
                    aplicarUmaEdicao(document, fonte, edicao);
                }
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            document.save(outputStream);
            return outputStream.toByteArray();
        } finally {
            document.close();
        }
    }

    private void aplicarUmaEdicao(PDDocument document, PDType1Font fonte, EdicaoTextoDTO edicao) {
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

            try (PDPageContentStream contentStream = new PDPageContentStream(
                    document, page, PDPageContentStream.AppendMode.APPEND, true, true)) {

                contentStream.setNonStrokingColor(Color.WHITE);
                contentStream.addRect(posicaoX, posicaoY, larguraCaixa, alturaCaixa);
                contentStream.fill();

                String texto = sanitizarParaFonte(fonte, edicao.getTexto());
                if (texto != null && !texto.isBlank()) {
                    float tamanhoFonte = alturaCaixa * FRACAO_FONTE_NA_CAIXA;
                    contentStream.setNonStrokingColor(Color.BLACK);
                    contentStream.beginText();
                    contentStream.setFont(fonte, tamanhoFonte);
                    // baseline perto do fundo da caixa, com uma folga pequena pra descendentes (g, j, p...)
                    contentStream.newLineAtOffset(posicaoX, posicaoY + alturaCaixa * 0.08f);
                    contentStream.showText(texto);
                    contentStream.endText();
                }
            }
        } catch (Exception e) {
            log.warn("Edição de PDF ignorada por falha ao aplicar (página {}): {}", edicao.getPagina(), e.getMessage());
        }
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
