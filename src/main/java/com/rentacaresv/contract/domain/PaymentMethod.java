package com.rentacaresv.contract.domain;

/**
 * Enum para las formas de pago del contrato
 */
public enum PaymentMethod {
    EFECTIVO("Efectivo"),
    TARJETA("Tarjeta"),
    TRANSFERENCIA("Transferencia");

    private final String label;

    PaymentMethod(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
