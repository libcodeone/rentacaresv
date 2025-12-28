package com.rentacaresv.customer.infrastructure;

import com.rentacaresv.customer.domain.Customer;
import com.rentacaresv.customer.domain.CustomerCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio de Customer (Infrastructure Layer)
 * Maneja la persistencia de clientes en la base de datos
 */
public interface CustomerRepository extends JpaRepository<Customer, Long>, JpaSpecificationExecutor<Customer> {

    /**
     * Busca un cliente por número de documento
     */
    Optional<Customer> findByDocumentNumber(String documentNumber);

    /**
     * Verifica si existe un número de documento
     */
    boolean existsByDocumentNumber(String documentNumber);

    /**
     * Busca un cliente por email
     */
    Optional<Customer> findByEmail(String email);

    /**
     * Encuentra todos los clientes activos (no eliminados)
     */
    @Query("SELECT c FROM Customer c WHERE c.deletedAt IS NULL")
    List<Customer> findAllActive();

    /**
     * Encuentra clientes por categoría
     */
    @Query("SELECT c FROM Customer c WHERE c.category = :category AND c.deletedAt IS NULL")
    List<Customer> findByCategory(@Param("category") CustomerCategory category);

    /**
     * Encuentra clientes VIP activos
     */
    @Query("SELECT c FROM Customer c WHERE c.category = 'VIP' AND c.active = true AND c.deletedAt IS NULL")
    List<Customer> findActiveVipCustomers();

    /**
     * Busca clientes por nombre (búsqueda parcial)
     */
    @Query("SELECT c FROM Customer c WHERE LOWER(c.fullName) LIKE LOWER(CONCAT('%', :name, '%')) AND c.deletedAt IS NULL")
    List<Customer> searchByName(@Param("name") String name);

    /**
     * Busca clientes por teléfono
     */
    @Query("SELECT c FROM Customer c WHERE c.phone LIKE CONCAT('%', :phone, '%') AND c.deletedAt IS NULL")
    List<Customer> searchByPhone(@Param("phone") String phone);

    /**
     * Cuenta clientes por categoría
     */
    @Query("SELECT COUNT(c) FROM Customer c WHERE c.category = :category AND c.deletedAt IS NULL")
    long countByCategory(@Param("category") CustomerCategory category);

    /**
     * Cuenta clientes VIP
     */
    @Query("SELECT COUNT(c) FROM Customer c WHERE c.category = 'VIP' AND c.deletedAt IS NULL")
    long countVipCustomers();

    /**
     * Encuentra clientes activos y no eliminados
     */
    @Query("SELECT c FROM Customer c WHERE c.active = true AND c.deletedAt IS NULL")
    List<Customer> findActiveCustomers();
}
