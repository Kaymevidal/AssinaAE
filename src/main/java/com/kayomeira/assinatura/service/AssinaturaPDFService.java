package com.kayomeira.assinatura.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
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
     * Adiciona assinatura (imagem PNG) ao PDF
     * @param pdfBytes PDF original em bytes
     * @param assinaturaBase64 Imagem da assinatura em Base64 (PNG)
     * @param posicaoX Posição X (em pixels)
     * @param posicaoY Posição Y (em pixels)
     * @param nomeSignatario Nome de quem está assinando
     * @return PDF com assinatura adicionada em bytes
     */
    public byte[] adicionarAssinatura(
            byte[] pdfBytes,
            String assinaturaBase64,
            float posicaoX,
            float posicaoY,
            String nomeSignatario) throws IOException {
        
        PDDocument document = Loader.loadPDF(pdfBytes);

        try {
            // Converte Base64 para bytes
            byte[] assinaturaBytes = Base64.decodeBase64(assinaturaBase64);
            
            // Pega última página do documento
            PDPage page = document.getPage(document.getNumberOfPages() - 1);
            
            // Cria stream de conteúdo para desenhar
            try (PDPageContentStream contentStream = new PDPageContentStream(
                    document, page, PDPageContentStream.AppendMode.APPEND, true, true)) {
                
                // Cria imagem a partir dos bytes
                PDImageXObject image = PDImageXObject.createFromByteArray(
                        document, assinaturaBytes, "signature");
                
                // Dimensões da assinatura (ajuste conforme necessário)
                float width = 100;
                float height = 60;
                
                // Desenha a imagem da assinatura no PDF
                contentStream.drawImage(image, posicaoX, posicaoY, width, height);
                
                // Adiciona texto com data/hora e nome
                contentStream.setFont(
                        new org.apache.pdfbox.pdmodel.font.PDType1Font(
                                org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName.HELVETICA),
                        8);
                contentStream.setLeading(10);
                
                String dataAssinatura = LocalDateTime.now()
                        .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
                String textoAssinatura = "Assinado por: " + nomeSignatario + " em " + dataAssinatura;
                
                contentStream.beginText();
                contentStream.newLineAtOffset(posicaoX, posicaoY - 15);
                contentStream.showText(textoAssinatura);
                contentStream.endText();
            }
            
            // Escreve PDF em ByteArray
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            document.save(outputStream);
            
            log.info("Assinatura adicionada ao PDF para: {}", nomeSignatario);
            return outputStream.toByteArray();
            
        } finally {
            document.close();
        }
    }
    
    /**
     * Adiciona duas assinaturas ao PDF (profissional e cliente)
     */
    public byte[] adicionarDuasAssinaturas(
            byte[] pdfBytes,
            String assinaturaProfissionalBase64,
            String assinaturaClienteBase64,
            String nomeProfissional,
            String nomeCliente) throws IOException {
        
        // Primeira assinatura (profissional) - lado esquerdo
        byte[] pdfComAssinatura1 = adicionarAssinatura(
                pdfBytes,
                assinaturaProfissionalBase64,
                50, 300,
                nomeProfissional);
        
        // Segunda assinatura (cliente) - lado direito
        byte[] pdfComAmbasAssinaturas = adicionarAssinatura(
                pdfComAssinatura1,
                assinaturaClienteBase64,
                350, 300,
                nomeCliente);
        
        return pdfComAmbasAssinaturas;
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
