package com.rentacaresv.rental.infrastructure;

import com.rentacaresv.rental.domain.Rental;
import com.rentacaresv.rental.domain.RentalStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    // ========================================
    // Métodos con Paginación (para Lazy Loading)
    // ========================================

    /**
     * Encuentra todas las rentas activas con paginación
     */
    @Query("SELECT r FROM Rental r WHERE r.deletedAt IS NULL ORDER BY r.createdAt DESC")
    Page<Rental> findAllActivePaged(Pageable pageable);

    /**
     * Encuentra rentas por estado con paginación
     */
    @Query("SELECT r FROM Rental r WHERE r.status = :status AND r.deletedAt IS NULL ORDER BY r.createdAt DESC")
    Page<Rental> findByStatusPaged(@Param("status") RentalStatus status, Pageable pageable);

    /**
     * Busca rentas por término de búsqueda (contrato, cliente, vehículo) con paginación
     */
    @Query("SELECT r FROM Rental r " +
           "WHERE r.deletedAt IS NULL " +
           "AND (LOWER(r.contractNumber) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR LOWER(r.customer.fullName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR LOWER(r.vehicle.licensePlate) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " +
           "ORDER BY r.createdAt DESC")
    Page<Rental> searchRentals(@Param("searchTerm") String searchTerm, Pageable pageable);

    /**
     * Busca rentas por estado y término de búsqueda con paginación
     */
    @Query("SELECT r FROM Rental r " +
           "WHERE r.status = :status " +
           "AND r.deletedAt IS NULL " +
           "AND (LOWER(r.contractNumber) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR LOWER(r.customer.fullName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR LOWER(r.vehicle.licensePlate) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " +
           "ORDER BY r.createdAt DESC")
    Page<Rental> searchRentalsByStatus(@Param("status") RentalStatus status, 
                                        @Param("searchTerm") String searchTerm, 
                                        Pageable pageable);

    /**
     * Cuenta total de rentas activas (para paginación)
     */
    @Query("SELECT COUNT(r) FROM Rental r WHERE r.deletedAt IS NULL")
    long countAllActive();

    /**
     * Cuenta rentas que terminan en una fecha específica
     */
    @Query("SELECT COUNT(r) FROM Rental r WHERE r.endDate = :date AND r.status IN ('ACTIVE', 'PENDING') AND r.deletedAt IS NULL")
    long countByEndDate(@Param("date") LocalDate date);

    /**
     * Cuenta rentas completadas en un rango de fechas
     */
    @Query("SELECT COUNT(r) FROM Rental r WHERE r.status = 'COMPLETED' AND CAST(r.actualReturnDate AS LocalDate) BETWEEN :startDate AND :endDate AND r.deletedAt IS NULL")
    long countCompletedBetweenDates(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * Verifica si un vehículo tiene rentas activas o pendientes en un rango de fechas
     * Detecta solapamiento de fechas: una nueva renta (newStart, newEnd) solapa con una existente si:
     * - La nueva renta empieza antes de que termine la existente Y
     * - La nueva renta termina después de que empiece la existente
     * 
     * Lógica: newStart < existingEnd AND newEnd > existingStart
     */
    @Query("SELECT COUNT(r) > 0 FROM Rental r " +
           "WHERE r.vehicle.id = :vehicleId " +
           "AND r.status IN ('PENDING', 'ACTIVE') " +
           "AND r.deletedAt IS NULL " +
           "AND r.startDate < :endDate " +
           "AND r.endDate > :startDate")
    boolean hasConflictingRentals(
        @Param("vehicleId") Long vehicleId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    /**
     * Verifica si un vehículo tiene rentas activas o pendientes en un rango de fechas,
     * excluyendo una renta específica (útil para ediciones)
     */
    @Query("SELECT COUNT(r) > 0 FROM Rental r " +
           "WHERE r.vehicle.id = :vehicleId " +
           "AND r.id != :excludeRentalId " +
           "AND r.status IN ('PENDING', 'ACTIVE') " +
           "AND r.deletedAt IS NULL " +
           "AND r.startDate < :endDate " +
           "AND r.endDate > :startDate")
    boolean hasConflictingRentalsExcluding(
        @Param("vehicleId") Long vehicleId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("excludeRentalId") Long excludeRentalId
    );

    /**
     * Obtiene todas las rentas activas o pendientes de un vehículo
     * (para mostrar fechas ocupadas en el calendario)
     */
    @Query("SELECT r FROM Rental r " +
           "WHERE r.vehicle.id = :vehicleId " +
           "AND r.status IN ('PENDING', 'ACTIVE') " +
           "AND r.deletedAt IS NULL " +
           "ORDER BY r.startDate ASC")
    List<Rental> findActiveRentalsByVehicleId(@Param("vehicleId") Long vehicleId);
}
