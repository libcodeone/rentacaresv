package com.rentacaresv.analytics.infrastructure.persistence;

import com.rentacaresv.analytics.domain.AnalyticsEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AnalyticsEventRepository extends JpaRepository<AnalyticsEvent, Long> {

    /** Visitantes únicos (session_ids distintos) en el rango de fechas */
    @Query(value = """
            SELECT COUNT(DISTINCT session_id)
            FROM analytics_event
            WHERE created_at BETWEEN :from AND :to
              AND session_id IS NOT NULL
            """, nativeQuery = true)
    Long countUniqueVisitors(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    /** Cantidad de eventos de un tipo específico */
    @Query(value = """
            SELECT COUNT(*)
            FROM analytics_event
            WHERE event_type = :type
              AND created_at BETWEEN :from AND :to
            """, nativeQuery = true)
    Long countByType(@Param("type") String type,
                     @Param("from") LocalDateTime from,
                     @Param("to") LocalDateTime to);

    /**
     * Top vehículos: vistas de detalle, clicks WhatsApp y clicks Reservar.
     * Columnas devueltas: vehicle_name, views, wa_clicks, reserve_clicks
     */
    @Query(value = """
            SELECT
                vehicle_name,
                SUM(CASE WHEN event_type = 'vehicle_detail' THEN 1 ELSE 0 END)  AS views,
                SUM(CASE WHEN event_type = 'whatsapp_click'  THEN 1 ELSE 0 END) AS wa_clicks,
                SUM(CASE WHEN event_type = 'reserve_click'   THEN 1 ELSE 0 END) AS reserve_clicks
            FROM analytics_event
            WHERE vehicle_name IS NOT NULL
              AND created_at BETWEEN :from AND :to
            GROUP BY vehicle_name
            ORDER BY views DESC
            LIMIT 10
            """, nativeQuery = true)
    List<Object[]> findTopVehicles(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    /**
     * Embudo de pasos del wizard de reserva.
     * Columnas: step_number, step_name, cnt
     */
    @Query(value = """
            SELECT step_number, step_name, COUNT(*) AS cnt
            FROM analytics_event
            WHERE event_type = 'reservation_step'
              AND created_at BETWEEN :from AND :to
            GROUP BY step_number, step_name
            ORDER BY step_number
            """, nativeQuery = true)
    List<Object[]> findReservationStepFunnel(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
