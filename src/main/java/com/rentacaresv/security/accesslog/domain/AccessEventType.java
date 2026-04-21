package com.rentacaresv.security.accesslog.domain;

/**
 * Tipos de eventos de acceso al sistema
 */
public enum AccessEventType {
    LOGIN_SUCCESS("Inicio de sesión exitoso"),
    LOGIN_FAILED("Inicio de sesión fallido"),
    LOGOUT("Cierre de sesión"),
    SESSION_EXPIRED("Sesión expirada"),
    PASSWORD_CHANGE("Cambio de contraseña"),
    PASSWORD_RESET_REQUEST("Solicitud de restablecimiento de contraseña"),
    ACCOUNT_LOCKED("Cuenta bloqueada"),
    ACCOUNT_UNLOCKED("Cuenta desbloqueada");

    private final String description;

    AccessEventType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
