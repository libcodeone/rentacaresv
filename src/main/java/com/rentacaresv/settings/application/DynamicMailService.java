package com.rentacaresv.settings.application;

import com.rentacaresv.settings.domain.Settings;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.util.Properties;

/**
 * Servicio de email dinámico que lee la configuración desde la base de datos.
 * Crea un JavaMailSender nuevo en cada envío basado en la configuración actual.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DynamicMailService {

    private final SettingsCache settingsCache;

    /**
     * Envía un email usando la configuración almacenada en la base de datos.
     * 
     * @param to Destinatario
     * @param subject Asunto
     * @param htmlContent Contenido HTML del email
     * @throws MessagingException si hay error al enviar
     */
    public void sendEmail(String to, String subject, String htmlContent) throws MessagingException {
        sendEmail(to, subject, htmlContent, null, null);
    }

    /**
     * Envía un email con adjunto usando la configuración almacenada en la base de datos.
     * 
     * @param to Destinatario
     * @param subject Asunto
     * @param htmlContent Contenido HTML del email
     * @param attachmentName Nombre del archivo adjunto (puede ser null)
     * @param attachmentData Datos del archivo adjunto (puede ser null)
     * @throws MessagingException si hay error al enviar
     */
    public void sendEmail(String to, String subject, String htmlContent, 
                          String attachmentName, byte[] attachmentData) throws MessagingException {
        
        Settings settings = settingsCache.getSettings();
        
        // Validar configuración
        if (!isEmailEnabled()) {
            log.info("Envío de email deshabilitado en configuración");
            return;
        }
        
        if (!settings.isEmailConfigured()) {
            log.warn("Configuración de email incompleta. No se puede enviar email.");
            throw new MessagingException("Configuración de email incompleta. Configure el servidor SMTP en Configuración.");
        }
        
        // Crear JavaMailSender con configuración actual
        JavaMailSender mailSender = createMailSender(settings);
        
        // Crear mensaje
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, attachmentData != null, "UTF-8");
        
        try {
            // Configurar From con nombre
            String fromEmail = settings.getMailFrom() != null ? settings.getMailFrom() : settings.getMailUsername();
            String fromName = settings.getMailFromName() != null ? settings.getMailFromName() : settings.getCompanyName();
            helper.setFrom(new InternetAddress(fromEmail, fromName));
        } catch (UnsupportedEncodingException e) {
            // Fallback sin nombre
            helper.setFrom(settings.getMailFrom() != null ? settings.getMailFrom() : settings.getMailUsername());
        }
        
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlContent, true);
        
        // Agregar adjunto si existe
        if (attachmentName != null && attachmentData != null) {
            helper.addAttachment(attachmentName, new ByteArrayResource(attachmentData));
        }
        
        // Enviar
        log.info("Enviando email a: {} | Host: {} | Puerto: {}", to, settings.getMailHost(), settings.getMailPort());
        mailSender.send(message);
        log.info("✅ Email enviado exitosamente a: {}", to);
    }

    /**
     * Verifica si el envío de email está habilitado
     */
    public boolean isEmailEnabled() {
        Settings settings = settingsCache.getSettings();
        return Boolean.TRUE.equals(settings.getEmailEnabled()) && settings.isEmailConfigured();
    }

    /**
     * Prueba la conexión SMTP con la configuración actual
     * 
     * @return true si la conexión es exitosa
     */
    public boolean testConnection() {
        Settings settings = settingsCache.getSettings();
        
        if (!settings.isEmailConfigured()) {
            log.warn("Configuración de email incompleta para prueba de conexión");
            return false;
        }
        
        try {
            JavaMailSenderImpl mailSender = (JavaMailSenderImpl) createMailSender(settings);
            mailSender.testConnection();
            log.info("✅ Conexión SMTP exitosa");
            return true;
        } catch (MessagingException e) {
            log.error("❌ Error de conexión SMTP: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Envía un email de prueba
     * 
     * @param testEmail Email destino para la prueba
     * @throws MessagingException si hay error
     */
    public void sendTestEmail(String testEmail) throws MessagingException {
        Settings settings = settingsCache.getSettings();
        String companyName = settings.getCompanyName() != null ? settings.getCompanyName() : "RentaCarESV";
        
        String htmlContent = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background: #003366; color: white; padding: 20px; text-align: center; border-radius: 8px 8px 0 0; }
                    .content { padding: 20px; background: #f9f9f9; }
                    .success { background: #d4edda; padding: 15px; border-radius: 8px; border-left: 4px solid #28a745; margin: 10px 0; }
                    .footer { text-align: center; padding: 20px; font-size: 12px; color: #666; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>%s</h1>
                    </div>
                    <div class="content">
                        <div class="success">
                            <h2>✅ ¡Configuración de Email Exitosa!</h2>
                            <p>Este es un correo de prueba para verificar que la configuración SMTP está funcionando correctamente.</p>
                        </div>
                        <p><strong>Detalles de configuración:</strong></p>
                        <ul>
                            <li>Servidor: %s</li>
                            <li>Puerto: %d</li>
                            <li>Usuario: %s</li>
                            <li>STARTTLS: %s</li>
                            <li>SSL: %s</li>
                        </ul>
                    </div>
                    <div class="footer">
                        <p>Este es un correo automático de prueba.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(
                companyName,
                settings.getMailHost(),
                settings.getMailPort(),
                settings.getMailUsername(),
                Boolean.TRUE.equals(settings.getMailStarttlsEnable()) ? "Habilitado" : "Deshabilitado",
                Boolean.TRUE.equals(settings.getMailSslEnable()) ? "Habilitado" : "Deshabilitado"
            );
        
        sendEmail(testEmail, "Prueba de Configuración de Email - " + companyName, htmlContent);
    }

    /**
     * Crea un JavaMailSender configurado con los valores de Settings
     */
    private JavaMailSender createMailSender(Settings settings) {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        
        mailSender.setHost(settings.getMailHost());
        mailSender.setPort(settings.getMailPort() != null ? settings.getMailPort() : 587);
        mailSender.setUsername(settings.getMailUsername());
        mailSender.setPassword(settings.getMailPassword());
        
        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", settings.getMailProtocol() != null ? settings.getMailProtocol() : "smtp");
        props.put("mail.smtp.auth", Boolean.TRUE.equals(settings.getMailSmtpAuth()) ? "true" : "false");
        props.put("mail.smtp.starttls.enable", Boolean.TRUE.equals(settings.getMailStarttlsEnable()) ? "true" : "false");
        props.put("mail.smtp.starttls.required", Boolean.TRUE.equals(settings.getMailStarttlsEnable()) ? "true" : "false");
        props.put("mail.smtp.ssl.enable", Boolean.TRUE.equals(settings.getMailSslEnable()) ? "true" : "false");
        
        // SSL trust para servidores que lo requieren
        if (Boolean.TRUE.equals(settings.getMailSslEnable()) || Boolean.TRUE.equals(settings.getMailStarttlsEnable())) {
            props.put("mail.smtp.ssl.trust", settings.getMailHost());
        }
        
        // Timeouts
        int connectionTimeout = settings.getMailConnectionTimeout() != null ? settings.getMailConnectionTimeout() : 10000;
        int timeout = settings.getMailTimeout() != null ? settings.getMailTimeout() : 10000;
        
        props.put("mail.smtp.connectiontimeout", String.valueOf(connectionTimeout));
        props.put("mail.smtp.timeout", String.valueOf(timeout));
        props.put("mail.smtp.writetimeout", String.valueOf(timeout));
        
        // Debug (solo para desarrollo)
        // props.put("mail.debug", "true");
        
        return mailSender;
    }
}
