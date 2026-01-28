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
import java.time.LocalDateTime;
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
     * Procesa el callback de OAuth2 y guarda el token
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
    }

    /**
     * Desvincula la cuenta de Google de un usuario
     */
    @Transactional
    public void unlinkAccount(Long userId) {
        tokenRepository.deleteByUserId(userId);
        log.info("Google Calendar desvinculado para usuario ID {}", userId);
    }

    /**
     * Crea un evento en Google Calendar para una renta
     */
    public String createRentalEvent(Rental rental, User user) throws GeneralSecurityException, IOException {
        Optional<GoogleCalendarToken> tokenOpt = tokenRepository.findByUserId(user.getId());
        if (tokenOpt.isEmpty() || !tokenOpt.get().getSyncEnabled()) {
            log.debug("Usuario {} no tiene Google Calendar vinculado o sincronización desactivada", user.getUsername());
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
     * Actualiza un evento existente
     */
    public void updateRentalEvent(String eventId, Rental rental, User user) throws GeneralSecurityException, IOException {
        Optional<GoogleCalendarToken> tokenOpt = tokenRepository.findByUserId(user.getId());
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
     * Elimina un evento de Google Calendar
     */
    public void deleteRentalEvent(String eventId, User user) throws GeneralSecurityException, IOException {
        Optional<GoogleCalendarToken> tokenOpt = tokenRepository.findByUserId(user.getId());
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
     * Prueba la conexión con Google Calendar
     */
    public boolean testConnection(Long userId) {
        try {
            Optional<GoogleCalendarToken> tokenOpt = tokenRepository.findByUserId(userId);
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
