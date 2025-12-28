package com.rentacaresv.payment.domain;

import com.rentacaresv.rental.domain.Rental;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entidad Payment (Domain Layer)
 * Representa un pago realizado para una renta
 * 
 * Un Rental puede tener múltiples Payments (pagos parciales)
 */
@Entity
@Table(name = "payment")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"rental"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "payment_number", unique = true, nullable = false, length = 30)
    private String paymentNumber;

    // ========================================
    // Relación con Rental
    // ========================================

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rental_id", nullable = false)
    private Rental rental;

    // ========================================
    // Información del pago
    // ========================================

    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.COMPLETED;

    @Column(name = "payment_date", nullable = false)
    @Builder.Default
    private LocalDateTime paymentDate = LocalDateTime.now();

    // ========================================
    // Datos adicionales según método de pago
    // ========================================

    @Column(name = "reference_number", length = 100)
    private String referenceNumber;  // Número de autorización, número de cheque, etc.

    @Column(name = "card_last_digits", length = 4)
    private String cardLastDigits;  // Últimos 4 dígitos de tarjeta

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    // ========================================
    // Auditoría
    // ========================================

    @Column(name = "created_by", length = 50)
    private String createdBy;  // Usuario que registró el pago

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
     * Verifica si el pago fue exitoso
     */
    public boolean isSuccessful() {
        return status == PaymentStatus.COMPLETED;
    }

    /**
     * Verifica si el pago puede ser reembolsado
     */
    public boolean canBeRefunded() {
        return status == PaymentStatus.COMPLETED && deletedAt == null;
    }

    /**
     * Marca el pago como reembolsado
     */
    public void refund() {
        if (!canBeRefunded()) {
            throw new IllegalStateException(
                "Solo se pueden reembolsar pagos completados. Estado actual: " + status
            );
        }
        this.status = PaymentStatus.REFUNDED;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Marca el pago como rechazado
     */
    public void reject(String reason) {
        if (status == PaymentStatus.COMPLETED) {
            throw new IllegalStateException("No se puede rechazar un pago completado");
        }
        this.status = PaymentStatus.REJECTED;
        this.notes = (notes != null ? notes + "\n" : "") + "RECHAZADO: " + reason;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Confirma un pago pendiente
     */
    public void confirm() {
        if (status != PaymentStatus.PENDING) {
            throw new IllegalStateException(
                "Solo se pueden confirmar pagos pendientes. Estado actual: " + status
            );
        }
        this.status = PaymentStatus.COMPLETED;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Valida el monto del pago
     */
    public void validateAmount() {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El monto debe ser mayor a cero");
        }
    }

    /**
     * Obtiene una descripción del pago
     */
    public String getDescription() {
        return String.format("%s - %s - $%.2f", 
            paymentNumber, 
            paymentMethod.getLabel(), 
            amount
        );
    }

    /**
     * Soft delete del pago
     */
    public void delete() {
        if (status == PaymentStatus.COMPLETED) {
            throw new IllegalStateException(
                "No se puede eliminar un pago completado. Use refund() en su lugar."
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
