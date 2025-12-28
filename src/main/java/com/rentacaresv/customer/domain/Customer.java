package com.rentacaresv.customer.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entidad Customer (Domain Layer)
 * Representa un cliente del sistema de rentas
 * 
 * Contiene lógica de negocio relacionada con clientes.
 */
@Entity
@Table(name = "customer")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "full_name", nullable = false, length = 200)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false)
    private DocumentType documentType;

    @Column(name = "document_number", unique = true, nullable = false, length = 50)
    private String documentNumber;

    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    @Builder.Default
    private CustomerCategory category = CustomerCategory.NORMAL;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    // ========================================
    // Auditoría
    // ========================================

    @Column(name = "active")
    @Builder.Default
    private Boolean active = true;

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
     * Verifica si el cliente es VIP
     */
    public boolean isVip() {
        return category == CustomerCategory.VIP;
    }

    /**
     * Promociona al cliente a VIP
     */
    public void promoteToVip() {
        this.category = CustomerCategory.VIP;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Cambia el cliente a categoría normal
     */
    public void demoteToNormal() {
        this.category = CustomerCategory.NORMAL;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Activa el cliente
     */
    public void activate() {
        this.active = true;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Desactiva el cliente
     */
    public void deactivate() {
        if (!canBeDeactivated()) {
            throw new IllegalStateException(
                "No se puede desactivar el cliente porque tiene rentas activas"
            );
        }
        this.active = false;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Verifica si el cliente puede ser desactivado
     * (No debe tener rentas activas - esto se validará con el RentalService)
     */
    private boolean canBeDeactivated() {
        // Por ahora siempre retorna true
        // En el futuro se validará con RentalService
        return true;
    }

    /**
     * Soft delete del cliente
     */
    public void delete() {
        if (!canBeDeleted()) {
            throw new IllegalStateException(
                "No se puede eliminar el cliente porque tiene rentas activas"
            );
        }
        this.deletedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Verifica si el cliente puede ser eliminado
     */
    private boolean canBeDeleted() {
        // Por ahora siempre retorna true
        // En el futuro se validará con RentalService
        return true;
    }

    /**
     * Restaura un cliente eliminado
     */
    public void restore() {
        this.deletedAt = null;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Actualiza la información de contacto
     */
    public void updateContactInfo(String email, String phone, String address) {
        this.email = email;
        this.phone = phone;
        this.address = address;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Obtiene el nombre completo formateado
     */
    public String getDisplayName() {
        return fullName + " (" + documentNumber + ")";
    }

    /**
     * Calcula la edad del cliente
     */
    public Integer getAge() {
        if (birthDate == null) {
            return null;
        }
        return LocalDate.now().getYear() - birthDate.getYear();
    }

    /**
     * Verifica si el cliente está activo y no eliminado
     */
    public boolean isActiveCustomer() {
        return active && deletedAt == null;
    }

    /**
     * Hook de JPA para actualizar updatedAt antes de persistir cambios
     */
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
