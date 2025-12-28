package com.rentacaresv.payment.infrastructure;

import com.rentacaresv.payment.domain.Payment;
import com.rentacaresv.payment.domain.PaymentMethod;
import com.rentacaresv.payment.domain.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio de Payment (Infrastructure Layer)
 * Maneja la persistencia de pagos en la base de datos
 */
public interface PaymentRepository extends JpaRepository<Payment, Long>, JpaSpecificationExecutor<Payment> {

    /**
     * Busca un pago por número de pago
     */
    Optional<Payment> findByPaymentNumber(String paymentNumber);

    /**
     * Verifica si existe un número de pago
     */
    boolean existsByPaymentNumber(String paymentNumber);

    /**
     * Encuentra todos los pagos activos (no eliminados)
     */
    @Query("SELECT p FROM Payment p WHERE p.deletedAt IS NULL ORDER BY p.paymentDate DESC")
    List<Payment> findAllActive();

    /**
     * Encuentra pagos por renta
     */
    @Query("SELECT p FROM Payment p WHERE p.rental.id = :rentalId AND p.deletedAt IS NULL ORDER BY p.paymentDate DESC")
    List<Payment> findByRentalId(@Param("rentalId") Long rentalId);

    /**
     * Encuentra pagos por estado
     */
    @Query("SELECT p FROM Payment p WHERE p.status = :status AND p.deletedAt IS NULL ORDER BY p.paymentDate DESC")
    List<Payment> findByStatus(@Param("status") PaymentStatus status);

    /**
     * Encuentra pagos por método de pago
     */
    @Query("SELECT p FROM Payment p WHERE p.paymentMethod = :method AND p.deletedAt IS NULL ORDER BY p.paymentDate DESC")
    List<Payment> findByPaymentMethod(@Param("method") PaymentMethod method);

    /**
     * Encuentra pagos en un rango de fechas
     */
    @Query("SELECT p FROM Payment p WHERE p.paymentDate BETWEEN :startDate AND :endDate AND p.deletedAt IS NULL ORDER BY p.paymentDate DESC")
    List<Payment> findByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    /**
     * Calcula el total de pagos completados para una renta
     */
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.rental.id = :rentalId AND p.status = 'COMPLETED' AND p.deletedAt IS NULL")
    BigDecimal calculateTotalPaidForRental(@Param("rentalId") Long rentalId);

    /**
     * Encuentra pagos pendientes
     */
    @Query("SELECT p FROM Payment p WHERE p.status = 'PENDING' AND p.deletedAt IS NULL ORDER BY p.paymentDate ASC")
    List<Payment> findPendingPayments();

    /**
     * Cuenta pagos por estado
     */
    @Query("SELECT COUNT(p) FROM Payment p WHERE p.status = :status AND p.deletedAt IS NULL")
    long countByStatus(@Param("status") PaymentStatus status);

    /**
     * Calcula total de ingresos en un período
     */
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.paymentDate BETWEEN :startDate AND :endDate AND p.status = 'COMPLETED' AND p.deletedAt IS NULL")
    BigDecimal calculateTotalIncome(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    /**
     * Encuentra los últimos N pagos
     */
    @Query("SELECT p FROM Payment p WHERE p.deletedAt IS NULL ORDER BY p.paymentDate DESC")
    List<Payment> findTopNByOrderByPaymentDateDesc(@Param("limit") int limit);
}
