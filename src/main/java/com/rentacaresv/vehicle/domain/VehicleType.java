package com.rentacaresv.vehicle.domain;

/**
 * Tipos de vehículo
 */
public enum VehicleType {
    SEDAN("Sedán"),
    SUV("SUV"),
    PICKUP("Pick Up"),
    HATCHBACK("Hatchback"),
    COUPE("Coupé"),
    MINIVAN("Minivan"),
    MICROBUS("Microbús"),
    TRUCK("Camión"),
    MOTORCYCLE("Motocicleta"),
    OTHER("Otro");

    private final String label;

    VehicleType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
