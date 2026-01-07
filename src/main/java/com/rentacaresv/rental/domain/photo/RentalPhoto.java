package com.rentacaresv.rental.domain.photo;

import com.rentacaresv.rental.domain.Rental;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entidad RentalPhoto
 * Almacena las fotos de entrega y devolución de vehículos
 */
@Entity
@Table(name = "rental_photo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"rental"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class RentalPhoto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rental_id", nullable = false)
    private Rental rental;

    /**
     * URL de la foto en Digital Ocean Spaces
     */
    @Column(name = "photo_url", nullable = false, length = 500)
    private String photoUrl;

    /**
     * Tipo de foto (Entrega/Devolución, Exterior/Interior/Accesorios)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "photo_type", nullable = false)
    private RentalPhotoType photoType;

    /**
     * Descripción o nota de la foto
     */
    @Column(name = "description", length = 500)
    private String description;

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
     * Verifica si es foto de entrega
     */
    public boolean isDeliveryPhoto() {
        return photoType.isDelivery();
    }

    /**
     * Verifica si es foto de devolución
     */
    public boolean isReturnPhoto() {
        return photoType.isReturn();
    }

    /**
     * Soft delete
     */
    public void delete() {
        this.deletedAt = LocalDateTime.now();
    }
}
