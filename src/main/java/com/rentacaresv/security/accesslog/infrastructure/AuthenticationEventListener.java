package com.rentacaresv.security.accesslog.infrastructure;

import com.rentacaresv.security.accesslog.application.AccessLogService;
import com.rentacaresv.user.domain.User;
import com.rentacaresv.user.infrastructure.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.authentication.event.LogoutSuccessEvent;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

/**
 * Listener de eventos de autenticación de Spring Security.
 * Captura login exitoso, login fallido y logout.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AuthenticationEventListener {

    private final AccessLogService accessLogService;
    private final UserRepository userRepository;

    /**
     * Captura evento de autenticación exitosa
     */
    @EventListener
    public void onAuthenticationSuccess(AuthenticationSuccessEvent event) {
        try {
            Object principal = event.getAuthentication().getPrincipal();
            String username;

            if (principal instanceof UserDetails) {
                username = ((UserDetails) principal).getUsername();
            } else {
                username = principal.toString();
            }

            User user = userRepository.findByUsername(username);
            if (user != null) {
                accessLogService.logLoginSuccess(user);
            } else {
                log.warn("Usuario no encontrado después de autenticación exitosa: {}", username);
            }
        } catch (Exception e) {
            log.error("Error registrando login exitoso: {}", e.getMessage());
        }
    }

    /**
     * Captura evento de autenticación fallida
     */
    @EventListener
    public void onAuthenticationFailure(AuthenticationFailureBadCredentialsEvent event) {
        try {
            String username = event.getAuthentication().getName();
            String reason = event.getException() != null ? 
                    event.getException().getMessage() : "Credenciales inválidas";

            accessLogService.logLoginFailed(username, reason);
        } catch (Exception e) {
            log.error("Error registrando login fallido: {}", e.getMessage());
        }
    }

    /**
     * Captura evento de logout exitoso
     */
    @EventListener
    public void onLogoutSuccess(LogoutSuccessEvent event) {
        try {
            Object principal = event.getAuthentication().getPrincipal();
            String username;

            if (principal instanceof UserDetails) {
                username = ((UserDetails) principal).getUsername();
            } else {
                username = principal.toString();
            }

            User user = userRepository.findByUsername(username);
            accessLogService.logLogout(user);
        } catch (Exception e) {
            log.error("Error registrando logout: {}", e.getMessage());
        }
    }
}
