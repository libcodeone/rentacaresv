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
     * URL base de la aplicación (ej: https://rentacaresv.com)
     * Usado para generar enlaces en correos
     */
    @Column(name = "base_url", length = 500)
    private String baseUrl;

    /**
     * Indica si las carpetas ya fueron inicializadas en DO Spaces
     */
    @Column(name = "folders_initialized", nullable = false)
    private Boolean foldersInitialized = false;

    // ========================================
    // Configuración de Email (SMTP)
    // ========================================

    /**
     * Habilitar/deshabilitar envío de emails
     */
    @Column(name = "email_enabled")
    private Boolean emailEnabled = true;

    /**
     * Servidor SMTP (ej: smtp.hostinger.com, smtp.gmail.com)
     */
    @Column(name = "mail_host", length = 100)
    private String mailHost;

    /**
     * Puerto SMTP (ej: 587, 465, 25)
     */
    @Column(name = "mail_port")
    private Integer mailPort;

    /**
     * Usuario/email para autenticación SMTP
     */
    @Column(name = "mail_username", length = 200)
    private String mailUsername;

    /**
     * Contraseña para autenticación SMTP
     */
    @Column(name = "mail_password", length = 200)
    private String mailPassword;

    /**
     * Email que aparece como remitente (From)
     */
    @Column(name = "mail_from", length = 200)
    private String mailFrom;

    /**
     * Nombre que aparece como remitente
     */
    @Column(name = "mail_from_name", length = 200)
    private String mailFromName;

    /**
     * Habilitar autenticación SMTP
     */
    @Column(name = "mail_smtp_auth")
    private Boolean mailSmtpAuth = true;

    /**
     * Habilitar STARTTLS
     */
    @Column(name = "mail_starttls_enable")
    private Boolean mailStarttlsEnable = true;

    /**
     * Habilitar SSL (para puerto 465)
     */
    @Column(name = "mail_ssl_enable")
    private Boolean mailSslEnable = false;

    /**
     * Protocolo de transporte (smtp)
     */
    @Column(name = "mail_protocol", length = 20)
    private String mailProtocol = "smtp";

    /**
     * Timeout de conexión en milisegundos
     */
    @Column(name = "mail_connection_timeout")
    private Integer mailConnectionTimeout = 10000;

    /**
     * Timeout de lectura en milisegundos
     */
    @Column(name = "mail_timeout")
    private Integer mailTimeout = 10000;

    // ========================================
    // Configuración de Google Calendar
    // ========================================

    /**
     * Habilitar integración con Google Calendar
     */
    @Column(name = "google_calendar_enabled")
    private Boolean googleCalendarEnabled = false;

    /**
     * Google OAuth2 Client ID
     */
    @Column(name = "google_client_id", length = 500)
    private String googleClientId;

    /**
     * Google OAuth2 Client Secret
     */
    @Column(name = "google_client_secret", length = 500)
    private String googleClientSecret;

    /**
     * ID del usuario cuyo token de Google Calendar es el "calendario de la empresa".
     * Cuando un admin vincula su cuenta, se guarda aquí para que todos los usuarios
     * vean ese calendario sin necesidad de vincular sus propias cuentas.
     */
    @Column(name = "company_calendar_user_id")
    private Long companyCalendarUserId;

    // ========================================
    // Tarifas especiales de renta
    // ========================================

    /**
     * Tarifa diaria adicional cuando el cliente saca el vehículo fuera del país.
     * Se suma al precio base por cada día fuera del país declarado.
     */
    @Column(name = "tarifa_sacar_pais", precision = 10, scale = 2)
    private java.math.BigDecimal tarifaSacarPais;

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

    public java.math.BigDecimal getTarifaSacarPais() {
        return this.tarifaSacarPais;
    }

    public void setTarifaSacarPais(java.math.BigDecimal tarifaSacarPais) {
        this.tarifaSacarPais = tarifaSacarPais;
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

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
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

    // ========================================
    // Getters/Setters Email
    // ========================================

    public Boolean getEmailEnabled() {
        return emailEnabled;
    }

    public void setEmailEnabled(Boolean emailEnabled) {
        this.emailEnabled = emailEnabled;
    }

    public String getMailHost() {
        return mailHost;
    }

    public void setMailHost(String mailHost) {
        this.mailHost = mailHost;
    }

    public Integer getMailPort() {
        return mailPort;
    }

    public void setMailPort(Integer mailPort) {
        this.mailPort = mailPort;
    }

    public String getMailUsername() {
        return mailUsername;
    }

    public void setMailUsername(String mailUsername) {
        this.mailUsername = mailUsername;
    }

    public String getMailPassword() {
        return mailPassword;
    }

    public void setMailPassword(String mailPassword) {
        this.mailPassword = mailPassword;
    }

    public String getMailFrom() {
        return mailFrom;
    }

    public void setMailFrom(String mailFrom) {
        this.mailFrom = mailFrom;
    }

    public String getMailFromName() {
        return mailFromName;
    }

    public void setMailFromName(String mailFromName) {
        this.mailFromName = mailFromName;
    }

    public Boolean getMailSmtpAuth() {
        return mailSmtpAuth;
    }

    public void setMailSmtpAuth(Boolean mailSmtpAuth) {
        this.mailSmtpAuth = mailSmtpAuth;
    }

    public Boolean getMailStarttlsEnable() {
        return mailStarttlsEnable;
    }

    public void setMailStarttlsEnable(Boolean mailStarttlsEnable) {
        this.mailStarttlsEnable = mailStarttlsEnable;
    }

    public Boolean getMailSslEnable() {
        return mailSslEnable;
    }

    public void setMailSslEnable(Boolean mailSslEnable) {
        this.mailSslEnable = mailSslEnable;
    }

    public String getMailProtocol() {
        return mailProtocol;
    }

    public void setMailProtocol(String mailProtocol) {
        this.mailProtocol = mailProtocol;
    }

    public Integer getMailConnectionTimeout() {
        return mailConnectionTimeout;
    }

    public void setMailConnectionTimeout(Integer mailConnectionTimeout) {
        this.mailConnectionTimeout = mailConnectionTimeout;
    }

    public Integer getMailTimeout() {
        return mailTimeout;
    }

    public void setMailTimeout(Integer mailTimeout) {
        this.mailTimeout = mailTimeout;
    }

    /**
     * Verifica si la configuración de email está completa
     */
    public boolean isEmailConfigured() {
        return mailHost != null && !mailHost.isEmpty() &&
               mailPort != null &&
               mailUsername != null && !mailUsername.isEmpty() &&
               mailPassword != null && !mailPassword.isEmpty();
    }

    // ========================================
    // Getters/Setters Google Calendar
    // ========================================

    public Boolean getGoogleCalendarEnabled() {
        return googleCalendarEnabled;
    }

    public void setGoogleCalendarEnabled(Boolean googleCalendarEnabled) {
        this.googleCalendarEnabled = googleCalendarEnabled;
    }

    public String getGoogleClientId() {
        return googleClientId;
    }

    public void setGoogleClientId(String googleClientId) {
        this.googleClientId = googleClientId;
    }

    public String getGoogleClientSecret() {
        return googleClientSecret;
    }

    public void setGoogleClientSecret(String googleClientSecret) {
        this.googleClientSecret = googleClientSecret;
    }

    /**
     * Verifica si la configuración de Google Calendar está completa
     */
    public boolean isGoogleCalendarConfigured() {
        return Boolean.TRUE.equals(googleCalendarEnabled) &&
               googleClientId != null && !googleClientId.isEmpty() &&
               googleClientSecret != null && !googleClientSecret.isEmpty();
    }

    public Long getCompanyCalendarUserId() {
        return companyCalendarUserId;
    }

    public void setCompanyCalendarUserId(Long companyCalendarUserId) {
        this.companyCalendarUserId = companyCalendarUserId;
    }

    /**
     * Verifica si hay un calendario de empresa configurado
     */
    public boolean hasCompanyCalendar() {
        return companyCalendarUserId != null;
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
