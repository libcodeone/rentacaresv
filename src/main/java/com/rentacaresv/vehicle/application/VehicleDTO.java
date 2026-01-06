package com.rentacaresv.vehicle.application;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO para transferir datos de Vehicle
 * Se usa para enviar información del vehículo a la capa de presentación
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleDTO {
    
    private Long id;
    private String licensePlate;
    private String brand;
    private String model;
    private Integer year;
    private String color;
    private String transmissionType;
    private String fuelType;
    private String vehicleType;
    private String vehicleTypeLabel;
    private Integer passengerCapacity;
    private Integer mileage;
    
    // Precios
    private BigDecimal priceNormal;
    private BigDecimal priceVip;
    private BigDecimal priceMoreThan15Days;
    private BigDecimal priceMonthly;
    
    // Estado
    private String status;
    private String notes;
    
    // Campos calculados
    private String fullDescription;
    private Boolean isAvailable;
}
