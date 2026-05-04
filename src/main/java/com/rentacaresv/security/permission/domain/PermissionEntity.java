package com.rentacaresv.security.permission.domain;

import jakarta.persistence.*;
import lombok.*;

/**
 * Entidad JPA que representa un permiso del sistema.
 * Cada fila de la tabla permission es un permiso individual.
 */
@Entity
@Table(name = "permission")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class PermissionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @EqualsAndHashCode.Include
    @Column(name = "name", unique = true, nullable = false, length = 100)
    private String name;

    @Column(name = "display_name", nullable = false, length = 200)
    private String displayName;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "category", nullable = false, length = 100)
    private String category;

    /**
     * Convierte a Permission enum para uso en lógica de negocio.
     */
    public Permission toEnum() {
        return Permission.valueOf(name);
    }

    /**
     * Crea una PermissionEntity a partir del enum Permission.
     */
    public static PermissionEntity from(Permission permission) {
        return PermissionEntity.builder()
                .name(permission.name())
                .displayName(permission.getDisplayName())
                .description(permission.getDescription())
                .category(permission.getCategory())
                .build();
    }
}
