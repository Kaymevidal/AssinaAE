package com.kayomeira.assinatura.service;

import com.kayomeira.assinatura.dto.ContratoAssinaturaDTO;
import com.kayomeira.assinatura.exception.DocumentoNaoVisualizadoException;
import com.kayomeira.assinatura.model.Contrato;
import com.kayomeira.assinatura.model.Profissional;
import com.kayomeira.assinatura.repository.ContratoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * A assinatura só pode ser confirmada depois que a parte registrou que
 * visualizou o documento inteiro (ver {@link ContratoService#confirmarLeitura}
 * e a checagem correspondente em {@link ContratoService#assinar}).
 */
class ContratoServiceLeituraTest {

    private static final String ASSINATURA_BASE64 =
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=";

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

    @Mock
    private ContratoRepository contratoRepository;

    private ContratoService contratoService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        contratoService = new ContratoService(contratoRepository, new AssinaturaPDFService(), new EmailService());
        when(contratoRepository.save(any(Contrato.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private Contrato contratoCliente() {
        Profissional profissional = Profissional.builder().id(1L).nome("Ana").email("ana@teste.com").build();
        return Contrato.builder()
                .id(10L)
                .profissional(profissional)
                .nomeCliente("Cliente Teste")
                .tokenProfissional("token-profissional")
                .tokenCliente("token-cliente")
                .pdfOriginal(PDF_MINIMO.getBytes())
                .build();
    }

    @Test
    void assinarSemConfirmarLeituraEhBloqueado() {
        Contrato contrato = contratoCliente();
        when(contratoRepository.findByTokenProfissional("token-cliente")).thenReturn(Optional.empty());
        when(contratoRepository.findByTokenCliente("token-cliente")).thenReturn(Optional.of(contrato));

        assertThatThrownBy(() -> contratoService.assinar(
                "token-cliente", ASSINATURA_BASE64, 0, 0.1f, 0.1f, 0.2f, 0.1f))
                .isInstanceOf(DocumentoNaoVisualizadoException.class);
    }

    @Test
    void confirmarLeituraLiberaAssinatura() throws Exception {
        Contrato contrato = contratoCliente();
        when(contratoRepository.findByTokenProfissional("token-cliente")).thenReturn(Optional.empty());
        when(contratoRepository.findByTokenCliente("token-cliente")).thenReturn(Optional.of(contrato));

        ContratoAssinaturaDTO resultado = contratoService.confirmarLeitura("token-cliente");
        assertThat(resultado.isDocumentoVisualizado()).isTrue();
        assertThat(contrato.getDataVisualizacaoCliente()).isNotNull();

        // não lança DocumentoNaoVisualizadoException — chega a manipular o PDF normalmente
        contratoService.assinar("token-cliente", ASSINATURA_BASE64, 0, 0.1f, 0.1f, 0.2f, 0.1f);
    }

    @Test
    void confirmarLeituraEhIdempotente() {
        Contrato contrato = contratoCliente();
        LocalDateTime dataOriginal = LocalDateTime.now().minusDays(1);
        contrato.setDataVisualizacaoCliente(dataOriginal);
        when(contratoRepository.findByTokenProfissional("token-cliente")).thenReturn(Optional.empty());
        when(contratoRepository.findByTokenCliente("token-cliente")).thenReturn(Optional.of(contrato));

        contratoService.confirmarLeitura("token-cliente");

        assertThat(contrato.getDataVisualizacaoCliente()).isEqualTo(dataOriginal);
    }
}
