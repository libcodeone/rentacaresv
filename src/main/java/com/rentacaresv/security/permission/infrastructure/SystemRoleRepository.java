package com.rentacaresv.security.permission.infrastructure;

import com.rentacaresv.security.permission.domain.SystemRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio de SystemRole (Infrastructure Layer)
 */
public interface SystemRoleRepository extends JpaRepository<SystemRole, Long> {

    /**
     * Busca un rol por nombre
     */
    Optional<SystemRole> findByName(String name);

    /**
     * Busca un rol por nombre de display
     */
    Optional<SystemRole> findByDisplayName(String displayName);

    /**
     * Encuentra todos los roles activos
     */
    @Query("SELECT r FROM SystemRole r WHERE r.active = true ORDER BY r.displayName")
    List<SystemRole> findAllActive();

    /**
     * Encuentra todos los roles ordenados por nombre
     */
    @Query("SELECT r FROM SystemRole r ORDER BY r.isSystemRole DESC, r.displayName")
    List<SystemRole> findAllOrdered();

    /**
     * Verifica si existe un rol con ese nombre
     */
    boolean existsByName(String name);

    /**
     * Verifica si existe un rol con ese display name
     */
    boolean existsByDisplayName(String displayName);

    /**
     * Busca roles por categoría (búsqueda parcial en nombre o descripción)
     */
    @Query("SELECT r FROM SystemRole r WHERE r.active = true AND " +
           "(LOWER(r.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(r.displayName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(r.description) LIKE LOWER(CONCAT('%', :search, '%')))")
    List<SystemRole> searchRoles(@Param("search") String search);

    /**
     * Encuentra roles del sistema (no eliminables)
     */
    @Query("SELECT r FROM SystemRole r WHERE r.isSystemRole = true")
    List<SystemRole> findSystemRoles();
}
