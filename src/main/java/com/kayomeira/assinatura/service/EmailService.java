package com.kayomeira.assinatura.service;

import com.kayomeira.assinatura.model.Contrato;
import com.kayomeira.assinatura.model.Profissional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {
    
    private final JavaMailSender mailSender;

    @Value("${spring.mail.from}")
    private String emailFrom;

    @Value("${app.frontend-url}")
    private String frontendUrl;
    
    /**
     * Envia link de assinatura para profissional
     */
    public void enviarLinkAssinaturaProfissional(Contrato contrato) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(emailFrom);
            message.setTo(contrato.getProfissional().getEmail());
            message.setSubject("Novo contrato aguardando sua assinatura - " + contrato.getTitulo());
            message.setText(gerarCorpoEmailProfissional(contrato));

            mailSender.send(message);
            log.info("Email de assinatura enviado para profissional: {}", contrato.getProfissional().getEmail());
        } catch (Exception e) {
            log.error("Erro ao enviar email para profissional", e);
        }
    }
    
    /**
     * Envia link de assinatura para cliente
     */
    public void enviarLinkAssinaturaCliente(Contrato contrato) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(emailFrom);
            message.setTo(contrato.getEmailCliente());
            message.setSubject("Novo contrato aguardando sua assinatura - " + contrato.getTitulo());
            message.setText(gerarCorpoEmailCliente(contrato));
            
            mailSender.send(message);
            log.info("Email de assinatura enviado para cliente: {}", contrato.getEmailCliente());
        } catch (Exception e) {
            log.error("Erro ao enviar email para cliente", e);
        }
    }
    
    /**
     * Notifica que o contrato foi totalmente assinado
     */
    public void notificarContratoAssinado(Contrato contrato) {
        enviarNotificacaoAssinado(contrato.getProfissional().getEmail(), contrato.getTokenProfissional(), contrato);
        enviarNotificacaoAssinado(contrato.getEmailCliente(), contrato.getTokenCliente(), contrato);
    }
    
    /**
     * Envia PDF assinado por email
     */
    public void enviarPDFAssinado(String email, Contrato contrato) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            
            helper.setFrom(emailFrom);
            helper.setTo(email);
            helper.setSubject("Contrato assinado - " + contrato.getTitulo());
            helper.setText(gerarCorpoEmailPDFAssinado(contrato));
            
            // Anexa o PDF
            ByteArrayResource resource = new ByteArrayResource(contrato.getPdfAssinado());
            helper.addAttachment("contrato-assinado.pdf", resource);
            
            mailSender.send(message);
            log.info("PDF assinado enviado para: {}", email);
        } catch (Exception e) {
            // Captura tanto MessagingException (montagem da mensagem) quanto as exceções
            // unchecked do Spring (MailAuthenticationException, MailSendException etc.) —
            // uma falha de SMTP aqui não pode derrubar a transação e desfazer a assinatura
            // que acabou de ser registrada.
            log.error("Erro ao enviar PDF assinado", e);
        }
    }
    
    /**
     * Notifica a outra parte que o contrato foi recusado por uma delas.
     */
    public void notificarContratoRecusado(Contrato contrato, String papelQueRecusou) {
        boolean recusadoPeloProfissional = "PROFISSIONAL".equals(papelQueRecusou);
        String emailDestino = recusadoPeloProfissional ? contrato.getEmailCliente() : contrato.getProfissional().getEmail();
        String nomeQuemRecusou = recusadoPeloProfissional ? contrato.getProfissional().getNome() : contrato.getNomeCliente();

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(emailFrom);
            message.setTo(emailDestino);
            message.setSubject("Contrato recusado - " + contrato.getTitulo());
            message.setText("Olá,\n\n" +
                    nomeQuemRecusou + " recusou o contrato \"" + contrato.getTitulo() + "\".\n\n" +
                    "Nenhuma outra ação é necessária.\n\n" +
                    "Atenciosamente,\nPlataforma de Assinatura Digital");

            mailSender.send(message);
            log.info("Notificação de recusa enviada para: {}", emailDestino);
        } catch (Exception e) {
            log.error("Erro ao enviar notificação de recusa", e);
        }
    }

    /**
     * Envia o link de confirmação de email no cadastro por senha.
     */
    public void enviarEmailVerificacao(Profissional profissional) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(emailFrom);
            message.setTo(profissional.getEmail());
            message.setSubject("Confirme seu email - Assinatura Digital");
            message.setText("Olá " + profissional.getNome() + ",\n\n" +
                    "Confirme seu email para poder enviar contratos:\n" +
                    frontendUrl + "/verificar-email/" + profissional.getTokenVerificacaoEmail() + "\n\n" +
                    "Se você não criou essa conta, ignore este email.\n\n" +
                    "Atenciosamente,\nPlataforma de Assinatura Digital");

            mailSender.send(message);
            log.info("Email de verificação enviado para: {}", profissional.getEmail());
        } catch (Exception e) {
            log.error("Erro ao enviar email de verificação", e);
        }
    }

    /**
     * Envia o link de redefinição de senha. Quem chama já decidiu que o
     * email existe e tem senha cadastrada — aqui é só o envio em si.
     */
    public void enviarEmailResetSenha(Profissional profissional) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(emailFrom);
            message.setTo(profissional.getEmail());
            message.setSubject("Redefinição de senha - Assinatura Digital");
            message.setText("Olá " + profissional.getNome() + ",\n\n" +
                    "Recebemos um pedido para redefinir sua senha. O link abaixo é válido por 1 hora:\n" +
                    frontendUrl + "/redefinir-senha/" + profissional.getTokenResetSenha() + "\n\n" +
                    "Se você não pediu isso, ignore este email — sua senha continua a mesma.\n\n" +
                    "Atenciosamente,\nPlataforma de Assinatura Digital");

            mailSender.send(message);
            log.info("Email de redefinição de senha enviado para: {}", profissional.getEmail());
        } catch (Exception e) {
            log.error("Erro ao enviar email de redefinição de senha", e);
        }
    }

    // ============ Métodos auxiliares ============
    
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
    
    private void enviarNotificacaoAssinado(String email, String token, Contrato contrato) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(emailFrom);
            message.setTo(email);
            message.setSubject("✓ Contrato totalmente assinado - " + contrato.getTitulo());
            message.setText("Parabéns!\n\n" +
                    "O contrato \"" + contrato.getTitulo() + "\" foi completamente assinado por ambas as partes.\n\n" +
                    "Você pode baixar o PDF assinado no link abaixo:\n" +
                    frontendUrl + "/download/" + token + "\n\n" +
                    "Atenciosamente,\nPlataforma de Assinatura Digital");
            
            mailSender.send(message);
            log.info("Notificação de contrato assinado enviada para: {}", email);
        } catch (Exception e) {
            log.error("Erro ao enviar notificação de assinatura", e);
        }
    }
    
    private String gerarCorpoEmailPDFAssinado(Contrato contrato) {
        return "Olá,\n\n" +
                "O contrato \"" + contrato.getTitulo() + "\" foi completamente assinado!\n\n" +
                "O arquivo PDF assinado está anexado neste email.\n\n" +
                "Atenciosamente,\nPlataforma de Assinatura Digital";
    }
}
