package com.rentacaresv.security.accesslog.domain;

import com.rentacaresv.user.domain.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entidad para registro de accesos al sistema (Domain Layer)
 * Almacena eventos de login, logout y otros eventos de seguridad.
 */
@Entity
@Table(name = "access_log", indexes = {
    @Index(name = "idx_access_log_user", columnList = "user_id"),
    @Index(name = "idx_access_log_timestamp", columnList = "timestamp"),
    @Index(name = "idx_access_log_event_type", columnList = "event_type"),
    @Index(name = "idx_access_log_username", columnList = "username")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class AccessLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "username", nullable = false, length = 50)
    private String username;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    private AccessEventType eventType;

    @Column(name = "timestamp", nullable = false)
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "session_id", length = 100)
    private String sessionId;

    @Column(name = "details", length = 1000)
    private String details;

    @Column(name = "success")
    @Builder.Default
    private Boolean success = true;

    // ========================================
    // Métodos de Fábrica
    // ========================================

    /**
     * Crea un registro de login exitoso
     */
    public static AccessLog loginSuccess(User user, String ipAddress, String userAgent, String sessionId) {
        return AccessLog.builder()
                .user(user)
                .username(user.getUsername())
                .eventType(AccessEventType.LOGIN_SUCCESS)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .sessionId(sessionId)
                .success(true)
                .details("Inicio de sesión exitoso")
                .build();
    }

    /**
     * Crea un registro de login fallido
     */
    public static AccessLog loginFailed(String username, String ipAddress, String userAgent, String reason) {
        return AccessLog.builder()
                .username(username)
                .eventType(AccessEventType.LOGIN_FAILED)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .success(false)
                .details(reason != null ? reason : "Credenciales inválidas")
                .build();
    }

    /**
     * Crea un registro de logout
     */
    public static AccessLog logout(User user, String ipAddress, String sessionId) {
        return AccessLog.builder()
                .user(user)
                .username(user != null ? user.getUsername() : "unknown")
                .eventType(AccessEventType.LOGOUT)
                .ipAddress(ipAddress)
                .sessionId(sessionId)
                .success(true)
                .details("Cierre de sesión")
                .build();
    }

    /**
     * Crea un registro de sesión expirada
     */
    public static AccessLog sessionExpired(User user, String sessionId) {
        return AccessLog.builder()
                .user(user)
                .username(user != null ? user.getUsername() : "unknown")
                .eventType(AccessEventType.SESSION_EXPIRED)
                .sessionId(sessionId)
                .success(true)
                .details("Sesión expirada por inactividad")
                .build();
    }

    // ========================================
    // Métodos de Negocio
    // ========================================

    /**
     * Verifica si el evento es de tipo login
     */
    public boolean isLoginEvent() {
        return eventType == AccessEventType.LOGIN_SUCCESS || eventType == AccessEventType.LOGIN_FAILED;
    }

    /**
     * Verifica si el evento fue exitoso
     */
    public boolean isSuccessful() {
        return Boolean.TRUE.equals(success);
    }

    /**
     * Obtiene una descripción legible del evento
     */
    public String getReadableDescription() {
        StringBuilder sb = new StringBuilder();
        sb.append(eventType.getDescription());
        
        if (ipAddress != null && !ipAddress.isEmpty()) {
            sb.append(" desde ").append(ipAddress);
        }
        
        return sb.toString();
    }

    @PrePersist
    protected void onCreate() {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
    }
}
