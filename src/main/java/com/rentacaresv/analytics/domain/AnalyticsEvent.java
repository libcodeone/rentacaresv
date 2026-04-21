package com.rentacaresv.analytics.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "analytics_event", indexes = {
        @Index(name = "idx_ae_event_type", columnList = "event_type"),
        @Index(name = "idx_ae_created_at",  columnList = "created_at"),
        @Index(name = "idx_ae_session_id",  columnList = "session_id"),
        @Index(name = "idx_ae_vehicle_id",  columnList = "vehicle_id")
})
@Getter @Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Tipo de evento: page_view, vehicle_click, vehicle_detail,
     *  whatsapp_click, reserve_click, reservation_step, reservation_complete */
    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Column(name = "page_path", length = 300)
    private String pagePath;

    @Column(name = "vehicle_id")
    private Long vehicleId;

    @Column(name = "vehicle_name", length = 200)
    private String vehicleName;

    /** Para reservation_step: número de paso completado (1-based) */
    @Column(name = "step_number")
    private Integer stepNumber;

    @Column(name = "step_name", length = 100)
    private String stepName;

    /** Para whatsapp_click: vehicle_detail | floating_button | reservation_success */
    @Column(name = "source", length = 50)
    private String source;

    @Column(name = "contract_number", length = 50)
    private String contractNumber;

    @Column(name = "total_amount", precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "session_id", length = 100)
    private String sessionId;

    @Column(name = "ip", length = 50)
    private String ip;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
