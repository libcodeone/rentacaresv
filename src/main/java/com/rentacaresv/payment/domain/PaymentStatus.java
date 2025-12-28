package com.rentacaresv.payment.domain;

/**
 * Estado del pago
 */
public enum PaymentStatus {
    /**
     * Pago completado exitosamente
     */
    COMPLETED,
    
    /**
     * Pago pendiente de confirmación
     */
    PENDING,
    
    /**
     * Pago rechazado
     */
    REJECTED,
    
    /**
     * Pago reembolsado
     */
    REFUNDED;

    public String getLabel() {
        return switch (this) {
            case COMPLETED -> "Completado";
            case PENDING -> "Pendiente";
            case REJECTED -> "Rechazado";
            case REFUNDED -> "Reembolsado";
        };
    }

    public boolean isSuccessful() {
        return this == COMPLETED;
    }
}
