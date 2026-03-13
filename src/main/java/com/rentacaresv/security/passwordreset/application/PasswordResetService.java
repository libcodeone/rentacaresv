package com.rentacaresv.security.passwordreset.application;

import com.rentacaresv.security.accesslog.application.AccessLogService;
import com.rentacaresv.security.accesslog.domain.AccessEventType;
import com.rentacaresv.security.passwordreset.domain.PasswordResetToken;
import com.rentacaresv.security.passwordreset.infrastructure.PasswordResetTokenRepository;
import com.rentacaresv.settings.application.DynamicMailService;
import com.rentacaresv.settings.application.SettingsCache;
import com.rentacaresv.user.domain.User;
import com.rentacaresv.user.infrastructure.UserRepository;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Servicio para recuperación de contraseña
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetService {

    private final PasswordResetTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final DynamicMailService mailService;
    private final PasswordEncoder passwordEncoder;
    private final SettingsCache settingsCache;
    private final AccessLogService accessLogService;

    @Value("${app.base-url:http://localhost:8091}")
    private String baseUrl;

    // Rate limiting: máximo de solicitudes por hora
    private static final int MAX_REQUESTS_PER_HOUR = 3;

    // ========================================
    // Solicitud de recuperación
    // ========================================

    /**
     * Solicita recuperación de contraseña para un email.
     * Envía un email con el enlace de recuperación.
     * 
     * @param email Email del usuario
     * @return true si se envió el email, false si no se encontró el usuario
     */
    @Transactional
    public boolean requestPasswordReset(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }

        email = email.toLowerCase().trim();
        
        // Buscar usuario por email
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            log.info("Solicitud de recuperación para email no registrado: {}", email);
            // No revelar si el email existe o no por seguridad
            return true;
        }

        User user = userOpt.get();

        // Verificar que el usuario esté activo
        if (!user.getActive()) {
            log.warn("Solicitud de recuperación para usuario inactivo: {}", email);
            return true;
        }

        // Rate limiting
        long recentRequests = tokenRepository.countRecentTokensByEmail(email, LocalDateTime.now().minusHours(1));
        if (recentRequests >= MAX_REQUESTS_PER_HOUR) {
            log.warn("Rate limit excedido para email: {}", email);
            return true;
        }

        // Invalidar tokens anteriores
        tokenRepository.invalidateAllUserTokens(user, LocalDateTime.now());

        // Crear nuevo token
        PasswordResetToken token = PasswordResetToken.createForUser(user);
        tokenRepository.save(token);

        // Enviar email
        try {
            sendPasswordResetEmail(user, token);
            log.info("Email de recuperación enviado a: {}", email);
            
            // Registrar en access log
            accessLogService.logEvent(user, AccessEventType.PASSWORD_RESET_REQUEST, 
                    "Solicitud de recuperación de contraseña");
            
            return true;
        } catch (MessagingException e) {
            log.error("Error enviando email de recuperación a {}: {}", email, e.getMessage());
            return false;
        }
    }

    /**
     * Envía el email de recuperación de contraseña
     */
    private void sendPasswordResetEmail(User user, PasswordResetToken token) throws MessagingException {
        String resetLink = baseUrl + "/reset-password?token=" + token.getToken();
        String companyName = settingsCache.getCompanyName();
        int hoursValid = PasswordResetToken.EXPIRATION_HOURS;

        String htmlContent = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; margin: 0; padding: 0; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background: linear-gradient(135deg, #003366 0%%, #0066cc 100%%); color: white; padding: 30px; text-align: center; border-radius: 8px 8px 0 0; }
                    .header h1 { margin: 0; font-size: 24px; }
                    .content { padding: 30px; background: #ffffff; border: 1px solid #e0e0e0; }
                    .button { display: inline-block; background: #0066cc; color: white !important; padding: 14px 30px; text-decoration: none; border-radius: 6px; font-weight: bold; margin: 20px 0; }
                    .button:hover { background: #0052a3; }
                    .warning { background: #fff3cd; padding: 15px; border-radius: 6px; border-left: 4px solid #ffc107; margin: 20px 0; font-size: 14px; }
                    .footer { text-align: center; padding: 20px; font-size: 12px; color: #666; background: #f5f5f5; border-radius: 0 0 8px 8px; }
                    .link-text { word-break: break-all; font-size: 12px; color: #666; margin-top: 15px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>%s</h1>
                    </div>
                    <div class="content">
                        <h2>Recuperación de Contraseña</h2>
                        <p>Hola <strong>%s</strong>,</p>
                        <p>Hemos recibido una solicitud para restablecer la contraseña de tu cuenta. Si no realizaste esta solicitud, puedes ignorar este correo.</p>
                        
                        <p style="text-align: center;">
                            <a href="%s" class="button">Restablecer Contraseña</a>
                        </p>
                        
                        <div class="warning">
                            <strong>⚠️ Importante:</strong>
                            <ul style="margin: 10px 0 0 0; padding-left: 20px;">
                                <li>Este enlace expira en <strong>%d horas</strong></li>
                                <li>Solo puede ser usado una vez</li>
                                <li>Si no solicitaste este cambio, ignora este correo</li>
                            </ul>
                        </div>
                        
                        <p class="link-text">Si el botón no funciona, copia y pega este enlace en tu navegador:<br>%s</p>
                    </div>
                    <div class="footer">
                        <p>Este es un correo automático de %s. Por favor no responder.</p>
                        <p>Si tienes problemas, contacta al administrador del sistema.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(companyName, user.getName(), resetLink, hoursValid, resetLink, companyName);

        mailService.sendEmail(
                user.getEmail(),
                "Recuperación de Contraseña - " + companyName,
                htmlContent
        );
    }

    // ========================================
    // Validación de token
    // ========================================

    /**
     * Valida un token de recuperación
     */
    public Optional<PasswordResetToken> validateToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            return Optional.empty();
        }

        Optional<PasswordResetToken> tokenOpt = tokenRepository.findByToken(token.trim());
        
        if (tokenOpt.isEmpty()) {
            log.warn("Token de recuperación no encontrado: {}", token.substring(0, Math.min(10, token.length())) + "...");
            return Optional.empty();
        }

        PasswordResetToken resetToken = tokenOpt.get();
        
        if (!resetToken.isValid()) {
            log.warn("Token de recuperación inválido o expirado para usuario: {}", resetToken.getUser().getUsername());
            return Optional.empty();
        }

        return Optional.of(resetToken);
    }

    /**
     * Obtiene el usuario asociado a un token válido
     */
    public Optional<User> getUserByValidToken(String token) {
        return validateToken(token).map(PasswordResetToken::getUser);
    }

    // ========================================
    // Restablecimiento de contraseña
    // ========================================

    /**
     * Restablece la contraseña usando un token válido
     */
    @Transactional
    public boolean resetPassword(String token, String newPassword) {
        Optional<PasswordResetToken> tokenOpt = validateToken(token);
        
        if (tokenOpt.isEmpty()) {
            return false;
        }

        PasswordResetToken resetToken = tokenOpt.get();
        User user = resetToken.getUser();

        // Validar contraseña
        if (newPassword == null || newPassword.length() < 6) {
            log.warn("Contraseña inválida en intento de reset para usuario: {}", user.getUsername());
            return false;
        }

        // Actualizar contraseña
        user.setHashedPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Marcar token como usado
        resetToken.markAsUsed();
        tokenRepository.save(resetToken);

        // Invalidar otros tokens del usuario
        tokenRepository.invalidateAllUserTokens(user, LocalDateTime.now());

        // Registrar en access log
        accessLogService.logEvent(user, AccessEventType.PASSWORD_CHANGE, 
                "Contraseña restablecida mediante enlace de recuperación");

        log.info("Contraseña restablecida exitosamente para usuario: {}", user.getUsername());
        return true;
    }

    // ========================================
    // Mantenimiento
    // ========================================

    /**
     * Limpia tokens expirados (ejecutado diariamente a las 4 AM)
     */
    @Scheduled(cron = "0 0 4 * * ?")
    @Transactional
    public void cleanupExpiredTokens() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(7); // Mantener 7 días después de expirar
        int deleted = tokenRepository.deleteExpiredTokens(cutoffDate);
        if (deleted > 0) {
            log.info("Limpieza de tokens de recuperación: {} tokens eliminados", deleted);
        }
    }

    // ========================================
    // Verificación de configuración
    // ========================================

    /**
     * Verifica si el email está configurado para enviar recuperación
     */
    public boolean isEmailConfigured() {
        return mailService.isEmailEnabled();
    }
}
