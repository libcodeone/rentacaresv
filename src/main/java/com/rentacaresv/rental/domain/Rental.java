package com.rentacaresv.rental.domain;

import com.rentacaresv.customer.domain.Customer;
import com.rentacaresv.vehicle.domain.Vehicle;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Entidad Rental (Domain Layer)
 * Representa una renta de vehículo en el sistema
 * 
 * Esta es la entidad CORE del negocio.
 * Contiene toda la lógica de negocio relacionada con el proceso de renta.
 */
@Entity
@Table(name = "rental")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Rental {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "contract_number", unique = true, nullable = false, length = 20)
    private String contractNumber;

    // ========================================
    // Relaciones
    // ========================================

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    // ========================================
    // Fechas
    // ========================================

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "actual_delivery_date")
    private LocalDateTime actualDeliveryDate;

    @Column(name = "actual_return_date")
    private LocalDateTime actualReturnDate;

    // ========================================
    // Montos
    // ========================================

    @Column(name = "daily_rate", nullable = false, precision = 10, scale = 2)
    private BigDecimal dailyRate;

    @Column(name = "total_days", nullable = false)
    private Integer totalDays;

    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "amount_paid", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal amountPaid = BigDecimal.ZERO;

    // ========================================
    // Estado y detalles
    // ========================================

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private RentalStatus status = RentalStatus.PENDING;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    // ========================================
    // Información de viaje (opcional)
    // ========================================

    /**
     * Número de vuelo de llegada (para clientes de aeropuerto)
     */
    @Column(name = "flight_number", length = 20)
    private String flightNumber;

    /**
     * Itinerario del cliente (destinos, hoteles, rutas)
     * Usado principalmente para clientes turistas
     */
    @Column(name = "travel_itinerary", columnDefinition = "TEXT")
    private String travelItinerary;

    /**
     * Hotel o lugar de hospedaje principal
     */
    @Column(name = "accommodation", length = 200)
    private String accommodation;

    /**
     * Teléfono de contacto durante el viaje
     */
    @Column(name = "contact_phone", length = 20)
    private String contactPhone;

    // ========================================
    // Auditoría
    // ========================================

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
     * Verifica si es una renta turística (tiene información de viaje)
     */
    public boolean isTouristRental() {
        return travelItinerary != null && !travelItinerary.isBlank();
    }

    /**
     * Actualiza información de viaje
     */
    public void updateTravelInfo(String flightNumber, String itinerary, 
                                  String accommodation, String contactPhone) {
        this.flightNumber = flightNumber;
        this.travelItinerary = itinerary;
        this.accommodation = accommodation;
        this.contactPhone = contactPhone;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Entrega el vehículo al cliente (inicia la renta)
     */
    public void deliverVehicle() {
        if (!status.canBeDelivered()) {
            throw new IllegalStateException(
                "Solo se puede entregar una renta en estado PENDING. Estado actual: " + status
            );
        }
        
        if (!vehicle.isAvailable()) {
            throw new IllegalStateException(
                "El vehículo " + vehicle.getLicensePlate() + " no está disponible"
            );
        }
        
        this.actualDeliveryDate = LocalDateTime.now();
        this.status = RentalStatus.ACTIVE;
        this.updatedAt = LocalDateTime.now();
        
        // Marcar vehículo como rentado (lógica de dominio de Vehicle)
        this.vehicle.markAsRented();
    }

    /**
     * Devuelve el vehículo (finaliza la renta)
     */
    public void returnVehicle() {
        if (!status.canBeReturned()) {
            throw new IllegalStateException(
                "Solo se puede devolver una renta en estado ACTIVE. Estado actual: " + status
            );
        }
        
        this.actualReturnDate = LocalDateTime.now();
        this.status = RentalStatus.COMPLETED;
        this.updatedAt = LocalDateTime.now();
        
        // Marcar vehículo como disponible (lógica de dominio de Vehicle)
        this.vehicle.markAsAvailable();
    }

    /**
     * Cancela la renta (solo si está pendiente)
     */
    public void cancel() {
        if (!status.canBeModified()) {
            throw new IllegalStateException(
                "Solo se puede cancelar una renta en estado PENDING. Estado actual: " + status
            );
        }
        
        this.status = RentalStatus.CANCELLED;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Registra un pago parcial o total
     */
    public void registerPayment(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El monto debe ser mayor a cero");
        }
        
        BigDecimal newTotal = this.amountPaid.add(amount);
        if (newTotal.compareTo(this.totalAmount) > 0) {
            throw new IllegalArgumentException(
                "El pago excede el total de la renta. Total: " + totalAmount + ", Ya pagado: " + amountPaid
            );
        }
        
        this.amountPaid = newTotal;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Calcula el saldo pendiente
     */
    public BigDecimal getBalance() {
        return totalAmount.subtract(amountPaid);
    }

    /**
     * Verifica si la renta está completamente pagada
     */
    public boolean isFullyPaid() {
        return amountPaid.compareTo(totalAmount) >= 0;
    }

    /**
     * Verifica si hay saldo pendiente
     */
    public boolean hasBalance() {
        return getBalance().compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Calcula los días reales de renta (si ya se devolvió)
     */
    public Integer getActualDays() {
        if (actualDeliveryDate == null || actualReturnDate == null) {
            return null;
        }
        return (int) ChronoUnit.DAYS.between(
            actualDeliveryDate.toLocalDate(),
            actualReturnDate.toLocalDate()
        ) + 1; // +1 porque se cuenta el día de entrega
    }

    /**
     * Verifica si la renta se devolvió con retraso
     */
    public boolean isDelayed() {
        if (actualReturnDate == null) {
            return false;
        }
        return actualReturnDate.toLocalDate().isAfter(endDate);
    }

    /**
     * Calcula los días de retraso
     */
    public Integer getDelayDays() {
        if (!isDelayed()) {
            return 0;
        }
        return (int) ChronoUnit.DAYS.between(endDate, actualReturnDate.toLocalDate());
    }

    /**
     * Calcula el monto adicional por retraso
     */
    public BigDecimal calculateDelayPenalty() {
        if (!isDelayed()) {
            return BigDecimal.ZERO;
        }
        // Penalidad: mismo precio diario por cada día de retraso
        return dailyRate.multiply(BigDecimal.valueOf(getDelayDays()));
    }

    /**
     * Verifica si la renta está activa
     */
    public boolean isActive() {
        return status == RentalStatus.ACTIVE;
    }

    /**
     * Verifica si la renta puede ser modificada
     */
    public boolean canBeModified() {
        return status == RentalStatus.PENDING;
    }

    /**
     * Soft delete de la renta
     */
    public void delete() {
        if (status == RentalStatus.ACTIVE) {
            throw new IllegalStateException(
                "No se puede eliminar una renta activa"
            );
        }
        this.deletedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Hook de JPA para actualizar updatedAt
     */
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
