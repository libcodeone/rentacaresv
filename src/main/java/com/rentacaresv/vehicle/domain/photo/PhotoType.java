package com.rentacaresv.vehicle.domain.photo;

/**
 * Tipo de foto del vehículo
 */
public enum PhotoType {
    EXTERIOR("Exterior"),
    INTERIOR("Interior");

    private final String label;

    PhotoType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
