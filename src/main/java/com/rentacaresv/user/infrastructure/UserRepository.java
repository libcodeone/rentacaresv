package com.rentacaresv.user.infrastructure;

import com.rentacaresv.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio de User (Infrastructure Layer)
 * Maneja la persistencia de usuarios en la base de datos
 */
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    /**
     * Busca un usuario por su username
     */
    User findByUsername(String username);

    /**
     * Busca un usuario por email
     */
    Optional<User> findByEmail(String email);

    /**
     * Encuentra todos los usuarios activos
     */
    @Query("SELECT u FROM User u WHERE u.active = true")
    List<User> findAllActive();

    /**
     * Verifica si existe un username
     */
    boolean existsByUsername(String username);

    /**
     * Verifica si existe un email
     */
    boolean existsByEmail(String email);

    /**
     * Busca usuarios por nombre (búsqueda parcial)
     */
    @Query("SELECT u FROM User u WHERE LOWER(u.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<User> searchByName(@Param("name") String name);
}
