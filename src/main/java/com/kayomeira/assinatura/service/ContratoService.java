package com.kayomeira.assinatura.service;

import com.kayomeira.assinatura.dto.ContratoAssinaturaDTO;
import com.kayomeira.assinatura.dto.ContratoRequestDTO;
import com.kayomeira.assinatura.exception.ContratoNotFoundException;
import com.kayomeira.assinatura.exception.PdfInvalidoException;
import com.kayomeira.assinatura.model.Contrato;
import com.kayomeira.assinatura.model.StatusAssinatura;
import com.kayomeira.assinatura.repository.ContratoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContratoService {

    private final ContratoRepository contratoRepository;
    private final AssinaturaPDFService assinaturaPDFService;
    private final EmailService emailService;

    /**
     * Cria um novo contrato a partir do PDF enviado, gera os tokens de
     * assinatura de cada parte e dispara os emails de convite.
     */
    @Transactional
    public Contrato criarContrato(ContratoRequestDTO dto, MultipartFile pdf) throws IOException {
        byte[] pdfBytes = pdf.getBytes();

        if (!assinaturaPDFService.validarPDF(pdfBytes)) {
            throw new PdfInvalidoException("O arquivo enviado não é um PDF válido");
        }

        Contrato contrato = Contrato.builder()
                .titulo(dto.getTitulo())
                .descricao(dto.getDescricao())
                .emailProfissional(dto.getEmailProfissional())
                .emailCliente(dto.getEmailCliente())
                .nomeProfissional(dto.getNomeProfissional())
                .nomeCliente(dto.getNomeCliente())
                .pdfOriginal(pdfBytes)
                .statusProfissional(StatusAssinatura.PENDENTE)
                .statusCliente(StatusAssinatura.PENDENTE)
                .tokenProfissional(gerarToken())
                .tokenCliente(gerarToken())
                .dataCriacao(LocalDateTime.now())
                .build();

        Contrato salvo = contratoRepository.save(contrato);

        emailService.enviarLinkAssinaturaProfissional(salvo);
        emailService.enviarLinkAssinaturaCliente(salvo);

        log.info("Contrato criado: id={}, titulo={}", salvo.getId(), salvo.getTitulo());
        return salvo;
    }

    @Transactional(readOnly = true)
    public Contrato buscarPorId(Long id) {
        return contratoRepository.findById(id)
                .orElseThrow(() -> new ContratoNotFoundException("Contrato não encontrado: " + id));
    }

    /**
     * Resolve um contrato a partir do token de assinatura de uma das partes,
     * identificando o papel (PROFISSIONAL ou CLIENTE) de quem está acessando.
     */
    @Transactional(readOnly = true)
    public ContratoAssinaturaDTO buscarPorToken(String token) {
        Contrato contrato = contratoRepository.findByTokenProfissional(token).orElse(null);
        String papel = "PROFISSIONAL";

        if (contrato == null) {
            contrato = contratoRepository.findByTokenCliente(token)
                    .orElseThrow(() -> new ContratoNotFoundException("Link de assinatura inválido ou expirado"));
            papel = "CLIENTE";
        }

        return ContratoAssinaturaDTO.fromEntity(contrato, papel);
    }

    /**
     * Registra a assinatura da parte associada ao token, embutindo a imagem
     * no PDF imediatamente (em cima do PDF original, ou do já parcialmente
     * assinado pela outra parte). Quando ambas as partes já tiverem
     * assinado, notifica os envolvidos por email com o PDF final.
     *
     * As assinaturas são aplicadas uma de cada vez, e não em lote no final,
     * porque cada parte assina em uma requisição HTTP separada e os campos
     * de assinatura em base64 da entidade são @Transient (não persistidos).
     */
    @Transactional
    public ContratoAssinaturaDTO assinar(String token, String assinaturaBase64) throws IOException {
        Contrato contrato = contratoRepository.findByTokenProfissional(token).orElse(null);
        boolean isProfissional = contrato != null;

        if (contrato == null) {
            contrato = contratoRepository.findByTokenCliente(token)
                    .orElseThrow(() -> new ContratoNotFoundException("Link de assinatura inválido ou expirado"));
        }

        StatusAssinatura statusAtual = isProfissional ? contrato.getStatusProfissional() : contrato.getStatusCliente();
        if (statusAtual == StatusAssinatura.ASSINADO) {
            throw new IllegalStateException("Esta parte já assinou o contrato");
        }

        byte[] pdfBase = contrato.getPdfAssinado() != null ? contrato.getPdfAssinado() : contrato.getPdfOriginal();
        float posicaoX = isProfissional ? 50 : 350;
        String nomeSignatario = isProfissional ? contrato.getNomeProfissional() : contrato.getNomeCliente();

        contrato.setPdfAssinado(assinaturaPDFService.adicionarAssinatura(pdfBase, assinaturaBase64, posicaoX, 300, nomeSignatario));

        if (isProfissional) {
            contrato.setStatusProfissional(StatusAssinatura.ASSINADO);
            contrato.setDataAssinaturaProfissional(LocalDateTime.now());
        } else {
            contrato.setStatusCliente(StatusAssinatura.ASSINADO);
            contrato.setDataAssinaturaCliente(LocalDateTime.now());
        }

        if (contrato.ambosAssinaram()) {
            emailService.notificarContratoAssinado(contrato);
            emailService.enviarPDFAssinado(contrato.getEmailProfissional(), contrato);
            emailService.enviarPDFAssinado(contrato.getEmailCliente(), contrato);
            log.info("Contrato finalizado (ambas as partes assinaram): id={}", contrato.getId());
        }

        Contrato salvo = contratoRepository.save(contrato);
        String papel = isProfissional ? "PROFISSIONAL" : "CLIENTE";
        return ContratoAssinaturaDTO.fromEntity(salvo, papel);
    }

    private String gerarToken() {
        return UUID.randomUUID().toString();
    }
}
