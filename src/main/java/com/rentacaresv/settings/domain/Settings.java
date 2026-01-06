package com.rentacaresv.settings.domain;

import jakarta.persistence.*;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Entidad Settings (Domain Layer)
 * Configuración global de la aplicación para cada tenant (cliente SaaS)
 */
@Entity
@Table(name = "settings")
public class Settings implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Serial id
    private static final long serialVersionUID = 1L;

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
    private Boolean foldersInitialized = false;

    // ========================================
    // Auditoría
    // ========================================

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ========================================
    // Getters/Setters explícitos (workaround Lombok)
    // ========================================

    /**
     * Obtiene el tenant ID
     * IMPORTANTE: Getter explícito para evitar problemas con Lombok
     */
    public String getTenantId() {
        return this.tenantId;
    }

    /**
     * Establece el tenant ID
     * IMPORTANTE: Setter explícito para evitar problemas con Lombok
     */
    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

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

    /**
     * Getters and setters
     */

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

    public Boolean getFoldersInitialized() {
        return foldersInitialized;
    }

    public void setFoldersInitialized(Boolean foldersInitialized) {
        this.foldersInitialized = foldersInitialized;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Settings(Long id, String tenantId, String companyName, String logoUrl, Boolean foldersInitialized,
            LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.tenantId = tenantId;
        this.companyName = companyName;
        this.logoUrl = logoUrl;
        this.foldersInitialized = foldersInitialized;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Settings() {
    }

    @Override
    public boolean equals(Object obj) {
        // TODO Auto-generated method stub
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        // TODO Auto-generated method stub
        return super.hashCode();
    }

    @Override
    public String toString() {
        return "Settings{" +
                "id=" + id +
                ", tenantId='" + tenantId + '\'' +
                ", companyName='" + companyName + '\'' +
                ", logoUrl='" + logoUrl + '\'' +
                ", foldersInitialized=" + foldersInitialized +
                '}';
    }
}
