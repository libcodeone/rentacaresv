package com.rentacaresv.customer.domain;

/**
 * Categoría del cliente
 * Determina los precios que se le aplican
 */
public enum CustomerCategory {
    /**
     * Cliente normal - precio estándar
     */
    NORMAL,
    
    /**
     * Cliente VIP - precio especial con descuento
     */
    VIP;

    /**
     * Verifica si el cliente es VIP
     */
    public boolean isVip() {
        return this == VIP;
    }
}
