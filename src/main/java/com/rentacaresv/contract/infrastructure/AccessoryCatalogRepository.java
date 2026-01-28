package com.rentacaresv.contract.infrastructure;

import com.rentacaresv.contract.domain.AccessoryCatalog;
import com.rentacaresv.contract.domain.AccessoryCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/**
 * Repositorio de AccessoryCatalog
 */
public interface AccessoryCatalogRepository extends JpaRepository<AccessoryCatalog, Long> {

    /**
     * Busca todos los accesorios activos ordenados por categoría y orden
     */
    @Query("SELECT a FROM AccessoryCatalog a WHERE a.isActive = true ORDER BY a.category, a.displayOrder, a.name")
    List<AccessoryCatalog> findAllActiveOrdered();

    /**
     * Busca accesorios por categoría
     */
    List<AccessoryCatalog> findByCategoryAndIsActiveTrueOrderByDisplayOrderAscNameAsc(AccessoryCategory category);

    /**
     * Busca accesorios obligatorios
     */
    List<AccessoryCatalog> findByIsMandatoryTrueAndIsActiveTrueOrderByDisplayOrderAsc();

    /**
     * Busca por nombre (para evitar duplicados)
     */
    boolean existsByNameIgnoreCase(String name);

    /**
     * Cuenta accesorios activos
     */
    long countByIsActiveTrue();
}
