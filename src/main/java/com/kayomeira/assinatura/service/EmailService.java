package com.kayomeira.assinatura.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kayomeira.assinatura.model.Contrato;
import com.kayomeira.assinatura.model.Profissional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Envia email pela API HTTP do SendGrid (porta 443) em vez de SMTP. A
 * Railway (e boa parte dos PaaS) bloqueia as portas de SMTP de saída
 * (25/465/587/2525) contra abuso/spam — a API HTTP do provedor é o caminho
 * recomendado nesses casos, e usa a mesma porta que o resto da aplicação já
 * usa pra falar com o mundo.
 */
@Service
@Slf4j
public class EmailService {

    private static final URI SENDGRID_URL = URI.create("https://api.sendgrid.com/v3/mail/send");

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.mail-from}")
    private String emailFrom;

    @Value("${app.sendgrid-api-key:}")
    private String sendgridApiKey;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    /**
     * Envia link de assinatura para profissional
     */
    public void enviarLinkAssinaturaProfissional(Contrato contrato) {
        enviar(contrato.getProfissional().getEmail(), null,
                "Novo contrato aguardando sua assinatura - " + contrato.getTitulo(),
                gerarCorpoEmailProfissional(contrato), null,
                "email para profissional");
    }

    /**
     * Envia link de assinatura para cliente
     */
    public void enviarLinkAssinaturaCliente(Contrato contrato) {
        enviar(contrato.getEmailCliente(), contrato.getProfissional().getEmail(),
                "Novo contrato aguardando sua assinatura - " + contrato.getTitulo(),
                gerarCorpoEmailCliente(contrato), null,
                "email para cliente");
    }

    /**
     * Notifica que o contrato foi totalmente assinado
     */
    public void notificarContratoAssinado(Contrato contrato) {
        enviarNotificacaoAssinado(contrato.getProfissional().getEmail(), contrato.getEmailCliente(), contrato.getTokenProfissional(), contrato);
        enviarNotificacaoAssinado(contrato.getEmailCliente(), contrato.getProfissional().getEmail(), contrato.getTokenCliente(), contrato);
    }

    /**
     * Envia PDF assinado por email
     */
    public void enviarPDFAssinado(String email, Contrato contrato) {
        boolean paraProfissional = email.equals(contrato.getProfissional().getEmail());
        String emailOutraParte = paraProfissional ? contrato.getEmailCliente() : contrato.getProfissional().getEmail();

        Anexo anexo = new Anexo("contrato-assinado.pdf", contrato.getPdfAssinado());
        enviar(email, emailOutraParte, "Contrato assinado - " + contrato.getTitulo(),
                gerarCorpoEmailPDFAssinado(contrato), anexo, "PDF assinado");
    }

    /**
     * Notifica a outra parte que o contrato foi recusado por uma delas.
     */
    public void notificarContratoRecusado(Contrato contrato, String papelQueRecusou) {
        boolean recusadoPeloProfissional = "PROFISSIONAL".equals(papelQueRecusou);
        String emailDestino = recusadoPeloProfissional ? contrato.getEmailCliente() : contrato.getProfissional().getEmail();
        String emailQuemRecusou = recusadoPeloProfissional ? contrato.getProfissional().getEmail() : contrato.getEmailCliente();
        String nomeQuemRecusou = recusadoPeloProfissional ? contrato.getProfissional().getNome() : contrato.getNomeCliente();

        String corpo = "Olá,\n\n" +
                nomeQuemRecusou + " recusou o contrato \"" + contrato.getTitulo() + "\".\n\n" +
                "Nenhuma outra ação é necessária.\n\n" +
                "Atenciosamente,\nPlataforma de Assinatura Digital";

        enviar(emailDestino, emailQuemRecusou, "Contrato recusado - " + contrato.getTitulo(), corpo, null, "notificação de recusa");
    }

    /**
     * Envia o link de confirmação de email no cadastro por senha.
     */
    public void enviarEmailVerificacao(Profissional profissional) {
        String corpo = "Olá " + profissional.getNome() + ",\n\n" +
                "Confirme seu email para poder enviar contratos:\n" +
                frontendUrl + "/verificar-email/" + profissional.getTokenVerificacaoEmail() + "\n\n" +
                "Se você não criou essa conta, ignore este email.\n\n" +
                "Atenciosamente,\nPlataforma de Assinatura Digital";

        enviar(profissional.getEmail(), null, "Confirme seu email - Assinatura Digital", corpo, null, "email de verificação");
    }

    /**
     * Envia o link de redefinição de senha. Quem chama já decidiu que o
     * email existe e tem senha cadastrada — aqui é só o envio em si.
     */
    public void enviarEmailResetSenha(Profissional profissional) {
        String corpo = "Olá " + profissional.getNome() + ",\n\n" +
                "Recebemos um pedido para redefinir sua senha. O link abaixo é válido por 1 hora:\n" +
                frontendUrl + "/redefinir-senha/" + profissional.getTokenResetSenha() + "\n\n" +
                "Se você não pediu isso, ignore este email — sua senha continua a mesma.\n\n" +
                "Atenciosamente,\nPlataforma de Assinatura Digital";

        enviar(profissional.getEmail(), null, "Redefinição de senha - Assinatura Digital", corpo, null, "email de redefinição de senha");
    }

    // ============ Métodos auxiliares ============

    private record Anexo(String nomeArquivo, byte[] bytes) {
    }

    private void enviarNotificacaoAssinado(String email, String emailOutraParte, String token, Contrato contrato) {
        String corpo = "Parabéns!\n\n" +
                "O contrato \"" + contrato.getTitulo() + "\" foi completamente assinado por ambas as partes.\n\n" +
                "Você pode baixar o PDF assinado no link abaixo:\n" +
                frontendUrl + "/download/" + token + "\n\n" +
                "Atenciosamente,\nPlataforma de Assinatura Digital";

        enviar(email, emailOutraParte, "✓ Contrato totalmente assinado - " + contrato.getTitulo(), corpo, null, "notificação de assinatura");
    }

    /**
     * Monta e envia o email via API HTTP do SendGrid. Falhas (rede, provedor
     * fora do ar, credencial inválida) são só logadas — o envio de email é
     * best-effort em toda a aplicação, nunca pode derrubar a operação que o
     * disparou (criar contrato, assinar, etc.).
     */
    private void enviar(String destinatario, String replyTo, String assunto, String corpo, Anexo anexo, String descricao) {
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("personalizations", List.of(Map.of("to", List.of(Map.of("email", destinatario)))));
            body.put("from", Map.of("email", emailFrom, "name", "Assinatura Digital"));
            if (replyTo != null) {
                body.put("reply_to", Map.of("email", replyTo));
            }
            body.put("subject", assunto);
            body.put("content", List.of(Map.of("type", "text/plain", "value", corpo)));
            if (anexo != null) {
                body.put("attachments", List.of(Map.of(
                        "content", Base64.getEncoder().encodeToString(anexo.bytes()),
                        "filename", anexo.nomeArquivo(),
                        "type", "application/pdf",
                        "disposition", "attachment")));
            }

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(SENDGRID_URL)
                    .header("Authorization", "Bearer " + sendgridApiKey)
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(10))
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 300) {
                throw new IOException("SendGrid respondeu " + response.statusCode() + ": " + response.body());
            }

            log.info("Enviado ({}) para: {}", descricao, destinatario);
        } catch (Exception e) {
            log.error("Erro ao enviar {} para {}", descricao, destinatario, e);
        }
    }

    private String gerarCorpoEmailProfissional(Contrato contrato) {
        return "Olá " + contrato.getProfissional().getNome() + ",\n\n" +
                "Você recebeu um novo contrato para assinatura:\n\n" +
                "Título: " + contrato.getTitulo() + "\n" +
                "Descrição: " + contrato.getDescricao() + "\n" +
                "Cliente: " + contrato.getNomeCliente() + "\n\n" +
                "Clique no link abaixo para assinar:\n" +
                frontendUrl + "/assinar/" + contrato.getTokenProfissional() + "\n\n" +
                "Atenciosamente,\nPlataforma de Assinatura Digital";
    }

    private String gerarCorpoEmailCliente(Contrato contrato) {
        return "Olá " + contrato.getNomeCliente() + ",\n\n" +
                "Você recebeu um novo contrato para assinatura:\n\n" +
                "Título: " + contrato.getTitulo() + "\n" +
                "Descrição: " + contrato.getDescricao() + "\n" +
                "Profissional: " + contrato.getProfissional().getNome() + "\n\n" +
                "Clique no link abaixo para assinar:\n" +
                frontendUrl + "/assinar/" + contrato.getTokenCliente() + "\n\n" +
                "Atenciosamente,\nPlataforma de Assinatura Digital";
    }

    private String gerarCorpoEmailPDFAssinado(Contrato contrato) {
        return "Olá,\n\n" +
                "O contrato \"" + contrato.getTitulo() + "\" foi completamente assinado!\n\n" +
                "O arquivo PDF assinado está anexado neste email.\n\n" +
                "Atenciosamente,\nPlataforma de Assinatura Digital";
    }
}
