package com.rentacaresv.security.accesslog.application;

import com.rentacaresv.security.accesslog.domain.AccessEventType;
import com.rentacaresv.security.accesslog.domain.AccessLog;
import com.rentacaresv.security.accesslog.infrastructure.AccessLogRepository;
import com.rentacaresv.user.domain.User;
import com.rentacaresv.user.infrastructure.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Servicio para gestión del registro de accesos
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AccessLogService {

    private final AccessLogRepository accessLogRepository;
    private final UserRepository userRepository;

    // Configuración de retención de logs (días)
    private static final int LOG_RETENTION_DAYS = 90;

    // Límite de intentos fallidos antes de alertar
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int FAILED_ATTEMPTS_WINDOW_HOURS = 1;

    // ========================================
    // Registro de Eventos
    // ========================================

    /**
     * Registra un login exitoso
     */
    @Transactional
    public void logLoginSuccess(User user) {
        HttpServletRequest request = getCurrentRequest();
        String ipAddress = getClientIpAddress(request);
        String userAgent = getUserAgent(request);
        String sessionId = getSessionId(request);

        AccessLog logEntry = AccessLog.loginSuccess(user, ipAddress, userAgent, sessionId);
        accessLogRepository.save(logEntry);

        // Actualizar último login del usuario
        user.updateLastLogin();
        userRepository.save(user);

        log.info("Login exitoso registrado: {} desde {}", user.getUsername(), ipAddress);
    }

    /**
     * Registra un login fallido
     */
    @Transactional
    public void logLoginFailed(String username, String reason) {
        HttpServletRequest request = getCurrentRequest();
        String ipAddress = getClientIpAddress(request);
        String userAgent = getUserAgent(request);

        AccessLog logEntry = AccessLog.loginFailed(username, ipAddress, userAgent, reason);
        accessLogRepository.save(logEntry);

        log.warn("Login fallido registrado: {} desde {} - Razón: {}", username, ipAddress, reason);

        // Verificar si hay demasiados intentos fallidos
        checkFailedAttempts(username, ipAddress);
    }

    /**
     * Registra un logout
     */
    @Transactional
    public void logLogout(User user) {
        HttpServletRequest request = getCurrentRequest();
        String ipAddress = getClientIpAddress(request);
        String sessionId = getSessionId(request);

        AccessLog logEntry = AccessLog.logout(user, ipAddress, sessionId);
        accessLogRepository.save(logEntry);

        log.info("Logout registrado: {}", user != null ? user.getUsername() : "unknown");
    }

    /**
     * Registra un evento genérico
     */
    @Transactional
    public void logEvent(User user, AccessEventType eventType, String details) {
        HttpServletRequest request = getCurrentRequest();

        AccessLog logEntry = AccessLog.builder()
                .user(user)
                .username(user != null ? user.getUsername() : "system")
                .eventType(eventType)
                .ipAddress(getClientIpAddress(request))
                .userAgent(getUserAgent(request))
                .sessionId(getSessionId(request))
                .details(details)
                .success(true)
                .build();

        accessLogRepository.save(logEntry);
        log.info("Evento {} registrado para usuario {}", eventType, user != null ? user.getUsername() : "system");
    }

    // ========================================
    // Consultas
    // ========================================

    /**
     * Obtiene logs paginados
     */
    public Page<AccessLog> getLogs(Pageable pageable) {
        return accessLogRepository.findAll(pageable);
    }

    /**
     * Obtiene los últimos N logs
     */
    public List<AccessLog> getRecentLogs(int limit) {
        return accessLogRepository.findRecentLogs(PageRequest.of(0, limit));
    }

    /**
     * Busca logs con filtros
     */
    public Page<AccessLog> searchLogs(String username, AccessEventType eventType,
                                       LocalDateTime startDate, LocalDateTime endDate,
                                       Pageable pageable) {
        return accessLogRepository.searchLogs(username, eventType, startDate, endDate, pageable);
    }

    /**
     * Obtiene logs de un usuario específico
     */
    public Page<AccessLog> getLogsByUser(Long userId, Pageable pageable) {
        return accessLogRepository.findByUserId(userId, pageable);
    }

    /**
     * Obtiene logs por username
     */
    public Page<AccessLog> getLogsByUsername(String username, Pageable pageable) {
        return accessLogRepository.findByUsernameOrderByTimestampDesc(username, pageable);
    }

    /**
     * Obtiene logs en un rango de fechas
     */
    public Page<AccessLog> getLogsByDateRange(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
        return accessLogRepository.findByDateRange(startDate, endDate, pageable);
    }

    /**
     * Obtiene estadísticas de eventos por tipo en un período
     */
    public Map<AccessEventType, Long> getEventStatistics(LocalDateTime startDate, LocalDateTime endDate) {
        List<Object[]> results = accessLogRepository.countByEventTypeInPeriod(startDate, endDate);
        return results.stream()
                .collect(Collectors.toMap(
                        r -> (AccessEventType) r[0],
                        r -> (Long) r[1]
                ));
    }

    /**
     * Obtiene los usuarios activos en las últimas N horas
     */
    public List<String> getActiveUsers(int hours) {
        LocalDateTime since = LocalDateTime.now().minusHours(hours);
        return accessLogRepository.findActiveUsersSince(since);
    }

    /**
     * Obtiene el último login exitoso de un usuario
     */
    public Optional<AccessLog> getLastSuccessfulLogin(String username) {
        List<AccessLog> logins = accessLogRepository.findLastSuccessfulLogins(username, PageRequest.of(0, 1));
        return logins.isEmpty() ? Optional.empty() : Optional.of(logins.get(0));
    }

    // ========================================
    // Seguridad y Alertas
    // ========================================

    /**
     * Verifica intentos fallidos y genera alertas si es necesario
     */
    private void checkFailedAttempts(String username, String ipAddress) {
        LocalDateTime since = LocalDateTime.now().minusHours(FAILED_ATTEMPTS_WINDOW_HOURS);

        // Verificar por usuario
        long userFailedAttempts = accessLogRepository.countFailedLoginsSince(username, since);
        if (userFailedAttempts >= MAX_FAILED_ATTEMPTS) {
            log.warn("ALERTA DE SEGURIDAD: {} intentos fallidos para usuario {} en la última hora",
                    userFailedAttempts, username);
            // Aquí podrías agregar lógica para bloquear la cuenta o enviar notificación
        }

        // Verificar por IP
        long ipFailedAttempts = accessLogRepository.countFailedLoginsFromIpSince(ipAddress, since);
        if (ipFailedAttempts >= MAX_FAILED_ATTEMPTS * 2) {
            log.warn("ALERTA DE SEGURIDAD: {} intentos fallidos desde IP {} en la última hora",
                    ipFailedAttempts, ipAddress);
            // Aquí podrías agregar lógica para bloquear la IP
        }
    }

    /**
     * Cuenta intentos fallidos de un usuario
     */
    public long countFailedAttempts(String username, int hours) {
        LocalDateTime since = LocalDateTime.now().minusHours(hours);
        return accessLogRepository.countFailedLoginsSince(username, since);
    }

    // ========================================
    // Mantenimiento
    // ========================================

    /**
     * Limpia logs antiguos (ejecutado diariamente a las 3 AM)
     */
    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional
    public void cleanupOldLogs() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(LOG_RETENTION_DAYS);
        int deleted = accessLogRepository.deleteLogsOlderThan(cutoffDate);
        if (deleted > 0) {
            log.info("Limpieza de logs: {} registros eliminados (anteriores a {})", deleted, cutoffDate);
        }
    }

    // ========================================
    // Utilidades
    // ========================================

    /**
     * Obtiene la request HTTP actual
     */
    private HttpServletRequest getCurrentRequest() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return attrs != null ? attrs.getRequest() : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Obtiene la IP del cliente
     */
    private String getClientIpAddress(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }

        // Verificar headers de proxy
        String[] headerNames = {
                "X-Forwarded-For",
                "X-Real-IP",
                "Proxy-Client-IP",
                "WL-Proxy-Client-IP",
                "HTTP_X_FORWARDED_FOR",
                "HTTP_X_FORWARDED",
                "HTTP_FORWARDED_FOR",
                "HTTP_FORWARDED",
                "HTTP_CLIENT_IP"
        };

        for (String header : headerNames) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                // Si hay múltiples IPs, tomar la primera
                if (ip.contains(",")) {
                    ip = ip.split(",")[0].trim();
                }
                return ip;
            }
        }

        return request.getRemoteAddr();
    }

    /**
     * Obtiene el User-Agent del cliente
     */
    private String getUserAgent(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }
        String userAgent = request.getHeader("User-Agent");
        if (userAgent != null && userAgent.length() > 500) {
            userAgent = userAgent.substring(0, 500);
        }
        return userAgent != null ? userAgent : "unknown";
    }

    /**
     * Obtiene el ID de sesión
     */
    private String getSessionId(HttpServletRequest request) {
        if (request == null || request.getSession(false) == null) {
            return null;
        }
        return request.getSession().getId();
    }
}
