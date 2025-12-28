package com.rentacaresv.rental.infrastructure;

import com.rentacaresv.rental.domain.Rental;
import com.rentacaresv.rental.domain.RentalStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio de Rental (Infrastructure Layer)
 * Maneja la persistencia de rentas en la base de datos
 */
public interface RentalRepository extends JpaRepository<Rental, Long>, JpaSpecificationExecutor<Rental> {

    /**
     * Busca una renta por número de contrato
     */
    Optional<Rental> findByContractNumber(String contractNumber);

    /**
     * Verifica si existe un número de contrato
     */
    boolean existsByContractNumber(String contractNumber);

    /**
     * Encuentra todas las rentas activas (no eliminadas)
     */
    @Query("SELECT r FROM Rental r WHERE r.deletedAt IS NULL ORDER BY r.createdAt DESC")
    List<Rental> findAllActive();

    /**
     * Encuentra rentas por estado
     */
    @Query("SELECT r FROM Rental r WHERE r.status = :status AND r.deletedAt IS NULL ORDER BY r.createdAt DESC")
    List<Rental> findByStatus(@Param("status") RentalStatus status);

    /**
     * Encuentra rentas activas (estado ACTIVE)
     */
    @Query("SELECT r FROM Rental r WHERE r.status = 'ACTIVE' AND r.deletedAt IS NULL")
    List<Rental> findActiveRentals();

    /**
     * Encuentra rentas pendientes (estado PENDING)
     */
    @Query("SELECT r FROM Rental r WHERE r.status = 'PENDING' AND r.deletedAt IS NULL")
    List<Rental> findPendingRentals();

    /**
     * Encuentra rentas por cliente
     */
    @Query("SELECT r FROM Rental r WHERE r.customer.id = :customerId AND r.deletedAt IS NULL ORDER BY r.createdAt DESC")
    List<Rental> findByCustomerId(@Param("customerId") Long customerId);

    /**
     * Encuentra rentas por vehículo
     */
    @Query("SELECT r FROM Rental r WHERE r.vehicle.id = :vehicleId AND r.deletedAt IS NULL ORDER BY r.createdAt DESC")
    List<Rental> findByVehicleId(@Param("vehicleId") Long vehicleId);

    /**
     * Encuentra rentas con saldo pendiente
     */
    @Query("SELECT r FROM Rental r WHERE r.amountPaid < r.totalAmount AND r.deletedAt IS NULL")
    List<Rental> findWithPendingBalance();

    /**
     * Encuentra rentas por rango de fechas
     */
    @Query("SELECT r FROM Rental r WHERE r.startDate BETWEEN :startDate AND :endDate AND r.deletedAt IS NULL")
    List<Rental> findByDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * Verifica si un vehículo tiene rentas activas
     */
    @Query("SELECT COUNT(r) > 0 FROM Rental r WHERE r.vehicle.id = :vehicleId AND r.status = 'ACTIVE'")
    boolean hasActiveRentals(@Param("vehicleId") Long vehicleId);

    /**
     * Verifica si un cliente tiene rentas activas
     */
    @Query("SELECT COUNT(r) > 0 FROM Rental r WHERE r.customer.id = :customerId AND r.status = 'ACTIVE'")
    boolean customerHasActiveRentals(@Param("customerId") Long customerId);

    /**
     * Cuenta rentas por estado
     */
    @Query("SELECT COUNT(r) FROM Rental r WHERE r.status = :status AND r.deletedAt IS NULL")
    long countByStatus(@Param("status") RentalStatus status);

    /**
     * Encuentra rentas que deberían haber sido devueltas (end_date pasado y status ACTIVE)
     */
    @Query("SELECT r FROM Rental r WHERE r.status = 'ACTIVE' AND r.endDate < :currentDate AND r.deletedAt IS NULL")
    List<Rental> findOverdueRentals(@Param("currentDate") LocalDate currentDate);
}
