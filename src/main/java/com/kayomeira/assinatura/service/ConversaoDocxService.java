package com.kayomeira.assinatura.service;

import com.kayomeira.assinatura.exception.ConversaoDocumentoException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

// DOCX -> PDF usa LibreOffice (`soffice`); PDF -> DOCX usa pdf2docx (Python),
// não o LibreOffice — ele importa PDF como desenho (Draw) e não tem filtro de
// exportação pra Writer.
@Service
@Slf4j
public class ConversaoDocxService {

    private static final long TIMEOUT_SEGUNDOS = 30;

    @Value("${app.libreoffice.path:soffice}")
    private String sofficePath;

    @Value("${app.pdf2docx.path:pdf2docx}")
    private String pdf2docxPath;

    public byte[] paraPdf(byte[] docxBytes) throws IOException {
        Path diretorio = Files.createTempDirectory("conversao-");
        try {
            Path entrada = diretorio.resolve("entrada.docx");
            Files.write(entrada, docxBytes);

            // perfil isolado por chamada, senão conversões concorrentes disputam o mesmo
            String perfilIsolado = "file:///" + diretorio.toString().replace('\\', '/') + "/loperfil";
            executar(sofficePath, "--headless", "--convert-to", "pdf",
                    "--outdir", diretorio.toString(),
                    "-env:UserInstallation=" + perfilIsolado,
                    entrada.toString());

            return lerSaida(diretorio.resolve("entrada.pdf"));
        } finally {
            apagarRecursivo(diretorio);
        }
    }

    public byte[] paraDocx(byte[] pdfBytes) throws IOException {
        Path diretorio = Files.createTempDirectory("conversao-");
        try {
            Path entrada = diretorio.resolve("entrada.pdf");
            Path saida = diretorio.resolve("saida.docx");
            Files.write(entrada, pdfBytes);

            executar(pdf2docxPath, "convert", entrada.toString(), saida.toString());

            return lerSaida(saida);
        } finally {
            apagarRecursivo(diretorio);
        }
    }

    private void executar(String... comando) throws IOException {
        ProcessBuilder processBuilder = new ProcessBuilder(comando);
        processBuilder.redirectErrorStream(true);

        Process processo = processBuilder.start();
        String saidaProcesso = new String(processo.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

        boolean terminou = aguardar(processo);
        if (!terminou) {
            processo.destroyForcibly();
            throw new ConversaoDocumentoException("A conversão demorou demais e foi cancelada");
        }
        if (processo.exitValue() != 0) {
            log.error("Conversão terminou com erro (exit {}): {}", processo.exitValue(), saidaProcesso);
            throw new ConversaoDocumentoException("Não foi possível converter o documento");
        }
    }

    private byte[] lerSaida(Path arquivoSaida) throws IOException {
        if (!Files.exists(arquivoSaida)) {
            log.error("Conversão terminou sem gerar o arquivo de saída esperado: {}", arquivoSaida);
            throw new ConversaoDocumentoException("Não foi possível converter o documento");
        }
        return Files.readAllBytes(arquivoSaida);
    }

    private boolean aguardar(Process processo) throws IOException {
        try {
            return processo.waitFor(TIMEOUT_SEGUNDOS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            processo.destroyForcibly();
            throw new ConversaoDocumentoException("Conversão interrompida");
        }
    }

    private void apagarRecursivo(Path diretorio) {
        try (Stream<Path> stream = Files.walk(diretorio)) {
            stream.sorted(Comparator.reverseOrder()).forEach(caminho -> {
                try {
                    Files.delete(caminho);
                } catch (IOException ignorado) {
                    log.warn("Não foi possível apagar arquivo temporário: {}", caminho);
                }
            });
        } catch (IOException ignorado) {
            log.warn("Não foi possível limpar diretório temporário: {}", diretorio);
        }
    }
}
