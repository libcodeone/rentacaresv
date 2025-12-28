package com.rentacaresv.vehicle.application;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Command para crear un nuevo vehículo
 * Contiene validaciones de entrada
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateVehicleCommand {

    @NotBlank(message = "La placa es obligatoria")
    @Size(max = 20, message = "La placa no puede tener más de 20 caracteres")
    private String licensePlate;

    @NotBlank(message = "La marca es obligatoria")
    @Size(max = 50, message = "La marca no puede tener más de 50 caracteres")
    private String brand;

    @NotBlank(message = "El modelo es obligatorio")
    @Size(max = 50, message = "El modelo no puede tener más de 50 caracteres")
    private String model;

    @NotNull(message = "El año es obligatorio")
    @Min(value = 1900, message = "El año debe ser mayor a 1900")
    @Max(value = 2100, message = "El año debe ser menor a 2100")
    private Integer year;

    @Size(max = 30, message = "El color no puede tener más de 30 caracteres")
    private String color;

    @NotBlank(message = "El tipo de transmisión es obligatorio")
    private String transmissionType;

    @NotBlank(message = "El tipo de combustible es obligatorio")
    private String fuelType;

    @NotNull(message = "La capacidad de pasajeros es obligatoria")
    @Min(value = 1, message = "Debe tener al menos 1 pasajero")
    @Max(value = 50, message = "No puede tener más de 50 pasajeros")
    private Integer passengerCapacity;

    @Min(value = 0, message = "El kilometraje no puede ser negativo")
    private Integer mileage;

    // Precios
    @NotNull(message = "El precio normal es obligatorio")
    @DecimalMin(value = "0.01", message = "El precio debe ser mayor a 0")
    private BigDecimal priceNormal;

    @NotNull(message = "El precio VIP es obligatorio")
    @DecimalMin(value = "0.01", message = "El precio debe ser mayor a 0")
    private BigDecimal priceVip;

    @NotNull(message = "El precio +15 días es obligatorio")
    @DecimalMin(value = "0.01", message = "El precio debe ser mayor a 0")
    private BigDecimal priceMoreThan15Days;

    @NotNull(message = "El precio mensual es obligatorio")
    @DecimalMin(value = "0.01", message = "El precio debe ser mayor a 0")
    private BigDecimal priceMonthly;

    private String notes;
}
