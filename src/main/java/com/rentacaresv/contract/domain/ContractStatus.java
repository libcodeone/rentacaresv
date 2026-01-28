package com.rentacaresv.contract.domain;

/**
 * Estados posibles de un contrato digital
 */
public enum ContractStatus {
    
    /**
     * Contrato creado, pendiente de firma del cliente
     */
    PENDING("Pendiente"),
    
    /**
     * Contrato firmado por el cliente
     */
    SIGNED("Firmado"),
    
    /**
     * Contrato expirado (no se firmó a tiempo)
     */
    EXPIRED("Expirado"),
    
    /**
     * Contrato cancelado
     */
    CANCELLED("Cancelado");

    private final String label;

    ContractStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /**
     * Verifica si el contrato puede ser firmado
     */
    public boolean canBeSigned() {
        return this == PENDING;
    }

    /**
     * Verifica si el contrato puede ser cancelado
     */
    public boolean canBeCancelled() {
        return this == PENDING;
    }
}
