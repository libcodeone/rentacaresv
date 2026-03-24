package com.rentacaresv.vehicle.application;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * DTO público para exponer vehículos en la API pública.
 * Solo contiene información relevante para el cliente final (página web).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicVehicleDTO {

    private Long id;
    private String brand;
    private String model;
    private Integer year;
    private String color;
    private String transmissionType;
    private String fuelType;
    private String vehicleType;
    private String vehicleTypeLabel;
    private Integer passengerCapacity;

    /**
     * Precio por día (precio normal)
     */
    private BigDecimal pricePerDay;

    /**
     * URL de la foto principal del vehículo
     */
    private String primaryPhotoUrl;

    /**
     * Todas las fotos del vehículo
     */
    private List<VehiclePhotoDTO> photos;

    /**
     * Descripción completa: "Brand Model Year - Placa"
     */
    private String fullDescription;

    // ========================================
    // Disponibilidad
    // ========================================

    /**
     * Si el vehículo está disponible AHORA (no tiene renta activa hoy)
     */
    private Boolean availableNow;

    /**
     * Si no está disponible ahora, cuándo estará disponible
     */
    private LocalDate availableFrom;

    /**
     * Períodos reservados (bloqueados) del vehículo
     */
    private List<ReservedPeriodDTO> reservedPeriods;

    // ========================================
    // DTOs internos
    // ========================================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VehiclePhotoDTO {
        private Long id;
        private String photoUrl;
        private String photoType;
        private Boolean isPrimary;
        private Integer displayOrder;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReservedPeriodDTO {
        private LocalDate startDate;
        private LocalDate endDate;
    }
}
