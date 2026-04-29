package com.rentacaresv.security.permission.domain;

/**
 * Enum de permisos del sistema RentaCar ESV.
 * Define todas las acciones que pueden ser controladas por rol.
 */
public enum Permission {
    
    // ========================================
    // Permisos de Vehículos
    // ========================================
    VEHICLE_VIEW("Vehículos", "Ver vehículos", "Permite ver el listado de vehículos"),
    VEHICLE_CREATE("Vehículos", "Crear vehículos", "Permite agregar nuevos vehículos"),
    VEHICLE_EDIT("Vehículos", "Editar vehículos", "Permite modificar información de vehículos"),
    VEHICLE_DELETE("Vehículos", "Eliminar vehículos", "Permite eliminar vehículos del sistema"),
    
    // ========================================
    // Permisos de Clientes
    // ========================================
    CUSTOMER_VIEW("Clientes", "Ver clientes", "Permite ver el listado de clientes"),
    CUSTOMER_CREATE("Clientes", "Crear clientes", "Permite agregar nuevos clientes"),
    CUSTOMER_EDIT("Clientes", "Editar clientes", "Permite modificar información de clientes"),
    CUSTOMER_DELETE("Clientes", "Eliminar clientes", "Permite eliminar clientes del sistema"),
    
    // ========================================
    // Permisos de Rentas
    // ========================================
    RENTAL_VIEW("Rentas", "Ver rentas", "Permite ver el listado de rentas"),
    RENTAL_CREATE("Rentas", "Crear rentas", "Permite crear nuevas rentas"),
    RENTAL_EDIT("Rentas", "Editar rentas", "Permite modificar información de rentas"),
    RENTAL_DELETE("Rentas", "Eliminar rentas", "Permite eliminar rentas del sistema"),
    RENTAL_DELIVER("Rentas", "Entregar vehículos", "Permite registrar la entrega de un vehículo al cliente"),
    RENTAL_COMPLETE("Rentas", "Completar rentas", "Permite marcar rentas como completadas"),
    RENTAL_CANCEL("Rentas", "Cancelar rentas", "Permite cancelar rentas activas"),
    
    // ========================================
    // Permisos de Contratos
    // ========================================
    CONTRACT_VIEW("Contratos", "Ver contratos", "Permite ver contratos"),
    CONTRACT_CREATE("Contratos", "Crear contratos", "Permite generar contratos"),
    CONTRACT_EDIT("Contratos", "Editar contratos", "Permite modificar contratos"),
    CONTRACT_DELETE("Contratos", "Eliminar contratos", "Permite eliminar contratos"),
    CONTRACT_SIGN("Contratos", "Firmar contratos", "Permite gestionar firmas de contratos"),
    CONTRACT_DOWNLOAD("Contratos", "Descargar contratos", "Permite descargar PDFs de contratos"),
    
    // ========================================
    // Permisos de Pagos
    // ========================================
    PAYMENT_VIEW("Pagos", "Ver pagos", "Permite ver el historial de pagos"),
    PAYMENT_CREATE("Pagos", "Registrar pagos", "Permite registrar nuevos pagos"),
    PAYMENT_EDIT("Pagos", "Editar pagos", "Permite modificar información de pagos"),
    PAYMENT_DELETE("Pagos", "Eliminar pagos", "Permite eliminar pagos del sistema"),
    PAYMENT_REFUND("Pagos", "Reembolsar pagos", "Permite procesar reembolsos"),
    
    // ========================================
    // Permisos de Catálogos
    // ========================================
    CATALOG_VIEW("Catálogos", "Ver catálogos", "Permite ver catálogos del sistema"),
    CATALOG_MANAGE("Catálogos", "Gestionar catálogos", "Permite crear, editar y eliminar ítems de catálogos"),
    
    // ========================================
    // Permisos de Calendario
    // ========================================
    CALENDAR_VIEW("Calendario", "Ver calendario", "Permite ver el calendario de rentas"),
    CALENDAR_SYNC("Calendario", "Sincronizar calendario", "Permite vincular con Google Calendar"),
    
    // ========================================
    // Permisos de Reportes
    // ========================================
    REPORT_VIEW("Reportes", "Ver reportes", "Permite ver reportes del sistema"),
    REPORT_EXPORT("Reportes", "Exportar reportes", "Permite exportar reportes a PDF/Excel"),
    
    // ========================================
    // Permisos de Usuarios
    // ========================================
    USER_VIEW("Usuarios", "Ver usuarios", "Permite ver el listado de usuarios"),
    USER_CREATE("Usuarios", "Crear usuarios", "Permite crear nuevos usuarios"),
    USER_EDIT("Usuarios", "Editar usuarios", "Permite modificar información de usuarios"),
    USER_DELETE("Usuarios", "Eliminar usuarios", "Permite eliminar usuarios del sistema"),
    USER_ROLES("Usuarios", "Gestionar roles", "Permite asignar roles a usuarios"),
    
    // ========================================
    // Permisos de Configuración
    // ========================================
    SETTINGS_VIEW("Configuración", "Ver configuración", "Permite ver la configuración del sistema"),
    SETTINGS_EDIT("Configuración", "Editar configuración", "Permite modificar la configuración del sistema"),
    
    // ========================================
    // Permisos de Seguridad
    // ========================================
    SECURITY_ACCESS_LOG("Seguridad", "Ver registro de acceso", "Permite ver el log de accesos al sistema"),
    SECURITY_ROLES("Seguridad", "Gestionar roles", "Permite crear y modificar roles del sistema"),
    SECURITY_PERMISSIONS("Seguridad", "Gestionar permisos", "Permite asignar permisos a roles");

    private final String category;
    private final String displayName;
    private final String description;

    Permission(String category, String displayName, String description) {
        this.category = category;
        this.displayName = displayName;
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }
}
