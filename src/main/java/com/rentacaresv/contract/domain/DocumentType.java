package com.rentacaresv.contract.domain;

/**
 * Tipos de documento de identidad aceptados
 */
public enum DocumentType {
    
    DUI("Documento de Identidad", "Documento de Identidad"),
    PASSPORT("Pasaporte", "Pasaporte"),
    LICENSE("Licencia", "Licencia de Conducir"),
    RESIDENCE("Residencia", "Carnet de Residencia"),
    OTHER("Otro", "Otro documento");

    private final String code;
    private final String label;

    DocumentType(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }
}
