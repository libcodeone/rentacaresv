package com.rentacaresv.contract.domain;

import jakarta.persistence.*;
import lombok.*;

/**
 * Entidad ContractAccessory (Domain Layer)
 * Representa un accesorio verificado en el contrato.
 * 
 * Cada accesorio tiene un estado (presente/ausente) y observaciones opcionales.
 */
@Entity
@Table(name = "contract_accessory")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "contract")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ContractAccessory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    /**
     * Contrato al que pertenece este accesorio
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id", nullable = false)
    private Contract contract;

    /**
     * ID del accesorio del catálogo (si aplica)
     */
    @Column(name = "accessory_catalog_id")
    private Long accessoryCatalogId;

    /**
     * Nombre del accesorio
     * Se guarda aquí para mantener historial aunque el catálogo cambie
     */
    @Column(name = "accessory_name", nullable = false, length = 100)
    private String accessoryName;

    /**
     * Indica si el accesorio está presente (true = SÍ, false = NO)
     */
    @Column(name = "is_present", nullable = false)
    @Builder.Default
    private Boolean isPresent = true;

    /**
     * Observaciones sobre el estado del accesorio
     */
    @Column(name = "observations", length = 255)
    private String observations;

    /**
     * Orden de visualización
     */
    @Column(name = "display_order")
    @Builder.Default
    private Integer displayOrder = 0;

    // ========================================
    // Métodos de Negocio
    // ========================================

    /**
     * Verifica si es un accesorio personalizado (no del catálogo)
     */
    public boolean isCustomAccessory() {
        return accessoryCatalogId == null;
    }

    /**
     * Marca el accesorio como presente
     */
    public void markAsPresent() {
        this.isPresent = true;
    }

    /**
     * Marca el accesorio como ausente
     */
    public void markAsAbsent() {
        this.isPresent = false;
    }

    /**
     * Marca el accesorio como ausente con observación
     */
    public void markAsAbsent(String observation) {
        this.isPresent = false;
        this.observations = observation;
    }
}
