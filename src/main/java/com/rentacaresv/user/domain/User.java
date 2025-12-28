package com.rentacaresv.user.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.rentacaresv.security.Role;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * Entidad de Usuario del Sistema (Domain Layer)
 * Representa los usuarios que pueden acceder al sistema RentaCar ESV
 * 
 * Esta entidad contiene lógica de negocio relacionada con usuarios.
 */
@Entity
@Table(name = "user")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"hashedPassword"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "username", unique = true, nullable = false, length = 50)
    private String username;

    @Column(name = "full_name", nullable = false, length = 200)
    private String name;

    @JsonIgnore
    @Column(name = "hashed_password", nullable = false)
    private String hashedPassword;

    @Column(name = "email", length = 100)
    private String email;

    @Enumerated(EnumType.STRING)
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "user_roles",
        joinColumns = @JoinColumn(name = "user_id")
    )
    @Column(name = "role")
    @Builder.Default
    private Set<Role> roles = Set.of(Role.OPERATOR);

    @Column(name = "active")
    @Builder.Default
    private Boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    // ========================================
    // Métodos de Negocio (Domain Logic)
    // ========================================

    /**
     * Actualiza la fecha de último login
     */
    public void updateLastLogin() {
        this.lastLogin = LocalDateTime.now();
    }

    /**
     * Verifica si el usuario tiene un rol específico
     */
    public boolean hasRole(Role role) {
        return roles != null && roles.contains(role);
    }

    /**
     * Verifica si el usuario es administrador
     */
    public boolean isAdmin() {
        return hasRole(Role.ADMIN);
    }

    /**
     * Activa el usuario
     */
    public void activate() {
        this.active = true;
    }

    /**
     * Desactiva el usuario
     */
    public void deactivate() {
        this.active = false;
    }

    /**
     * Agrega un rol al usuario
     */
    public void addRole(Role role) {
        this.roles.add(role);
    }

    /**
     * Remueve un rol del usuario
     */
    public void removeRole(Role role) {
        this.roles.remove(role);
    }
}
