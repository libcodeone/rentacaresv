package com.rentacaresv.analytics.infrastructure.api;

import com.rentacaresv.analytics.application.AnalyticsService;
import com.rentacaresv.analytics.domain.AnalyticsEvent;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * Endpoint público — recibe eventos de analíticas desde el frontend web.
 * No requiere autenticación. CSRF ya exento por /api/public/**.
 */
@RestController
@RequestMapping("/api/public/analytics")
@RequiredArgsConstructor
@Slf4j
public class PublicAnalyticsController {

    private final AnalyticsService analyticsService;

    @PostMapping("/event")
    public ResponseEntity<Void> trackEvent(
            @Valid @RequestBody EventRequest body,
            HttpServletRequest request) {

        // Ignorar bots / requests sin sessionId
        if (body.getSessionId() == null || body.getSessionId().isBlank()) {
            return ResponseEntity.ok().build();
        }

        AnalyticsEvent event = AnalyticsEvent.builder()
                .eventType(body.getEventType())
                .pagePath(body.getPagePath())
                .vehicleId(body.getVehicleId())
                .vehicleName(body.getVehicleName())
                .stepNumber(body.getStepNumber())
                .stepName(body.getStepName())
                .source(body.getSource())
                .contractNumber(body.getContractNumber())
                .totalAmount(body.getTotalAmount())
                .sessionId(body.getSessionId())
                .ip(extractIp(request))
                .build();

        analyticsService.save(event);
        return ResponseEntity.ok().build();
    }

    // ─── DTO interno ───────────────────────────────────────────────────────────

    @Data
    public static class EventRequest {
        @NotBlank @Size(max = 50)
        private String eventType;

        @Size(max = 300)
        private String pagePath;

        private Long vehicleId;

        @Size(max = 200)
        private String vehicleName;

        private Integer stepNumber;

        @Size(max = 100)
        private String stepName;

        @Size(max = 50)
        private String source;

        @Size(max = 50)
        private String contractNumber;

        private BigDecimal totalAmount;

        @Size(max = 100)
        private String sessionId;
    }

    // ─── Helpers ───────────────────────────────────────────────────────────────

    private String extractIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) return xff.split(",")[0].trim();
        String xri = request.getHeader("X-Real-IP");
        if (xri != null && !xri.isBlank()) return xri;
        return request.getRemoteAddr();
    }
}
