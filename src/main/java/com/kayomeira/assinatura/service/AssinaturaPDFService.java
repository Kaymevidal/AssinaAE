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

    /**
     * Adiciona assinatura (imagem PNG) ao PDF, na página e posição escolhidas
     * por quem assina. Posição e tamanho chegam como frações (0 a 1) da
     * página — independentes de resolução/zoom — e são convertidas aqui para
     * pontos usando o MediaBox real da página, o que funciona para A4,
     * Letter ou qualquer tamanho customizado.
     *
     * @param pdfBytes PDF original em bytes
     * @param assinaturaBase64 Imagem da assinatura em Base64 (PNG)
     * @param pagina Página onde a assinatura é desenhada (0-indexada)
     * @param x Posição horizontal do canto superior esquerdo, fração da largura da página (0 a 1)
     * @param y Posição vertical do canto superior esquerdo, fração da altura da página a partir do topo (0 a 1)
     * @param largura Largura da assinatura, fração da largura da página (0 a 1)
     * @param altura Altura da assinatura, fração da altura da página (0 a 1)
     * @param nomeSignatario Nome de quem está assinando
     * @return PDF com assinatura adicionada em bytes
     */
    public byte[] adicionarAssinatura(
            byte[] pdfBytes,
            String assinaturaBase64,
            int pagina,
            float x,
            float y,
            float largura,
            float altura,
            String nomeSignatario) throws IOException {

        PDDocument document = Loader.loadPDF(pdfBytes);

        try {
            byte[] assinaturaBytes = Base64.decodeBase64(assinaturaBase64);
            // Página escolhida no cliente pode não bater mais (ex.: PDF trocado) — cai pra última em vez de estourar índice.
            int indicePagina = pagina >= 0 && pagina < document.getNumberOfPages()
                    ? pagina
                    : document.getNumberOfPages() - 1;
            PDPage page = document.getPage(indicePagina);
            PDRectangle mediaBox = page.getMediaBox();

            float larguraPagina = mediaBox.getWidth();
            float alturaPagina = mediaBox.getHeight();

            float larguraAssinatura = largura * larguraPagina;
            float alturaAssinatura = altura * alturaPagina;

            // x/y chegam como fração a partir do canto superior esquerdo (convenção de tela);
            // PDF usa origem no canto inferior esquerdo, daí a inversão do eixo Y.
            float posicaoX = clamp(x * larguraPagina, 0, larguraPagina - larguraAssinatura);
            float posicaoY = clamp(
                    alturaPagina - (y * alturaPagina) - alturaAssinatura,
                    0,
                    alturaPagina - alturaAssinatura);

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

                // Texto fica logo abaixo da assinatura; se não houver espaço (assinatura colada no rodapé), vai por cima.
                float posicaoTextoY = posicaoY - 12 >= 0 ? posicaoY - 12 : posicaoY + alturaAssinatura + 2;

                contentStream.beginText();
                contentStream.newLineAtOffset(posicaoX, posicaoTextoY);
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

    private static float clamp(float valor, float min, float max) {
        if (max < min) return min;
        return Math.max(min, Math.min(max, valor));
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
