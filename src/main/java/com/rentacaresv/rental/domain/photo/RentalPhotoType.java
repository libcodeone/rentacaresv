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
    RETURN_ACCESSORIES("Devolución - Accesorios");

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
