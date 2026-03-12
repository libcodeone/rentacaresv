package com.rentacaresv.security.accesslog.infrastructure;

import com.rentacaresv.security.accesslog.domain.AccessEventType;
import com.rentacaresv.security.accesslog.domain.AccessLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repositorio de AccessLog (Infrastructure Layer)
 */
public interface AccessLogRepository extends JpaRepository<AccessLog, Long>, JpaSpecificationExecutor<AccessLog> {

    /**
     * Encuentra logs por usuario
     */
    Page<AccessLog> findByUserId(Long userId, Pageable pageable);

    /**
     * Encuentra logs por username
     */
    Page<AccessLog> findByUsernameOrderByTimestampDesc(String username, Pageable pageable);

    /**
     * Encuentra logs por tipo de evento
     */
    Page<AccessLog> findByEventTypeOrderByTimestampDesc(AccessEventType eventType, Pageable pageable);

    /**
     * Encuentra logs en un rango de fechas
     */
    @Query("SELECT a FROM AccessLog a WHERE a.timestamp BETWEEN :startDate AND :endDate ORDER BY a.timestamp DESC")
    Page<AccessLog> findByDateRange(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);

    /**
     * Encuentra los últimos N logs
     */
    @Query("SELECT a FROM AccessLog a ORDER BY a.timestamp DESC")
    List<AccessLog> findRecentLogs(Pageable pageable);

    /**
     * Cuenta intentos de login fallidos para un usuario en las últimas N horas
     */
    @Query("SELECT COUNT(a) FROM AccessLog a WHERE a.username = :username " +
           "AND a.eventType = 'LOGIN_FAILED' AND a.timestamp > :since")
    long countFailedLoginsSince(@Param("username") String username, @Param("since") LocalDateTime since);

    /**
     * Encuentra logs por IP
     */
    Page<AccessLog> findByIpAddressOrderByTimestampDesc(String ipAddress, Pageable pageable);

    /**
     * Cuenta intentos de login fallidos desde una IP en las últimas N horas
     */
    @Query("SELECT COUNT(a) FROM AccessLog a WHERE a.ipAddress = :ipAddress " +
           "AND a.eventType = 'LOGIN_FAILED' AND a.timestamp > :since")
    long countFailedLoginsFromIpSince(@Param("ipAddress") String ipAddress, @Param("since") LocalDateTime since);

    /**
     * Búsqueda general con filtros
     */
    @Query("SELECT a FROM AccessLog a WHERE " +
           "(:username IS NULL OR LOWER(a.username) LIKE LOWER(CONCAT('%', :username, '%'))) AND " +
           "(:eventType IS NULL OR a.eventType = :eventType) AND " +
           "(:startDate IS NULL OR a.timestamp >= :startDate) AND " +
           "(:endDate IS NULL OR a.timestamp <= :endDate) " +
           "ORDER BY a.timestamp DESC")
    Page<AccessLog> searchLogs(
            @Param("username") String username,
            @Param("eventType") AccessEventType eventType,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);

    /**
     * Elimina logs antiguos (para mantenimiento)
     */
    @Modifying
    @Query("DELETE FROM AccessLog a WHERE a.timestamp < :beforeDate")
    int deleteLogsOlderThan(@Param("beforeDate") LocalDateTime beforeDate);

    /**
     * Estadísticas: cuenta por tipo de evento en un período
     */
    @Query("SELECT a.eventType, COUNT(a) FROM AccessLog a " +
           "WHERE a.timestamp BETWEEN :startDate AND :endDate " +
           "GROUP BY a.eventType")
    List<Object[]> countByEventTypeInPeriod(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * Últimos logins exitosos de un usuario
     */
    @Query("SELECT a FROM AccessLog a WHERE a.username = :username " +
           "AND a.eventType = 'LOGIN_SUCCESS' ORDER BY a.timestamp DESC")
    List<AccessLog> findLastSuccessfulLogins(@Param("username") String username, Pageable pageable);

    /**
     * Usuarios activos en las últimas N horas
     */
    @Query("SELECT DISTINCT a.username FROM AccessLog a " +
           "WHERE a.eventType = 'LOGIN_SUCCESS' AND a.timestamp > :since")
    List<String> findActiveUsersSince(@Param("since") LocalDateTime since);
}
