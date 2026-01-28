package com.rentacaresv.contract.domain;

import com.rentacaresv.vehicle.domain.VehicleType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entidad VehicleDiagram (Domain Layer)
 * Catálogo de diagramas/imágenes para cada tipo de vehículo.
 * 
 * Cada tipo de vehículo tiene un diagrama diferente donde
 * se pueden marcar los daños existentes.
 */
@Entity
@Table(name = "vehicle_diagram")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class VehicleDiagram {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    /**
     * Tipo de vehículo al que aplica este diagrama
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "vehicle_type", nullable = false, unique = true)
    private VehicleType vehicleType;

    /**
     * Nombre descriptivo del diagrama
     */
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /**
     * Contenido SVG del diagrama (legacy - para compatibilidad)
     */
    @Column(name = "svg_content", columnDefinition = "TEXT")
    private String svgContent;

    /**
     * URL de la imagen del diagrama en Digital Ocean Spaces
     * Puede ser PNG, JPG o SVG
     */
    @Column(name = "image_url", length = 500)
    private String imageUrl;

    /**
     * URL del archivo SVG en Digital Ocean Spaces (legacy)
     */
    @Column(name = "svg_url", length = 500)
    private String svgUrl;

    /**
     * Ancho del diagrama en píxeles
     */
    @Column(name = "width", nullable = false)
    @Builder.Default
    private Integer width = 800;

    /**
     * Alto del diagrama en píxeles
     */
    @Column(name = "height", nullable = false)
    @Builder.Default
    private Integer height = 400;

    /**
     * Indica si está activo
     */
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

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
     * Obtiene la URL de la imagen del diagrama
     * Prioriza imageUrl, luego svgUrl
     */
    public String getDiagramUrl() {
        if (imageUrl != null && !imageUrl.isEmpty()) {
            return imageUrl;
        }
        return svgUrl;
    }

    /**
     * Verifica si tiene contenido de diagrama (imagen o SVG)
     */
    public boolean hasDiagramContent() {
        return (imageUrl != null && !imageUrl.isEmpty()) ||
               (svgUrl != null && !svgUrl.isEmpty()) ||
               (svgContent != null && !svgContent.isEmpty());
    }

    /**
     * Actualiza la URL de la imagen
     */
    public void updateImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Actualiza el contenido SVG (legacy)
     */
    public void updateSvgContent(String svgContent) {
        this.svgContent = svgContent;
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
