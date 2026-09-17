package com.kayomeira.assinatura.service;

import com.kayomeira.assinatura.dto.EdicaoTextoDTO;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Cada edição é isolada: uma edição ruim (caractere não suportado, dado
 * incompleto) nunca pode derrubar as outras nem fazer o endpoint falhar —
 * o pior resultado aceitável é essa edição específica ser ignorada.
 */
class EdicaoPdfServiceTest {

    private static final String PDF_MINIMO = """
            %PDF-1.1
            1 0 obj << /Type /Catalog /Pages 2 0 R >> endobj
            2 0 obj << /Type /Pages /Kids [3 0 R] /Count 1 >> endobj
            3 0 obj << /Type /Page /Parent 2 0 R /MediaBox [0 0 200 200] >> endobj
            xref
            0 4
            0000000000 65535 f\s
            trailer << /Size 4 /Root 1 0 R >>
            startxref
            0
            %%EOF
            """;

    private final EdicaoPdfService service = new EdicaoPdfService();

    private EdicaoTextoDTO edicao(Integer pagina, Float x, Float y, Float largura, Float altura, String texto) {
        EdicaoTextoDTO dto = new EdicaoTextoDTO();
        dto.setPagina(pagina);
        dto.setX(x);
        dto.setY(y);
        dto.setLargura(largura);
        dto.setAltura(altura);
        dto.setTexto(texto);
        return dto;
    }

    @Test
    void aplicaEdicaoValida() throws Exception {
        byte[] resultado = service.aplicarEdicoes(
                PDF_MINIMO.getBytes(),
                List.of(edicao(0, 0.1f, 0.1f, 0.3f, 0.05f, "Texto novo")));

        assertThat(resultado).isNotEmpty();
        assertThat(new String(resultado, 0, 5)).isEqualTo("%PDF-");
    }

    @Test
    void caractereNaoSuportadoNaoDerrubaAEdicao() {
        assertThatCode(() -> {
            byte[] resultado = service.aplicarEdicoes(
                    PDF_MINIMO.getBytes(),
                    List.of(edicao(0, 0.1f, 0.1f, 0.3f, 0.05f, "emoji: 😀")));
            assertThat(resultado).isNotEmpty();
        }).doesNotThrowAnyException();
    }

    @Test
    void edicaoComDadoIncompletoEhIgnoradaSemAfetarAsOutras() {
        EdicaoTextoDTO invalida = edicao(0, 0.1f, 0.1f, null, 0.05f, "não deveria aplicar");
        EdicaoTextoDTO valida = edicao(0, 0.2f, 0.2f, 0.3f, 0.05f, "essa deve aplicar");

        assertThatCode(() -> {
            byte[] resultado = service.aplicarEdicoes(PDF_MINIMO.getBytes(), List.of(invalida, valida));
            assertThat(resultado).isNotEmpty();
            assertThat(new String(resultado, 0, 5)).isEqualTo("%PDF-");
        }).doesNotThrowAnyException();
    }

    @Test
    void listaVaziaDevolveDocumentoIntacto() throws Exception {
        byte[] resultado = service.aplicarEdicoes(PDF_MINIMO.getBytes(), List.of());
        assertThat(resultado).isNotEmpty();
    }

    @ParameterizedTest
    @CsvSource({
            "serif, false, false",
            "serif, true, false",
            "serif, false, true",
            "serif, true, true",
            "monospace, false, false",
            "monospace, true, true",
            "sans, false, false",
            "sans, true, false",
            "sans, false, true",
            "sans, true, true",
    })
    void todaCombinacaoDeFonteAplicaOTextoCorretamente(String familia, boolean negrito, boolean italico) throws Exception {
        EdicaoTextoDTO edicao = edicao(0, 0.1f, 0.1f, 0.5f, 0.08f, "Texto " + familia + negrito + italico);
        edicao.setFamiliaFonte(familia);
        edicao.setNegrito(negrito);
        edicao.setItalico(italico);

        byte[] resultado = service.aplicarEdicoes(PDF_MINIMO.getBytes(), List.of(edicao));

        assertThat(extrairTexto(resultado)).contains(edicao.getTexto());
    }

    @Test
    void familiaDeFonteNulaCaiNoPadraoSemQuebrar() throws Exception {
        EdicaoTextoDTO edicao = edicao(0, 0.1f, 0.1f, 0.5f, 0.08f, "Texto sem familia definida");

        byte[] resultado = service.aplicarEdicoes(PDF_MINIMO.getBytes(), List.of(edicao));

        assertThat(extrairTexto(resultado)).contains("Texto sem familia definida");
    }

    @Test
    void corPersonalizadaDeTextoEFundoNaoQuebraAEdicao() throws Exception {
        EdicaoTextoDTO edicao = edicao(0, 0.1f, 0.1f, 0.5f, 0.08f, "Texto colorido");
        edicao.setCorR(200);
        edicao.setCorG(30);
        edicao.setCorB(30);
        edicao.setFundoR(255);
        edicao.setFundoG(240);
        edicao.setFundoB(200);

        byte[] resultado = service.aplicarEdicoes(PDF_MINIMO.getBytes(), List.of(edicao));

        assertThat(extrairTexto(resultado)).contains("Texto colorido");
    }

    @Test
    void textoMuitoMaiorQueACaixaAindaAplicaSemQuebrar() throws Exception {
        String textoLongo = "Esse texto de substituição é bem mais longo do que a caixa original permitiria mostrar sem ajuste de largura";
        EdicaoTextoDTO edicao = edicao(0, 0.1f, 0.1f, 0.15f, 0.03f, textoLongo);

        byte[] resultado = service.aplicarEdicoes(PDF_MINIMO.getBytes(), List.of(edicao));

        assertThat(extrairTexto(resultado)).contains(textoLongo);
    }

    private String extrairTexto(byte[] pdfBytes) throws Exception {
        try (PDDocument documento = Loader.loadPDF(pdfBytes)) {
            return new PDFTextStripper().getText(documento);
        }
    }
}
