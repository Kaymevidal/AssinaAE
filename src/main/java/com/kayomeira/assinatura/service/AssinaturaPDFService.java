package com.kayomeira.assinatura.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.commons.codec.binary.Base64;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@Slf4j
public class AssinaturaPDFService {

    /** Em qual metade da última página a assinatura é desenhada. */
    public enum PosicaoAssinatura { ESQUERDA, DIREITA }

    /**
     * Adiciona assinatura (imagem PNG) ao PDF. A posição e o tamanho são
     * calculados a partir do MediaBox real da última página — funciona para
     * A4, Letter ou qualquer tamanho customizado, em vez de coordenadas
     * fixas que só faziam sentido para um tamanho de página específico.
     *
     * @param pdfBytes PDF original em bytes
     * @param assinaturaBase64 Imagem da assinatura em Base64 (PNG)
     * @param posicao Lado da página onde a assinatura é desenhada
     * @param nomeSignatario Nome de quem está assinando
     * @return PDF com assinatura adicionada em bytes
     */
    public byte[] adicionarAssinatura(
            byte[] pdfBytes,
            String assinaturaBase64,
            PosicaoAssinatura posicao,
            String nomeSignatario) throws IOException {

        PDDocument document = Loader.loadPDF(pdfBytes);

        try {
            byte[] assinaturaBytes = Base64.decodeBase64(assinaturaBase64);
            PDPage page = document.getPage(document.getNumberOfPages() - 1);
            PDRectangle mediaBox = page.getMediaBox();

            float larguraPagina = mediaBox.getWidth();
            float alturaPagina = mediaBox.getHeight();

            float larguraAssinatura = Math.min(larguraPagina * 0.28f, 180);
            float alturaAssinatura = larguraAssinatura * 0.5f;
            float posicaoY = alturaPagina * 0.08f;
            float posicaoX = posicao == PosicaoAssinatura.ESQUERDA
                    ? larguraPagina * 0.08f
                    : larguraPagina - larguraAssinatura - (larguraPagina * 0.08f);

            try (PDPageContentStream contentStream = new PDPageContentStream(
                    document, page, PDPageContentStream.AppendMode.APPEND, true, true)) {

                PDImageXObject image = PDImageXObject.createFromByteArray(
                        document, assinaturaBytes, "signature");

                contentStream.drawImage(image, posicaoX, posicaoY, larguraAssinatura, alturaAssinatura);

                contentStream.setFont(
                        new org.apache.pdfbox.pdmodel.font.PDType1Font(
                                org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName.HELVETICA),
                        8);
                contentStream.setLeading(10);

                String dataAssinatura = LocalDateTime.now()
                        .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
                String textoAssinatura = "Assinado por: " + nomeSignatario + " em " + dataAssinatura;

                contentStream.beginText();
                contentStream.newLineAtOffset(posicaoX, posicaoY - 12);
                contentStream.showText(textoAssinatura);
                contentStream.endText();
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            document.save(outputStream);

            log.info("Assinatura adicionada ao PDF para: {}", nomeSignatario);
            return outputStream.toByteArray();

        } finally {
            document.close();
        }
    }

    /**
     * Valida se é um PDF válido
     */
    public boolean validarPDF(byte[] pdfBytes) {
        try {
            Loader.loadPDF(pdfBytes).close();
            return true;
        } catch (Exception e) {
            log.error("PDF inválido: {}", e.getMessage());
            return false;
        }
    }
}
