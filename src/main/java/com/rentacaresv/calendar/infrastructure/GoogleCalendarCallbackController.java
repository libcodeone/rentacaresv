package com.rentacaresv.calendar.infrastructure;

import com.rentacaresv.calendar.application.GoogleCalendarService;
import com.rentacaresv.user.domain.User;
import com.rentacaresv.user.infrastructure.UserRepository;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Optional;

/**
 * Controlador para manejar el callback de OAuth2 de Google
 */
@RestController
@RequestMapping("/api/google-calendar")
@RequiredArgsConstructor
@Slf4j
public class GoogleCalendarCallbackController {

    private final GoogleCalendarService googleCalendarService;
    private final UserRepository userRepository;

    /**
     * Callback de OAuth2 de Google
     * Google redirige aquí después de que el usuario autoriza la aplicación
     */
    @GetMapping("/callback")
    public void handleCallback(
            @RequestParam(value = "code", required = false) String code,
            @RequestParam(value = "error", required = false) String error,
            @RequestParam(value = "state", required = false) String state,
            HttpServletResponse response) throws IOException {

        if (error != null) {
            log.warn("Error en autorización de Google: {}", error);
            response.sendRedirect("/calendar?error=auth_denied");
            return;
        }

        if (code == null || state == null) {
            log.warn("Callback incompleto: code={}, state={}", code, state);
            response.sendRedirect("/calendar?error=invalid_callback");
            return;
        }

        try {
            // El state contiene el userId
            Long userId = Long.parseLong(state);
            Optional<User> userOpt = userRepository.findById(userId);

            if (userOpt.isEmpty()) {
                log.warn("Usuario no encontrado: {}", userId);
                response.sendRedirect("/calendar?error=user_not_found");
                return;
            }

            // Procesar el token
            googleCalendarService.handleCallback(code, userOpt.get());
            
            log.info("Google Calendar vinculado exitosamente para usuario {}", userId);
            response.sendRedirect("/calendar?success=linked");

        } catch (NumberFormatException e) {
            log.error("State inválido: {}", state);
            response.sendRedirect("/calendar?error=invalid_state");
        } catch (Exception e) {
            log.error("Error procesando callback de Google: {}", e.getMessage(), e);
            response.sendRedirect("/calendar?error=processing_error");
        }
    }
}
