package com.rentacaresv.calendar.domain;

import com.rentacaresv.user.domain.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entidad para almacenar tokens de Google OAuth2 por usuario
 */
@Entity
@Table(name = "google_calendar_token")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoogleCalendarToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Usuario dueño del token
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    /**
     * Access token para llamadas a la API
     */
    @Column(name = "access_token", nullable = false, length = 2000)
    private String accessToken;

    /**
     * Refresh token para renovar el access token
     */
    @Column(name = "refresh_token", length = 2000)
    private String refreshToken;

    /**
     * Fecha de expiración del access token
     */
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    /**
     * Email de la cuenta de Google vinculada
     */
    @Column(name = "google_email", length = 200)
    private String googleEmail;

    /**
     * ID del calendario a usar (primary por defecto)
     */
    @Column(name = "calendar_id", length = 200)
    @Builder.Default
    private String calendarId = "primary";

    /**
     * Indica si la sincronización está activa
     */
    @Column(name = "sync_enabled")
    @Builder.Default
    private Boolean syncEnabled = true;

    /**
     * Fecha de creación
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    /**
     * Fecha de última actualización
     */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Verifica si el token ha expirado
     */
    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    /**
     * Verifica si el token necesita renovarse pronto (5 minutos antes)
     */
    public boolean needsRefresh() {
        return expiresAt != null && LocalDateTime.now().plusMinutes(5).isAfter(expiresAt);
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
