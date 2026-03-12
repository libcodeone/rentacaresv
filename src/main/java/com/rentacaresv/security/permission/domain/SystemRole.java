package com.rentacaresv.security.permission.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Entidad de Rol del Sistema (Domain Layer)
 * Roles dinámicos creados por el administrador con permisos asignables.
 */
@Entity
@Table(name = "system_role")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class SystemRole {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "name", unique = true, nullable = false, length = 50)
    private String name;

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "color", length = 7)
    @Builder.Default
    private String color = "#3B82F6"; // Azul por defecto

    @Column(name = "is_system_role")
    @Builder.Default
    private Boolean isSystemRole = false; // true para ADMIN, OPERATOR que no se pueden eliminar

    @Column(name = "active")
    @Builder.Default
    private Boolean active = true;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "role_permissions",
        joinColumns = @JoinColumn(name = "role_id")
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "permission")
    @Builder.Default
    private Set<Permission> permissions = new HashSet<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ========================================
    // Métodos de Negocio
    // ========================================

    /**
     * Verifica si el rol tiene un permiso específico
     */
    public boolean hasPermission(Permission permission) {
        return permissions != null && permissions.contains(permission);
    }

    /**
     * Agrega un permiso al rol
     */
    public void addPermission(Permission permission) {
        if (this.permissions == null) {
            this.permissions = new HashSet<>();
        }
        this.permissions.add(permission);
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Remueve un permiso del rol
     */
    public void removePermission(Permission permission) {
        if (this.permissions != null) {
            this.permissions.remove(permission);
            this.updatedAt = LocalDateTime.now();
        }
    }

    /**
     * Establece todos los permisos del rol
     */
    public void setAllPermissions(Set<Permission> newPermissions) {
        this.permissions = new HashSet<>(newPermissions);
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Limpia todos los permisos del rol
     */
    public void clearPermissions() {
        if (this.permissions != null) {
            this.permissions.clear();
            this.updatedAt = LocalDateTime.now();
        }
    }

    /**
     * Verifica si el rol puede ser eliminado
     */
    public boolean canBeDeleted() {
        return !Boolean.TRUE.equals(isSystemRole);
    }

    /**
     * Verifica si el rol puede ser modificado
     */
    public boolean canBeModified() {
        return true; // Todos los roles pueden ser modificados, pero los de sistema no pueden ser eliminados
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
