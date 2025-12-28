package com.rentacaresv.rental.domain;

/**
 * Estado del proceso de renta
 */
public enum RentalStatus {
    /**
     * Renta creada, esperando entrega del vehículo
     */
    PENDING,
    
    /**
     * Vehículo entregado al cliente, renta activa
     */
    ACTIVE,
    
    /**
     * Vehículo devuelto, renta completada
     */
    COMPLETED,
    
    /**
     * Renta cancelada antes de la entrega
     */
    CANCELLED;

    public String getLabel() {
        return switch (this) {
            case PENDING -> "Pendiente";
            case ACTIVE -> "Activa";
            case COMPLETED -> "Completada";
            case CANCELLED -> "Cancelada";
        };
    }

    public boolean isActive() {
        return this == ACTIVE;
    }

    public boolean canBeModified() {
        return this == PENDING;
    }

    public boolean canBeDelivered() {
        return this == PENDING;
    }

    public boolean canBeReturned() {
        return this == ACTIVE;
    }
}
