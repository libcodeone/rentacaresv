package com.rentacaresv.rental.domain.photo;

/**
 * Tipo de foto de la renta
 */
public enum RentalPhotoType {
    DELIVERY_EXTERIOR("Entrega - Exterior"),
    DELIVERY_INTERIOR("Entrega - Interior"),
    DELIVERY_ACCESSORIES("Entrega - Accesorios"),
    RETURN_EXTERIOR("Devolución - Exterior"),
    RETURN_INTERIOR("Devolución - Interior"),
    RETURN_ACCESSORIES("Devolución - Accesorios"),
    DOCUMENT_ID_FRONT("Documento de Identidad - Frente"),
    DOCUMENT_ID_BACK("Documento de Identidad - Reverso"),
    DOCUMENT_LICENSE_FRONT("Licencia de Conducir - Frente"),
    DOCUMENT_LICENSE_BACK("Licencia de Conducir - Reverso");

    private final String label;

    RentalPhotoType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public boolean isDelivery() {
        return this.name().startsWith("DELIVERY");
    }

    public boolean isReturn() {
        return this.name().startsWith("RETURN");
    }
}
