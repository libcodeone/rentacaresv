package com.rentacaresv.customer.domain;

/**
 * Tipo de documento de identificación
 */
public enum DocumentType {
    /**
     * Documento Único de Identidad (El Salvador)
     */
    DUI,
    
    /**
     * Pasaporte
     */
    PASSPORT,
    
    /**
     * Licencia de conducir
     */
    DRIVERS_LICENSE,
    
    /**
     * Otro tipo de documento
     */
    OTHER;

    public String getLabel() {
        return switch (this) {
            case DUI -> "DUI";
            case PASSPORT -> "Pasaporte";
            case DRIVERS_LICENSE -> "Licencia de Conducir";
            case OTHER -> "Otro";
        };
    }
}
