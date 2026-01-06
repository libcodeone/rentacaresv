package com.rentacaresv.catalog.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidad VehicleBrand - Catálogo de Marcas de Vehículos
 */
@Entity
@Table(name = "vehicle_brand")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "models")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class VehicleBrand {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "name", nullable = false, unique = true, length = 50)
    private String name;

    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "brand", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<VehicleModel> models = new ArrayList<>();

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Métodos de dominio
    public void addModel(VehicleModel model) {
        models.add(model);
        model.setBrand(this);
    }

    public void removeModel(VehicleModel model) {
        models.remove(model);
        model.setBrand(null);
    }

    public void deactivate() {
        this.active = false;
    }

    public void activate() {
        this.active = true;
    }
}
