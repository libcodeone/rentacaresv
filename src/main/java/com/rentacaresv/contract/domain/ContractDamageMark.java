package com.rentacaresv.contract.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Entidad ContractDamageMark (Domain Layer)
 * Representa una marca de daño en el diagrama del vehículo.
 * 
 * Cada marca tiene coordenadas (x, y) relativas al diagrama SVG,
 * un tipo de daño y una descripción.
 */
@Entity
@Table(name = "contract_damage_mark")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "contract")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ContractDamageMark {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    /**
     * Contrato al que pertenece esta marca
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id", nullable = false)
    private Contract contract;

    /**
     * Coordenada X en el diagrama (porcentaje 0-100)
     */
    @Column(name = "position_x", nullable = false, precision = 5, scale = 2)
    private BigDecimal positionX;

    /**
     * Coordenada Y en el diagrama (porcentaje 0-100)
     */
    @Column(name = "position_y", nullable = false, precision = 5, scale = 2)
    private BigDecimal positionY;

    /**
     * Tipo de daño
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "damage_type", nullable = false)
    private DamageType damageType;

    /**
     * Descripción del daño
     */
    @Column(name = "description", length = 255)
    private String description;

    /**
     * Indica si es un daño preexistente (salida) o nuevo (entrada)
     */
    @Column(name = "is_pre_existing", nullable = false)
    @Builder.Default
    private Boolean isPreExisting = true;

    /**
     * Severidad del daño (1-5)
     */
    @Column(name = "severity")
    @Builder.Default
    private Integer severity = 1;

    // ========================================
    // Métodos de Negocio
    // ========================================

    /**
     * Verifica si es un daño nuevo (encontrado en la devolución)
     */
    public boolean isNewDamage() {
        return !isPreExisting;
    }

    /**
     * Marca como daño preexistente
     */
    public void markAsPreExisting() {
        this.isPreExisting = true;
    }

    /**
     * Marca como daño nuevo
     */
    public void markAsNew() {
        this.isPreExisting = false;
    }
}
