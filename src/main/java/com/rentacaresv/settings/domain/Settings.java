package com.rentacaresv.settings.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entidad Settings (Domain Layer)
 * Configuración global de la aplicación para cada tenant (cliente SaaS)
 */
@Entity
@Table(name = "settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Settings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    /**
     * Identificador único del tenant (cliente SaaS)
     * Se usa para organizar archivos en Digital Ocean Spaces
     */
    @Column(name = "tenant_id", unique = true, nullable = false, length = 100)
    private String tenantId;

    /**
     * Nombre de la empresa/cliente
     */
    @Column(name = "company_name", length = 200)
    private String companyName;

    /**
     * URL del logo de la empresa en Digital Ocean Spaces
     */
    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    /**
     * Indica si las carpetas ya fueron inicializadas en DO Spaces
     */
    @Column(name = "folders_initialized", nullable = false)
    @Builder.Default
    private Boolean foldersInitialized = false;

    // ========================================
    // Auditoría
    // ========================================

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ========================================
    // Métodos de Negocio
    // ========================================

    /**
     * Marca las carpetas como inicializadas
     */
    public void markFoldersAsInitialized() {
        this.foldersInitialized = true;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Actualiza la URL del logo
     */
    public void updateLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Hook de JPA para actualizar updatedAt
     */
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
