package com.rentacaresv.payment.domain;

/**
 * Método de pago
 */
public enum PaymentMethod {
    /**
     * Efectivo
     */
    CASH,
    
    /**
     * Tarjeta de crédito/débito
     */
    CARD,
    
    /**
     * Transferencia bancaria
     */
    BANK_TRANSFER,
    
    /**
     * Cheque
     */
    CHECK;

    public String getLabel() {
        return switch (this) {
            case CASH -> "Efectivo";
            case CARD -> "Tarjeta";
            case BANK_TRANSFER -> "Transferencia";
            case CHECK -> "Cheque";
        };
    }
}
