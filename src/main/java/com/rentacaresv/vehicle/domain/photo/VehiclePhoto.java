package com.rentacaresv.vehicle.domain.photo;

import com.rentacaresv.vehicle.domain.Vehicle;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entidad VehiclePhoto
 * Almacena las URLs de las fotos de los vehículos
 */
@Entity
@Table(name = "vehicle_photo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"vehicle"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class VehiclePhoto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    /**
     * URL de la foto en Digital Ocean Spaces
     */
    @Column(name = "photo_url", nullable = false, length = 500)
    private String photoUrl;

    /**
     * Tipo de foto (Exterior/Interior)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "photo_type", nullable = false)
    private PhotoType photoType;

    /**
     * Indica si es la foto principal del vehículo
     */
    @Column(name = "is_primary", nullable = false)
    @Builder.Default
    private Boolean isPrimary = false;

    /**
     * Orden de visualización
     */
    @Column(name = "display_order")
    private Integer displayOrder;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // ========================================
    // Métodos de Negocio
    // ========================================

    /**
     * Marca esta foto como principal
     */
    public void markAsPrimary() {
        this.isPrimary = true;
    }

    /**
     * Desmarca como principal
     */
    public void unmarkAsPrimary() {
        this.isPrimary = false;
    }

    /**
     * Soft delete
     */
    public void delete() {
        this.deletedAt = LocalDateTime.now();
    }
}
