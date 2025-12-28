package com.rentacaresv.vehicle.domain;

/**
 * Estado actual del vehículo en el sistema
 */
public enum VehicleStatus {
    /**
     * Vehículo disponible para rentar
     */
    AVAILABLE,
    
    /**
     * Vehículo actualmente rentado
     */
    RENTED,
    
    /**
     * Vehículo en mantenimiento
     */
    MAINTENANCE,
    
    /**
     * Vehículo fuera de servicio
     */
    OUT_OF_SERVICE;

    /**
     * Verifica si el vehículo está disponible para rentar
     */
    public boolean isAvailableForRent() {
        return this == AVAILABLE;
    }
}
