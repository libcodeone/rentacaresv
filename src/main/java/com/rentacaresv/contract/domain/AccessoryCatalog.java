package com.rentacaresv.contract.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entidad AccessoryCatalog (Domain Layer)
 * Catálogo de accesorios que se verifican en los contratos.
 * 
 * Este catálogo es configurable y permite agregar/quitar accesorios
 * que se mostrarán en el checklist del contrato.
 */
@Entity
@Table(name = "accessory_catalog")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class AccessoryCatalog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    /**
     * Nombre del accesorio
     */
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /**
     * Descripción opcional
     */
    @Column(name = "description", length = 255)
    private String description;

    /**
     * Categoría del accesorio (para agrupar en el checklist)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    @Builder.Default
    private AccessoryCategory category = AccessoryCategory.EXTERIOR;

    /**
     * Indica si está activo (se muestra en contratos nuevos)
     */
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    /**
     * Orden de visualización dentro de su categoría
     */
    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private Integer displayOrder = 0;

    /**
     * Indica si es un accesorio obligatorio (siempre debe verificarse)
     */
    @Column(name = "is_mandatory", nullable = false)
    @Builder.Default
    private Boolean isMandatory = false;

    // ========================================
    // Auditoría
    // ========================================

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ========================================
    // Métodos de Negocio
    // ========================================

    /**
     * Activa el accesorio
     */
    public void activate() {
        this.isActive = true;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Desactiva el accesorio
     */
    public void deactivate() {
        this.isActive = false;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Hook de JPA
     */
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
