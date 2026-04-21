package com.rentacaresv.rental.infrastructure.api;

import com.rentacaresv.rental.application.PublicReservationDTO;
import com.rentacaresv.rental.application.PublicReservationService;
import com.rentacaresv.settings.application.SettingsCache;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.Map;

/**
 * API REST pública para reservas desde la web.
 * No requiere autenticación. Incluye protección anti-bot y rate limiting.
 */
@RestController
@RequestMapping("/api/public/reservations")
@RequiredArgsConstructor
@Slf4j
public class PublicReservationController {

    private final PublicReservationService reservationService;
    private final SettingsCache settingsCache;

    /**
     * Crea una nueva reserva desde la web.
     * POST /api/public/reservations
     */
    @PostMapping
    public ResponseEntity<?> createReservation(
            @Valid @RequestPart("reservation") PublicReservationDTO dto,
            @RequestPart("documentFront") MultipartFile documentFront,
            @RequestPart("documentBack") MultipartFile documentBack,
            @RequestPart("licenseFront") MultipartFile licenseFront,
            @RequestPart("licenseBack") MultipartFile licenseBack,
            HttpServletRequest request) {
        try {
            String ip = getClientIp(request);
            String contractNumber = reservationService.processReservation(
                    dto,
                    ip,
                    documentFront,
                    documentBack,
                    licenseFront,
                    licenseBack
            );

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "contractNumber", contractNumber,
                    "message", "Reserva creada exitosamente. Nos pondremos en contacto contigo pronto."
            ));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Error procesando reserva web: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "Error al procesar la reserva. Intente de nuevo."
            ));
        }
    }

    /**
     * Configuración pública para el formulario de reserva web.
     * GET /api/public/reservations/config
     */
    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> getPublicConfig() {
        BigDecimal tarifa = settingsCache.getSettings().getTarifaSacarPais();
        return ResponseEntity.ok(Map.of(
                "tarifaSacarPais", tarifa != null ? tarifa : BigDecimal.ZERO
        ));
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }
}
