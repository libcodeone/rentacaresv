package com.rentacaresv.catalog.domain;

import com.rentacaresv.vehicle.domain.VehicleType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entidad VehicleModel - Catálogo de Modelos de Vehículos
 */
@Entity
@Table(name = "vehicle_model", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"brand_id", "name"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "brand")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class VehicleModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id", nullable = false)
    private VehicleBrand brand;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "vehicle_type", nullable = false)
    private VehicleType vehicleType;

    @Column(name = "year_start")
    private Integer yearStart;

    @Column(name = "year_end")
    private Integer yearEnd;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Métodos de dominio
    public String getFullName() {
        return brand != null ? brand.getName() + " " + name : name;
    }

    public void deactivate() {
        this.active = false;
    }

    public void activate() {
        this.active = true;
    }

    public boolean isValidForYear(Integer year) {
        if (year == null) return true;
        if (yearStart != null && year < yearStart) return false;
        if (yearEnd != null && year > yearEnd) return false;
        return true;
    }
}
