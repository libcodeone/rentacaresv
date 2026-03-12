package com.rentacaresv.calendar.application;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.services.calendar.model.EventReminder;
import com.rentacaresv.calendar.domain.GoogleCalendarToken;
import com.rentacaresv.calendar.infrastructure.GoogleCalendarTokenRepository;
import com.rentacaresv.rental.domain.Rental;
import com.rentacaresv.settings.application.SettingsCache;
import com.rentacaresv.user.domain.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Servicio para integración con Google Calendar.
 * Las credenciales se obtienen desde la base de datos (Settings).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GoogleCalendarService {

    private final GoogleCalendarTokenRepository tokenRepository;
    private final SettingsCache settingsCache;

    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final List<String> SCOPES = Collections.singletonList(CalendarScopes.CALENDAR);
    private static final String APPLICATION_NAME = "RentaCar ESV";

    // URL base de la aplicación (para el callback de OAuth)
    @Value("${app.base-url:http://localhost:8091}")
    private String baseUrl;

    /**
     * Verifica si Google Calendar está configurado en Settings
     */
    public boolean isConfigured() {
        return settingsCache.isGoogleCalendarConfigured();
    }

    /**
     * Verifica si un usuario tiene su cuenta de Google vinculada
     */
    public boolean isUserLinked(Long userId) {
        return tokenRepository.existsByUserId(userId);
    }

    /**
     * Obtiene información del token de un usuario
     */
    public Optional<GoogleCalendarToken> getUserToken(Long userId) {
        return tokenRepository.findByUserId(userId);
    }

    // ========================================
    // Métodos para Calendario Empresarial
    // ========================================

    /**
     * Verifica si hay un calendario de empresa configurado
     */
    public boolean hasCompanyCalendar() {
        return settingsCache.hasCompanyCalendar();
    }

    /**
     * Obtiene el token del calendario de la empresa
     */
    public Optional<GoogleCalendarToken> getCompanyCalendarToken() {
        Long companyCalendarUserId = settingsCache.getCompanyCalendarUserId();
        if (companyCalendarUserId == null) {
            return Optional.empty();
        }
        return tokenRepository.findByUserId(companyCalendarUserId);
    }

    /**
     * Obtiene el token efectivo para un usuario:
     * - Si hay calendario de empresa configurado, devuelve ese
     * - Si no, devuelve el token personal del usuario (si existe)
     */
    public Optional<GoogleCalendarToken> getEffectiveToken(Long userId) {
        // Primero verificar si hay calendario de empresa
        if (hasCompanyCalendar()) {
            return getCompanyCalendarToken();
        }
        // Si no, devolver el token personal del usuario
        return getUserToken(userId);
    }

    /**
     * Establece el calendario de un usuario como el calendario de la empresa
     */
    @Transactional
    public void setAsCompanyCalendar(Long userId) {
        var settings = settingsCache.getSettings();
        settings.setCompanyCalendarUserId(userId);
        settingsCache.updateSettings(settings);
        log.info("Calendario del usuario {} establecido como calendario de empresa", userId);
    }

    /**
     * Remueve el calendario de empresa (vuelve al modo individual)
     */
    @Transactional
    public void removeCompanyCalendar() {
        var settings = settingsCache.getSettings();
        settings.setCompanyCalendarUserId(null);
        settingsCache.updateSettings(settings);
        log.info("Calendario de empresa removido");
    }

    /**
     * Genera la URL para autorización OAuth2
     */
    public String getAuthorizationUrl(Long userId) throws GeneralSecurityException, IOException {
        if (!isConfigured()) {
            throw new IllegalStateException("Google Calendar no está configurado. Configure las credenciales en Configuración > Google Calendar.");
        }

        GoogleAuthorizationCodeFlow flow = getFlow();
        String redirectUri = baseUrl + "/api/google-calendar/callback";
        
        return flow.newAuthorizationUrl()
                .setRedirectUri(redirectUri)
                .setState(userId.toString())
                .setAccessType("offline")
                .setApprovalPrompt("force")
                .build();
    }

    /**
     * Procesa el callback de OAuth2 y guarda el token.
     * Si el usuario es admin y no hay calendario de empresa, se establece automáticamente.
     */
    @Transactional
    public void handleCallback(String code, User user) throws GeneralSecurityException, IOException {
        if (!isConfigured()) {
            throw new IllegalStateException("Google Calendar no está configurado");
        }

        GoogleAuthorizationCodeFlow flow = getFlow();
        String redirectUri = baseUrl + "/api/google-calendar/callback";

        GoogleTokenResponse tokenResponse = flow.newTokenRequest(code)
                .setRedirectUri(redirectUri)
                .execute();

        // Obtener email de Google
        String googleEmail = getGoogleEmail(tokenResponse.getAccessToken());

        // Calcular expiración
        LocalDateTime expiresAt = LocalDateTime.now()
                .plusSeconds(tokenResponse.getExpiresInSeconds());

        // Buscar token existente o crear nuevo
        GoogleCalendarToken token = tokenRepository.findByUserId(user.getId())
                .orElse(GoogleCalendarToken.builder()
                        .user(user)
                        .build());

        token.setAccessToken(tokenResponse.getAccessToken());
        token.setRefreshToken(tokenResponse.getRefreshToken());
        token.setExpiresAt(expiresAt);
        token.setGoogleEmail(googleEmail);
        token.setSyncEnabled(true);

        tokenRepository.save(token);
        log.info("Google Calendar vinculado para usuario {} con email {}", user.getUsername(), googleEmail);

        // Si es admin y no hay calendario de empresa, establecerlo automáticamente
        if (user.isAdmin() && !hasCompanyCalendar()) {
            setAsCompanyCalendar(user.getId());
            log.info("Calendario de {} establecido como calendario de empresa", googleEmail);
        }
    }

    /**
     * Desvincula la cuenta de Google de un usuario.
     * Si es el calendario de empresa, también se limpia esa referencia.
     */
    @Transactional
    public void unlinkAccount(Long userId) {
        // Si este usuario es el dueño del calendario de empresa, limpiar la referencia
        Long companyCalendarUserId = settingsCache.getCompanyCalendarUserId();
        if (companyCalendarUserId != null && companyCalendarUserId.equals(userId)) {
            removeCompanyCalendar();
            log.info("Calendario de empresa removido porque el usuario {} se desvincula", userId);
        }
        
        tokenRepository.deleteByUserId(userId);
        log.info("Google Calendar desvinculado para usuario ID {}", userId);
    }

    /**
     * Crea un evento en Google Calendar para una renta.
     * Usa el calendario de empresa si está configurado, sino el del usuario.
     */
    public String createRentalEvent(Rental rental, User user) throws GeneralSecurityException, IOException {
        // Usar el token efectivo (calendario empresa o personal)
        Optional<GoogleCalendarToken> tokenOpt = getEffectiveToken(user.getId());
        if (tokenOpt.isEmpty() || !tokenOpt.get().getSyncEnabled()) {
            log.debug("No hay calendario disponible para crear evento de renta");
            return null;
        }

        GoogleCalendarToken token = tokenOpt.get();
        Calendar service = getCalendarService(token);

        String companyName = settingsCache.getCompanyName();
        
        Event event = new Event()
                .setSummary("🚗 Renta: " + rental.getVehicle().getBrand() + " " + rental.getVehicle().getModel())
                .setDescription(buildEventDescription(rental, companyName))
                .setColorId("9"); // Azul

        // Fecha inicio (todo el día)
        EventDateTime start = new EventDateTime()
                .setDate(new DateTime(rental.getStartDate().toString()))
                .setTimeZone("America/El_Salvador");
        event.setStart(start);

        // Fecha fin (día después del fin de la renta)
        EventDateTime end = new EventDateTime()
                .setDate(new DateTime(rental.getEndDate().plusDays(1).toString()))
                .setTimeZone("America/El_Salvador");
        event.setEnd(end);

        // Recordatorios
        EventReminder[] reminders = new EventReminder[] {
                new EventReminder().setMethod("popup").setMinutes(24 * 60), // 1 día antes
                new EventReminder().setMethod("popup").setMinutes(60), // 1 hora antes
        };
        Event.Reminders eventReminders = new Event.Reminders()
                .setUseDefault(false)
                .setOverrides(Arrays.asList(reminders));
        event.setReminders(eventReminders);

        Event createdEvent = service.events().insert(token.getCalendarId(), event).execute();
        log.info("Evento creado en Google Calendar: {}", createdEvent.getId());
        
        return createdEvent.getId();
    }

    /**
     * Actualiza un evento existente.
     * Usa el calendario de empresa si está configurado.
     */
    public void updateRentalEvent(String eventId, Rental rental, User user) throws GeneralSecurityException, IOException {
        // Usar el token efectivo (calendario empresa o personal)
        Optional<GoogleCalendarToken> tokenOpt = getEffectiveToken(user.getId());
        if (tokenOpt.isEmpty() || !tokenOpt.get().getSyncEnabled()) {
            return;
        }

        GoogleCalendarToken token = tokenOpt.get();
        Calendar service = getCalendarService(token);

        try {
            Event event = service.events().get(token.getCalendarId(), eventId).execute();
            
            String companyName = settingsCache.getCompanyName();
            event.setSummary("🚗 Renta: " + rental.getVehicle().getBrand() + " " + rental.getVehicle().getModel());
            event.setDescription(buildEventDescription(rental, companyName));

            EventDateTime start = new EventDateTime()
                    .setDate(new DateTime(rental.getStartDate().toString()))
                    .setTimeZone("America/El_Salvador");
            event.setStart(start);

            EventDateTime end = new EventDateTime()
                    .setDate(new DateTime(rental.getEndDate().plusDays(1).toString()))
                    .setTimeZone("America/El_Salvador");
            event.setEnd(end);

            service.events().update(token.getCalendarId(), eventId, event).execute();
            log.info("Evento actualizado en Google Calendar: {}", eventId);
        } catch (Exception e) {
            log.warn("No se pudo actualizar el evento {}: {}", eventId, e.getMessage());
        }
    }

    /**
     * Elimina un evento de Google Calendar.
     * Usa el calendario de empresa si está configurado.
     */
    public void deleteRentalEvent(String eventId, User user) throws GeneralSecurityException, IOException {
        // Usar el token efectivo (calendario empresa o personal)
        Optional<GoogleCalendarToken> tokenOpt = getEffectiveToken(user.getId());
        if (tokenOpt.isEmpty()) {
            return;
        }

        GoogleCalendarToken token = tokenOpt.get();
        Calendar service = getCalendarService(token);

        try {
            service.events().delete(token.getCalendarId(), eventId).execute();
            log.info("Evento eliminado de Google Calendar: {}", eventId);
        } catch (Exception e) {
            log.warn("No se pudo eliminar el evento {}: {}", eventId, e.getMessage());
        }
    }

    /**
     * Prueba la conexión con Google Calendar.
     * Usa el calendario de empresa si está configurado.
     */
    public boolean testConnection(Long userId) {
        try {
            // Usar el token efectivo (calendario empresa o personal)
            Optional<GoogleCalendarToken> tokenOpt = getEffectiveToken(userId);
            if (tokenOpt.isEmpty()) {
                return false;
            }

            Calendar service = getCalendarService(tokenOpt.get());
            service.calendarList().list().setMaxResults(1).execute();
            return true;
        } catch (Exception e) {
            log.warn("Error probando conexión con Google Calendar: {}", e.getMessage());
            return false;
        }
    }

    // ========================================
    // Métodos privados
    // ========================================

    /**
     * Crea el flujo de autorización OAuth2 usando credenciales de Settings
     */
    private GoogleAuthorizationCodeFlow getFlow() throws GeneralSecurityException, IOException {
        NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        
        // Obtener credenciales desde Settings (base de datos)
        String clientId = settingsCache.getGoogleClientId();
        String clientSecret = settingsCache.getGoogleClientSecret();
        
        GoogleClientSecrets.Details details = new GoogleClientSecrets.Details()
                .setClientId(clientId)
                .setClientSecret(clientSecret);
        GoogleClientSecrets clientSecrets = new GoogleClientSecrets().setWeb(details);

        return new GoogleAuthorizationCodeFlow.Builder(
                httpTransport, JSON_FACTORY, clientSecrets, SCOPES)
                .setAccessType("offline")
                .build();
    }

    private Calendar getCalendarService(GoogleCalendarToken token) throws GeneralSecurityException, IOException {
        NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();

        // Verificar si necesita refresh
        if (token.needsRefresh() && token.getRefreshToken() != null) {
            refreshToken(token);
        }

        Credential credential = new Credential.Builder(
                com.google.api.client.auth.oauth2.BearerToken.authorizationHeaderAccessMethod())
                .setTransport(httpTransport)
                .setJsonFactory(JSON_FACTORY)
                .build()
                .setAccessToken(token.getAccessToken());

        return new Calendar.Builder(httpTransport, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    @Transactional
    private void refreshToken(GoogleCalendarToken token) {
        try {
            GoogleAuthorizationCodeFlow flow = getFlow();
            TokenResponse response = flow.newTokenRequest(token.getRefreshToken())
                    .setGrantType("refresh_token")
                    .execute();

            token.setAccessToken(response.getAccessToken());
            if (response.getRefreshToken() != null) {
                token.setRefreshToken(response.getRefreshToken());
            }
            token.setExpiresAt(LocalDateTime.now().plusSeconds(response.getExpiresInSeconds()));
            tokenRepository.save(token);
            
            log.info("Token refrescado para usuario {}", token.getUser().getUsername());
        } catch (Exception e) {
            log.error("Error refrescando token: {}", e.getMessage());
        }
    }

    private String getGoogleEmail(String accessToken) {
        try {
            NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
            Credential credential = new Credential.Builder(
                    com.google.api.client.auth.oauth2.BearerToken.authorizationHeaderAccessMethod())
                    .setTransport(httpTransport)
                    .setJsonFactory(JSON_FACTORY)
                    .build()
                    .setAccessToken(accessToken);

            Calendar service = new Calendar.Builder(httpTransport, JSON_FACTORY, credential)
                    .setApplicationName(APPLICATION_NAME)
                    .build();

            return service.calendarList().get("primary").execute().getSummary();
        } catch (Exception e) {
            log.warn("No se pudo obtener email de Google: {}", e.getMessage());
            return "Cuenta vinculada";
        }
    }

    /**
     * Obtiene los eventos del calendario de Google del usuario para un rango de fechas.
     * Esta es la alternativa cuando el iframe no funciona.
     */
    public List<GoogleCalendarEventDTO> getUserEvents(Long userId, LocalDate startDate, LocalDate endDate) {
        List<GoogleCalendarEventDTO> result = new ArrayList<>();
        
        try {
            // Usar el token efectivo (calendario empresa o personal)
            Optional<GoogleCalendarToken> tokenOpt = getEffectiveToken(userId);
            if (tokenOpt.isEmpty() || !tokenOpt.get().getSyncEnabled()) {
                log.debug("No hay calendario disponible para usuario {}", userId);
                return result;
            }

            GoogleCalendarToken token = tokenOpt.get();
            Calendar service = getCalendarService(token);

            // Convertir fechas a DateTime de Google
            DateTime timeMin = new DateTime(startDate.atStartOfDay(ZoneId.of("America/El_Salvador")).toInstant().toEpochMilli());
            DateTime timeMax = new DateTime(endDate.plusDays(1).atStartOfDay(ZoneId.of("America/El_Salvador")).toInstant().toEpochMilli());

            // Obtener eventos
            com.google.api.services.calendar.model.Events events = service.events()
                    .list(token.getCalendarId())
                    .setTimeMin(timeMin)
                    .setTimeMax(timeMax)
                    .setOrderBy("startTime")
                    .setSingleEvents(true)
                    .setMaxResults(100)
                    .execute();

            if (events.getItems() != null) {
                for (Event event : events.getItems()) {
                    GoogleCalendarEventDTO dto = convertToDTO(event);
                    if (dto != null) {
                        result.add(dto);
                    }
                }
            }

            log.debug("Obtenidos {} eventos de Google Calendar para usuario {}", result.size(), userId);
        } catch (Exception e) {
            log.error("Error obteniendo eventos de Google Calendar: {}", e.getMessage());
        }

        return result;
    }

    /**
     * Convierte un evento de Google Calendar a DTO
     */
    private GoogleCalendarEventDTO convertToDTO(Event event) {
        try {
            LocalDate startDate;
            LocalDate endDate;
            boolean allDay = false;

            // Manejar eventos de todo el día vs eventos con hora
            if (event.getStart().getDate() != null) {
                // Evento de todo el día
                startDate = LocalDate.parse(event.getStart().getDate().toStringRfc3339().substring(0, 10));
                endDate = LocalDate.parse(event.getEnd().getDate().toStringRfc3339().substring(0, 10));
                allDay = true;
            } else if (event.getStart().getDateTime() != null) {
                // Evento con hora específica
                startDate = LocalDate.parse(event.getStart().getDateTime().toStringRfc3339().substring(0, 10));
                endDate = LocalDate.parse(event.getEnd().getDateTime().toStringRfc3339().substring(0, 10));
            } else {
                return null;
            }

            return GoogleCalendarEventDTO.builder()
                    .id(event.getId())
                    .title(event.getSummary() != null ? event.getSummary() : "(Sin título)")
                    .description(event.getDescription())
                    .startDate(startDate)
                    .endDate(endDate)
                    .allDay(allDay)
                    .color(event.getColorId() != null ? getColorFromId(event.getColorId()) : "#4285F4")
                    .htmlLink(event.getHtmlLink())
                    .build();
        } catch (Exception e) {
            log.warn("Error convirtiendo evento {}: {}", event.getId(), e.getMessage());
            return null;
        }
    }

    /**
     * Convierte el colorId de Google a un color hex
     */
    private String getColorFromId(String colorId) {
        // Colores de Google Calendar (aproximados)
        return switch (colorId) {
            case "1" -> "#7986CB"; // Lavanda
            case "2" -> "#33B679"; // Salvia
            case "3" -> "#8E24AA"; // Uva
            case "4" -> "#E67C73"; // Flamenco
            case "5" -> "#F6BF26"; // Banana
            case "6" -> "#F4511E"; // Mandarina
            case "7" -> "#039BE5"; // Pavo real
            case "8" -> "#616161"; // Grafito
            case "9" -> "#3F51B5"; // Arándano
            case "10" -> "#0B8043"; // Albahaca
            case "11" -> "#D50000"; // Tomate
            default -> "#4285F4"; // Azul Google por defecto
        };
    }

    private String buildEventDescription(Rental rental, String companyName) {
        StringBuilder sb = new StringBuilder();
        sb.append("📋 Contrato: ").append(rental.getContractNumber()).append("\n\n");
        sb.append("👤 Cliente: ").append(rental.getCustomer().getFullName()).append("\n");
        sb.append("📱 Teléfono: ").append(rental.getCustomer().getPhone()).append("\n\n");
        sb.append("🚗 Vehículo: ").append(rental.getVehicle().getBrand())
                .append(" ").append(rental.getVehicle().getModel())
                .append(" ").append(rental.getVehicle().getYear()).append("\n");
        sb.append("🔢 Placa: ").append(rental.getVehicle().getLicensePlate()).append("\n\n");
        sb.append("📅 Período: ").append(rental.getStartDate())
                .append(" al ").append(rental.getEndDate()).append("\n");
        sb.append("💰 Total: $").append(rental.getTotalAmount()).append("\n\n");
        sb.append("---\n");
        sb.append("Generado por ").append(companyName);
        return sb.toString();
    }
}
