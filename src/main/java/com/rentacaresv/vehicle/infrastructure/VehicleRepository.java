package com.rentacaresv.vehicle.infrastructure;

import com.rentacaresv.vehicle.domain.Vehicle;
import com.rentacaresv.vehicle.domain.VehicleStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio de Vehicle (Infrastructure Layer)
 * Maneja la persistencia de vehículos en la base de datos
 */
public interface VehicleRepository extends JpaRepository<Vehicle, Long>, JpaSpecificationExecutor<Vehicle> {

    /**
     * Busca un vehículo por placa
     */
    Optional<Vehicle> findByLicensePlate(String licensePlate);

    /**
     * Verifica si existe una placa
     */
    boolean existsByLicensePlate(String licensePlate);

    /**
     * Encuentra todos los vehículos activos (no eliminados)
     */
    @Query("SELECT v FROM Vehicle v WHERE v.deletedAt IS NULL")
    List<Vehicle> findAllActive();

    /**
     * Encuentra vehículos por estado
     */
    @Query("SELECT v FROM Vehicle v WHERE v.status = :status AND v.deletedAt IS NULL")
    List<Vehicle> findByStatus(@Param("status") VehicleStatus status);

    /**
     * Encuentra vehículos disponibles para rentar
     */
    @Query("SELECT v FROM Vehicle v WHERE v.status = 'AVAILABLE' AND v.deletedAt IS NULL")
    List<Vehicle> findAvailableVehicles();

    /**
     * Busca vehículos por marca
     */
    @Query("SELECT v FROM Vehicle v WHERE LOWER(v.brand) LIKE LOWER(CONCAT('%', :brand, '%')) AND v.deletedAt IS NULL")
    List<Vehicle> searchByBrand(@Param("brand") String brand);

    /**
     * Busca vehículos por marca y modelo
     */
    @Query("SELECT v FROM Vehicle v WHERE LOWER(v.brand) = LOWER(:brand) AND LOWER(v.model) = LOWER(:model) AND v.deletedAt IS NULL")
    List<Vehicle> findByBrandAndModel(@Param("brand") String brand, @Param("model") String model);

    /**
     * Busca vehículos por año
     */
    @Query("SELECT v FROM Vehicle v WHERE v.year = :year AND v.deletedAt IS NULL")
    List<Vehicle> findByYear(@Param("year") Integer year);

    /**
     * Cuenta vehículos por estado
     */
    @Query("SELECT COUNT(v) FROM Vehicle v WHERE v.status = :status AND v.deletedAt IS NULL")
    long countByStatus(@Param("status") VehicleStatus status);

    /**
     * Cuenta vehículos disponibles
     */
    @Query("SELECT COUNT(v) FROM Vehicle v WHERE v.status = 'AVAILABLE' AND v.deletedAt IS NULL")
    long countAvailableVehicles();
}
