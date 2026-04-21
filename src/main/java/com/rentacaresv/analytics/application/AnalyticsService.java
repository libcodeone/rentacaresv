package com.rentacaresv.analytics.application;

import com.rentacaresv.analytics.domain.AnalyticsEvent;
import com.rentacaresv.analytics.infrastructure.persistence.AnalyticsEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsService {

    private final AnalyticsEventRepository repo;

    // ─── Guardar evento ────────────────────────────────────────────────────────

    public void save(AnalyticsEvent event) {
        try {
            repo.save(event);
        } catch (Exception e) {
            log.warn("Error guardando evento de analytics: {}", e.getMessage());
        }
    }

    // ─── Dashboard ─────────────────────────────────────────────────────────────

    public DashboardStats getStats(LocalDate from, LocalDate to) {
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end   = to.atTime(LocalTime.MAX);

        long uniqueVisitors        = nvl(repo.countUniqueVisitors(start, end));
        long vehicleDetailViews    = nvl(repo.countByType("vehicle_detail",        start, end));
        long whatsappClicks        = nvl(repo.countByType("whatsapp_click",        start, end));
        long reserveClicks         = nvl(repo.countByType("reserve_click",         start, end));
        long reservationsCompleted = nvl(repo.countByType("reservation_complete",  start, end));

        List<StepStat> funnel    = buildStepFunnel(repo.findReservationStepFunnel(start, end));
        List<VehicleStat> topVehicles = buildTopVehicles(repo.findTopVehicles(start, end));

        return new DashboardStats(
                uniqueVisitors,
                vehicleDetailViews,
                whatsappClicks,
                reserveClicks,
                reservationsCompleted,
                funnel,
                topVehicles
        );
    }

    // ─── Helpers privados ──────────────────────────────────────────────────────

    private List<StepStat> buildStepFunnel(List<Object[]> rows) {
        List<StepStat> list = new ArrayList<>();
        for (Object[] row : rows) {
            int    stepNumber = row[0] != null ? ((Number) row[0]).intValue() : 0;
            String stepName   = row[1] != null ? row[1].toString() : "Paso " + stepNumber;
            long   count      = row[2] != null ? ((Number) row[2]).longValue() : 0L;
            list.add(new StepStat(stepNumber, stepName, count));
        }
        return list;
    }

    private List<VehicleStat> buildTopVehicles(List<Object[]> rows) {
        List<VehicleStat> list = new ArrayList<>();
        for (Object[] row : rows) {
            String vehicleName  = row[0] != null ? row[0].toString() : "—";
            long   views        = row[1] != null ? ((Number) row[1]).longValue() : 0L;
            long   waClicks     = row[2] != null ? ((Number) row[2]).longValue() : 0L;
            long   reserveClks  = row[3] != null ? ((Number) row[3]).longValue() : 0L;
            list.add(new VehicleStat(vehicleName, views, waClicks, reserveClks));
        }
        return list;
    }

    private long nvl(Long value) {
        return value != null ? value : 0L;
    }

    // ─── Records de datos ──────────────────────────────────────────────────────

    public record DashboardStats(
            long uniqueVisitors,
            long vehicleDetailViews,
            long whatsappClicks,
            long reserveClicks,
            long reservationsCompleted,
            List<StepStat> reservationFunnel,
            List<VehicleStat> topVehicles
    ) {}

    public record StepStat(int stepNumber, String stepName, long count) {}

    public record VehicleStat(String vehicleName, long views, long waClicks, long reserveClicks) {}
}
