package com.rentacaresv.vehicle.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entidad Vehicle (Domain Layer)
 * Representa un vehículo en el sistema de rentas
 * 
 * Contiene lógica de negocio relacionada con vehículos.
 */
@Entity
@Table(name = "vehicle")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Vehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "license_plate", unique = true, nullable = false, length = 20)
    private String licensePlate;

    @Column(name = "brand", nullable = false, length = 50)
    private String brand;

    @Column(name = "model", nullable = false, length = 50)
    private String model;

    @Column(name = "year", nullable = false)
    private Integer year;

    @Column(name = "color", length = 30)
    private String color;

    @Enumerated(EnumType.STRING)
    @Column(name = "transmission_type", nullable = false)
    private TransmissionType transmissionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "fuel_type", nullable = false)
    private FuelType fuelType;

    @Enumerated(EnumType.STRING)
    @Column(name = "vehicle_type")
    private VehicleType vehicleType;

    @Column(name = "passenger_capacity", nullable = false)
    private Integer passengerCapacity;

    @Column(name = "mileage")
    private Integer mileage;

    // ========================================
    // Precios según categoría de cliente
    // ========================================

    @Column(name = "price_normal", nullable = false, precision = 10, scale = 2)
    private BigDecimal priceNormal;

    @Column(name = "price_vip", nullable = false, precision = 10, scale = 2)
    private BigDecimal priceVip;

    @Column(name = "price_more_than_15_days", nullable = false, precision = 10, scale = 2)
    private BigDecimal priceMoreThan15Days;

    @Column(name = "price_monthly", nullable = false, precision = 10, scale = 2)
    private BigDecimal priceMonthly;

    // ========================================
    // Estado y auditoría
    // ========================================

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private VehicleStatus status = VehicleStatus.AVAILABLE;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // ========================================
    // Métodos de Negocio (Domain Logic)
    // ========================================

    /**
     * Verifica si el vehículo está disponible para rentar
     */
    public boolean isAvailable() {
        return status == VehicleStatus.AVAILABLE && deletedAt == null;
    }

    /**
     * Marca el vehículo como rentado
     */
    public void markAsRented() {
        if (!isAvailable()) {
            throw new IllegalStateException(
                "El vehículo " + licensePlate + " no está disponible para rentar"
            );
        }
        this.status = VehicleStatus.RENTED;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Marca el vehículo como disponible
     */
    public void markAsAvailable() {
        if (this.status == VehicleStatus.OUT_OF_SERVICE) {
            throw new IllegalStateException(
                "El vehículo " + licensePlate + " está fuera de servicio"
            );
        }
        this.status = VehicleStatus.AVAILABLE;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Envía el vehículo a mantenimiento
     */
    public void sendToMaintenance() {
        if (this.status == VehicleStatus.RENTED) {
            throw new IllegalStateException(
                "No se puede enviar a mantenimiento el vehículo " + licensePlate + " porque está rentado"
            );
        }
        this.status = VehicleStatus.MAINTENANCE;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Marca el vehículo como fuera de servicio
     */
    public void markAsOutOfService() {
        if (this.status == VehicleStatus.RENTED) {
            throw new IllegalStateException(
                "No se puede sacar de servicio el vehículo " + licensePlate + " porque está rentado"
            );
        }
        this.status = VehicleStatus.OUT_OF_SERVICE;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Actualiza el kilometraje del vehículo
     */
    public void updateMileage(Integer newMileage) {
        if (newMileage < this.mileage) {
            throw new IllegalArgumentException(
                "El nuevo kilometraje no puede ser menor al actual"
            );
        }
        this.mileage = newMileage;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Soft delete del vehículo
     */
    public void delete() {
        if (this.status == VehicleStatus.RENTED) {
            throw new IllegalStateException(
                "No se puede eliminar el vehículo " + licensePlate + " porque está rentado"
            );
        }
        this.deletedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Restaura un vehículo eliminado
     */
    public void restore() {
        this.deletedAt = null;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Obtiene el precio según la categoría
     */
    public BigDecimal getPriceForCategory(String category) {
        return switch (category.toUpperCase()) {
            case "VIP" -> priceVip;
            case "MORE_THAN_15_DAYS" -> priceMoreThan15Days;
            case "MONTHLY" -> priceMonthly;
            default -> priceNormal;
        };
    }

    /**
     * Obtiene una descripción completa del vehículo
     */
    public String getFullDescription() {
        return String.format("%s %s %d - %s", brand, model, year, licensePlate);
    }

    /**
     * Hook de JPA para actualizar updatedAt antes de persistir cambios
     */
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
