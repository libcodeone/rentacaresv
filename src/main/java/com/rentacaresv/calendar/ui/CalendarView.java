package com.rentacaresv.calendar.ui;

import com.rentacaresv.calendar.application.CalendarEventDTO;
import com.rentacaresv.calendar.application.GoogleCalendarEventDTO;
import com.rentacaresv.calendar.application.GoogleCalendarService;
import com.rentacaresv.calendar.domain.GoogleCalendarToken;
import com.rentacaresv.components.calendar.CustomCalendar;
import com.rentacaresv.rental.application.RentalDTO;
import com.rentacaresv.rental.application.RentalService;
import com.rentacaresv.rental.ui.RentalDetailsDialog;
import com.rentacaresv.security.AuthenticatedUser;
import com.rentacaresv.user.domain.User;
import com.rentacaresv.views.MainLayout;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.router.*;
import jakarta.annotation.security.RolesAllowed;
import lombok.extern.slf4j.Slf4j;
import org.vaadin.lineawesome.LineAwesomeIconUrl;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Vista del Calendario de Rentas con integración a Google Calendar
 */
@PageTitle("Calendario")
@Route(value = "calendar", layout = MainLayout.class)
@Menu(order = 1, icon = LineAwesomeIconUrl.CALENDAR)
@RolesAllowed({"ADMIN", "OPERATOR"})
@Slf4j
public class CalendarView extends VerticalLayout implements HasUrlParameter<String>, BeforeEnterObserver {

    private final RentalService rentalService;
    private final GoogleCalendarService googleCalendarService;
    private final AuthenticatedUser authenticatedUser;

    private CustomCalendar calendar;
    private Div googleCalendarStatus;
    private Button linkGoogleButton;
    private Button unlinkGoogleButton;
    private User currentUser;
    private TabSheet tabSheet;
    private Div googleCalendarIframe;

    public CalendarView(RentalService rentalService,
            GoogleCalendarService googleCalendarService,
            AuthenticatedUser authenticatedUser) {
        this.rentalService = rentalService;
        this.googleCalendarService = googleCalendarService;
        this.authenticatedUser = authenticatedUser;

        setSizeFull();
        setPadding(true);
        setSpacing(true);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        Optional<User> userOpt = authenticatedUser.get();
        if (userOpt.isEmpty()) {
            event.forwardTo("login");
            return;
        }
        currentUser = userOpt.get();
        buildView();
    }

    @Override
    public void setParameter(BeforeEvent event, @OptionalParameter String parameter) {
        // Manejar parámetros de la URL (success, error del callback de Google)
        Location location = event.getLocation();
        Map<String, List<String>> params = location.getQueryParameters().getParameters();

        if (params.containsKey("success")) {
            String success = params.get("success").get(0);
            if ("linked".equals(success)) {
                Notification.show("¡Google Calendar vinculado exitosamente!", 4000, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            }
        }

        if (params.containsKey("error")) {
            String error = params.get("error").get(0);
            String message = switch (error) {
                case "auth_denied" -> "Autorización denegada por el usuario";
                case "invalid_callback" -> "Callback inválido";
                case "user_not_found" -> "Usuario no encontrado";
                case "processing_error" -> "Error procesando la autorización";
                default -> "Error desconocido";
            };
            Notification.show("Error: " + message, 5000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void buildView() {
        removeAll();

        // Header
        HorizontalLayout header = createHeader();

        // Panel de Google Calendar
        Div googlePanel = createGoogleCalendarPanel();

        // TabSheet con Calendario de Rentas y Google Calendar
        tabSheet = createTabSheet();

        add(header, googlePanel, tabSheet);
        setFlexGrow(1, tabSheet);
    }

    private HorizontalLayout createHeader() {
        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);

        H2 title = new H2("Calendario de Rentas");
        title.getStyle().set("margin", "0");

        // Botón refrescar
        Button refreshBtn = new Button("Actualizar", VaadinIcon.REFRESH.create());
        refreshBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        refreshBtn.addClickListener(e -> loadCalendarEvents());

        // Leyenda
        HorizontalLayout legend = createLegend();

        HorizontalLayout rightSide = new HorizontalLayout(legend, refreshBtn);
        rightSide.setAlignItems(FlexComponent.Alignment.CENTER);
        rightSide.setSpacing(true);

        header.add(title, rightSide);
        return header;
    }

    private Div createGoogleCalendarPanel() {
        Div panel = new Div();
        panel.addClassName("google-calendar-panel");
        panel.getStyle()
                .set("background", "var(--lumo-contrast-5pct)")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("padding", "var(--lumo-space-m)")
                .set("margin-bottom", "var(--lumo-space-m)");

        HorizontalLayout content = new HorizontalLayout();
        content.setWidthFull();
        content.setAlignItems(FlexComponent.Alignment.CENTER);
        content.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);

        // Lado izquierdo: Ícono de Google + estado
        HorizontalLayout leftSide = new HorizontalLayout();
        leftSide.setAlignItems(FlexComponent.Alignment.CENTER);
        leftSide.setSpacing(true);

        // Ícono de Google Calendar
        Div googleIcon = new Div();
        googleIcon.getStyle()
                .set("width", "32px")
                .set("height", "32px")
                .set("background-image",
                        "url('https://upload.wikimedia.org/wikipedia/commons/a/a5/Google_Calendar_icon_%282020%29.svg')")
                .set("background-size", "contain")
                .set("background-repeat", "no-repeat");

        VerticalLayout statusLayout = new VerticalLayout();
        statusLayout.setPadding(false);
        statusLayout.setSpacing(false);

        Span titleSpan = new Span("Google Calendar");
        titleSpan.getStyle().set("font-weight", "600");

        googleCalendarStatus = new Div();
        googleCalendarStatus.getStyle().set("font-size", "var(--lumo-font-size-s)");

        statusLayout.add(titleSpan, googleCalendarStatus);
        leftSide.add(googleIcon, statusLayout);

        // Lado derecho: Botones (solo visibles para admins)
        HorizontalLayout rightSide = new HorizontalLayout();
        rightSide.setSpacing(true);

        // Solo los administradores pueden vincular/desvincular el calendario de empresa
        if (currentUser.isAdmin()) {
            linkGoogleButton = new Button("Vincular Calendario Empresa", VaadinIcon.LINK.create());
            linkGoogleButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            linkGoogleButton.addClickListener(e -> linkGoogleCalendar());

            unlinkGoogleButton = new Button("Desvincular", VaadinIcon.UNLINK.create());
            unlinkGoogleButton.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);
            unlinkGoogleButton.addClickListener(e -> confirmUnlinkGoogle());

            rightSide.add(linkGoogleButton, unlinkGoogleButton);
        }

        content.add(leftSide, rightSide);
        panel.add(content);

        // Actualizar estado
        updateGoogleCalendarStatus();

        return panel;
    }

    private void updateGoogleCalendarStatus() {
        boolean isAdmin = currentUser.isAdmin();
        
        if (!googleCalendarService.isConfigured()) {
            googleCalendarStatus.setText("No configurado (contacte al administrador)");
            googleCalendarStatus.getStyle().set("color", "var(--lumo-secondary-text-color)");
            if (isAdmin && linkGoogleButton != null) {
                linkGoogleButton.setEnabled(false);
            }
            if (unlinkGoogleButton != null) {
                unlinkGoogleButton.setVisible(false);
            }
            return;
        }

        // Verificar primero si hay calendario de empresa
        if (googleCalendarService.hasCompanyCalendar()) {
            Optional<GoogleCalendarToken> companyTokenOpt = googleCalendarService.getCompanyCalendarToken();
            
            if (companyTokenOpt.isPresent()) {
                GoogleCalendarToken token = companyTokenOpt.get();
                String email = token.getGoogleEmail() != null ? token.getGoogleEmail() : "Calendario empresa";

                if (token.getSyncEnabled()) {
                    googleCalendarStatus.setText("🏢 Calendario Empresa: " + email);
                    googleCalendarStatus.getStyle().set("color", "var(--lumo-success-color)");
                } else {
                    googleCalendarStatus.setText("🏢 Calendario Empresa (pausado): " + email);
                    googleCalendarStatus.getStyle().set("color", "var(--lumo-warning-color)");
                }

                // Solo admins pueden ver los botones
                if (isAdmin) {
                    if (linkGoogleButton != null) linkGoogleButton.setVisible(false);
                    if (unlinkGoogleButton != null) unlinkGoogleButton.setVisible(true);
                }
            }
        } else {
            // No hay calendario de empresa configurado
            if (isAdmin) {
                // Admin: puede vincular
                googleCalendarStatus.setText("Sin calendario de empresa - Vincule una cuenta para todos los usuarios");
                googleCalendarStatus.getStyle().set("color", "var(--lumo-secondary-text-color)");
                if (linkGoogleButton != null) linkGoogleButton.setVisible(true);
                if (unlinkGoogleButton != null) unlinkGoogleButton.setVisible(false);
            } else {
                // Usuario normal: mostrar mensaje de espera
                googleCalendarStatus.setText("Esperando que un administrador vincule el calendario de la empresa");
                googleCalendarStatus.getStyle().set("color", "var(--lumo-secondary-text-color)");
            }
        }
    }

    private void linkGoogleCalendar() {
        try {
            String authUrl = googleCalendarService.getAuthorizationUrl(currentUser.getId());
            // Redirigir a la página de autorización de Google
            UI.getCurrent().getPage().setLocation(authUrl);
        } catch (Exception e) {
            log.error("Error generando URL de autorización: {}", e.getMessage(), e);
            Notification.show("Error: " + e.getMessage(), 5000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void confirmUnlinkGoogle() {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Desvincular Google Calendar");
        dialog.setText("¿Está seguro de desvincular su cuenta de Google Calendar? " +
                "Las rentas ya no se sincronizarán automáticamente.");
        dialog.setCancelable(true);
        dialog.setCancelText("Cancelar");
        dialog.setConfirmText("Desvincular");
        dialog.setConfirmButtonTheme("error primary");
        dialog.addConfirmListener(e -> unlinkGoogleCalendar());
        dialog.open();
    }

    private void unlinkGoogleCalendar() {
        try {
            googleCalendarService.unlinkAccount(currentUser.getId());
            updateGoogleCalendarStatus();
            Notification.show("Google Calendar desvinculado", 3000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        } catch (Exception e) {
            log.error("Error desvinculando Google Calendar: {}", e.getMessage(), e);
            Notification.show("Error: " + e.getMessage(), 5000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private TabSheet createTabSheet() {
        TabSheet tabs = new TabSheet();
        tabs.setSizeFull();

        // Pestaña 1: Calendario de Rentas (Vaadin)
        VerticalLayout rentalCalendarTab = createRentalCalendarTab();
        tabs.add("📅 Calendario de Rentas", rentalCalendarTab);

        // Pestaña 2: Google Calendar (Iframe)
        VerticalLayout googleCalendarTab = createGoogleCalendarTab();
        tabs.add("🗓️ Google Calendar", googleCalendarTab);

        return tabs;
    }

    private VerticalLayout createRentalCalendarTab() {
        calendar = new CustomCalendar();
        calendar.setSizeFull();

        loadCalendarEvents();

        calendar.addEventClickListener(event -> {
            Object data = event.getData();
            if (data instanceof Long rentalId) {
                openRentalDetails(rentalId);
            }
        });

        VerticalLayout container = new VerticalLayout();
        container.setSizeFull();
        container.setPadding(false);
        container.setSpacing(false);
        container.add(calendar);
        container.setFlexGrow(1, calendar);

        return container;
    }

    private VerticalLayout createGoogleCalendarTab() {
        VerticalLayout container = new VerticalLayout();
        container.setSizeFull();
        container.setPadding(false);
        container.setSpacing(false);

        // Usar el token efectivo (calendario empresa o personal)
        Optional<GoogleCalendarToken> tokenOpt = googleCalendarService.getEffectiveToken(currentUser.getId());

        if (tokenOpt.isEmpty()) {
            // Mostrar mensaje según el rol del usuario
            container.add(createNotLinkedMessage());
        } else {
            // Crear contenido con iframe + fallback
            container.add(createGoogleCalendarContent(tokenOpt.get()));
        }

        return container;
    }

    /**
     * Crea el mensaje cuando no está vinculado Google Calendar.
     * Muestra diferente contenido según si el usuario es admin o no.
     */
    private VerticalLayout createNotLinkedMessage() {
        VerticalLayout messageLayout = new VerticalLayout();
        messageLayout.setSizeFull();
        messageLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        messageLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);

        Div icon = new Div();
        icon.getStyle()
                .set("width", "64px")
                .set("height", "64px")
                .set("background-image",
                        "url('https://upload.wikimedia.org/wikipedia/commons/a/a5/Google_Calendar_icon_%282020%29.svg')")
                .set("background-size", "contain")
                .set("background-repeat", "no-repeat")
                .set("margin-bottom", "var(--lumo-space-m)");

        if (currentUser.isAdmin()) {
            // Admin: puede vincular el calendario de empresa
            H3 title = new H3("Vincular Calendario de Empresa");
            title.getStyle().set("margin", "var(--lumo-space-s) 0");

            Paragraph description = new Paragraph(
                    "Vincule una cuenta de Google para establecer el calendario de la empresa. " +
                    "Todos los usuarios verán este calendario automáticamente.");
            description.getStyle().set("color", "var(--lumo-secondary-text-color)");

            Button linkButton = new Button("Vincular Calendario Empresa", VaadinIcon.LINK.create());
            linkButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_LARGE);
            linkButton.addClickListener(e -> linkGoogleCalendar());

            messageLayout.add(icon, title, description, linkButton);
        } else {
            // Usuario normal: esperar a que el admin vincule
            H3 title = new H3("Calendario no disponible");
            title.getStyle().set("margin", "var(--lumo-space-s) 0");

            Paragraph description = new Paragraph(
                    "El administrador aún no ha vinculado el calendario de la empresa. " +
                    "Contacte al administrador para habilitar esta funcionalidad.");
            description.getStyle().set("color", "var(--lumo-secondary-text-color)");

            messageLayout.add(icon, title, description);
        }
        
        return messageLayout;
    }

    /**
     * Crea el contenido principal de Google Calendar con iframe y fallback
     */
    private VerticalLayout createGoogleCalendarContent(GoogleCalendarToken token) {
        VerticalLayout content = new VerticalLayout();
        content.setSizeFull();
        content.setPadding(true);
        content.setSpacing(true);

        // Banner informativo sobre el iframe
        Div infoBanner = createIframeInfoBanner(token);
        content.add(infoBanner);

        // Tabs para alternar entre iframe y lista de eventos
        TabSheet viewTabs = new TabSheet();
        viewTabs.setSizeFull();

        // Tab 1: Vista Iframe (puede fallar con 403)
        VerticalLayout iframeTab = new VerticalLayout();
        iframeTab.setSizeFull();
        iframeTab.setPadding(false);
        googleCalendarIframe = createGoogleCalendarIframe(token);
        iframeTab.add(googleCalendarIframe);
        iframeTab.setFlexGrow(1, googleCalendarIframe);
        viewTabs.add("📅 Vista Calendario", iframeTab);

        // Tab 2: Vista Lista (fallback usando API)
        VerticalLayout listTab = createEventListView(token);
        viewTabs.add("📋 Vista Lista (API)", listTab);

        content.add(viewTabs);
        content.setFlexGrow(1, viewTabs);

        return content;
    }

    /**
     * Banner informativo sobre posibles problemas con el iframe
     */
    private Div createIframeInfoBanner(GoogleCalendarToken token) {
        Div banner = new Div();
        banner.getStyle()
                .set("background", "linear-gradient(135deg, #FFF3E0 0%, #FFE0B2 100%)")
                .set("border", "1px solid #FFB74D")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("padding", "var(--lumo-space-m)")
                .set("margin-bottom", "var(--lumo-space-s)");

        HorizontalLayout bannerContent = new HorizontalLayout();
        bannerContent.setWidthFull();
        bannerContent.setAlignItems(FlexComponent.Alignment.CENTER);
        bannerContent.setSpacing(true);

        // Icono de información
        Span infoIcon = new Span("💡");
        infoIcon.getStyle().set("font-size", "1.5em");

        // Texto explicativo
        VerticalLayout textContent = new VerticalLayout();
        textContent.setPadding(false);
        textContent.setSpacing(false);

        Span title = new Span("¿El calendario no carga?");
        title.getStyle()
                .set("font-weight", "600")
                .set("color", "#E65100");

        Div explanation = new Div();
        explanation.getStyle()
                .set("font-size", "var(--lumo-font-size-s)")
                .set("color", "#5D4037")
                .set("line-height", "1.4");
        
        String googleEmail = token.getGoogleEmail() != null ? token.getGoogleEmail() : "tu cuenta de Google";
        explanation.setText(
            "Si ves un error 403, necesitas iniciar sesión en Google en este navegador con la cuenta: " + googleEmail + 
            ". Alternativamente, usa la pestaña 'Vista Lista (API)' que funciona sin necesidad de sesión del navegador."
        );

        textContent.add(title, explanation);

        // Botón para abrir Google en nueva pestaña
        Button openGoogleBtn = new Button("Abrir Google", VaadinIcon.EXTERNAL_LINK.create());
        openGoogleBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        openGoogleBtn.getStyle().set("color", "#E65100");
        openGoogleBtn.addClickListener(e -> {
            UI.getCurrent().getPage().open("https://accounts.google.com", "_blank");
        });

        bannerContent.add(infoIcon, textContent, openGoogleBtn);
        bannerContent.setFlexGrow(1, textContent);

        banner.add(bannerContent);
        return banner;
    }

    /**
     * Crea la vista de lista de eventos usando la API (fallback)
     */
    private VerticalLayout createEventListView(GoogleCalendarToken token) {
        VerticalLayout container = new VerticalLayout();
        container.setSizeFull();
        container.setPadding(true);
        container.setSpacing(true);

        // Header con controles
        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);

        H4 listTitle = new H4("Eventos de Google Calendar");
        listTitle.getStyle().set("margin", "0");

        Button refreshBtn = new Button("Actualizar", VaadinIcon.REFRESH.create());
        refreshBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);

        header.add(listTitle, refreshBtn);

        // Contenedor de eventos
        Div eventsContainer = new Div();
        eventsContainer.setWidthFull();
        eventsContainer.getStyle()
                .set("overflow-y", "auto")
                .set("max-height", "500px");

        // Cargar eventos
        Runnable loadEvents = () -> {
            eventsContainer.removeAll();
            
            // Mostrar loading
            Div loading = new Div();
            loading.setText("Cargando eventos...");
            loading.getStyle()
                    .set("text-align", "center")
                    .set("padding", "var(--lumo-space-l)")
                    .set("color", "var(--lumo-secondary-text-color)");
            eventsContainer.add(loading);

            try {
                // Obtener eventos del mes actual y siguiente
                LocalDate startDate = YearMonth.now().atDay(1);
                LocalDate endDate = startDate.plusMonths(2);

                List<GoogleCalendarEventDTO> events = googleCalendarService.getUserEvents(
                        currentUser.getId(), startDate, endDate);

                eventsContainer.removeAll();

                if (events.isEmpty()) {
                    Div noEvents = new Div();
                    noEvents.setText("No hay eventos en los próximos 2 meses");
                    noEvents.getStyle()
                            .set("text-align", "center")
                            .set("padding", "var(--lumo-space-xl)")
                            .set("color", "var(--lumo-secondary-text-color)");
                    eventsContainer.add(noEvents);
                } else {
                    for (GoogleCalendarEventDTO event : events) {
                        eventsContainer.add(createEventCard(event));
                    }
                }
            } catch (Exception e) {
                eventsContainer.removeAll();
                Div errorDiv = new Div();
                errorDiv.setText("Error al cargar eventos: " + e.getMessage());
                errorDiv.getStyle()
                        .set("color", "var(--lumo-error-color)")
                        .set("padding", "var(--lumo-space-m)");
                eventsContainer.add(errorDiv);
                log.error("Error cargando eventos de Google Calendar", e);
            }
        };

        refreshBtn.addClickListener(e -> loadEvents.run());

        // Cargar eventos inicialmente
        loadEvents.run();

        // Mensaje de éxito
        Div successBanner = new Div();
        successBanner.getStyle()
                .set("background", "#E8F5E9")
                .set("border", "1px solid #81C784")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("padding", "var(--lumo-space-s)")
                .set("margin-bottom", "var(--lumo-space-m)")
                .set("font-size", "var(--lumo-font-size-s)")
                .set("color", "#2E7D32");
        successBanner.setText("✅ Esta vista usa la API de Google Calendar y funciona independientemente de la sesión del navegador.");

        container.add(header, successBanner, eventsContainer);
        container.setFlexGrow(1, eventsContainer);

        return container;
    }

    /**
     * Crea una tarjeta visual para un evento de Google Calendar
     */
    private Div createEventCard(GoogleCalendarEventDTO event) {
        Div card = new Div();
        card.getStyle()
                .set("background", "var(--lumo-base-color)")
                .set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("border-left", "4px solid " + event.getColor())
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("padding", "var(--lumo-space-m)")
                .set("margin-bottom", "var(--lumo-space-s)")
                .set("cursor", "pointer")
                .set("transition", "box-shadow 0.2s");

        // Hover effect via click listener to open in Google
        if (event.getHtmlLink() != null) {
            card.getElement().addEventListener("click", e -> {
                UI.getCurrent().getPage().open(event.getHtmlLink(), "_blank");
            });
            card.getStyle().set("cursor", "pointer");
        }

        HorizontalLayout content = new HorizontalLayout();
        content.setWidthFull();
        content.setAlignItems(FlexComponent.Alignment.CENTER);
        content.setSpacing(true);

        // Fecha
        VerticalLayout dateBox = new VerticalLayout();
        dateBox.setPadding(false);
        dateBox.setSpacing(false);
        dateBox.setAlignItems(FlexComponent.Alignment.CENTER);
        dateBox.setWidth("60px");

        String dayOfMonth = String.valueOf(event.getStartDate().getDayOfMonth());
        String monthName = event.getStartDate().getMonth().toString().substring(0, 3);

        Span daySpan = new Span(dayOfMonth);
        daySpan.getStyle()
                .set("font-size", "1.5em")
                .set("font-weight", "bold")
                .set("color", event.getColor());

        Span monthSpan = new Span(monthName);
        monthSpan.getStyle()
                .set("font-size", "var(--lumo-font-size-xs)")
                .set("color", "var(--lumo-secondary-text-color)")
                .set("text-transform", "uppercase");

        dateBox.add(daySpan, monthSpan);

        // Detalles del evento
        VerticalLayout details = new VerticalLayout();
        details.setPadding(false);
        details.setSpacing(false);

        Span titleSpan = new Span(event.getTitle());
        titleSpan.getStyle()
                .set("font-weight", "500")
                .set("font-size", "var(--lumo-font-size-m)");

        // Rango de fechas
        String dateRange;
        if (event.getStartDate().equals(event.getEndDate()) || 
            event.getEndDate().equals(event.getStartDate().plusDays(1))) {
            dateRange = event.getStartDate().toString();
        } else {
            dateRange = event.getStartDate() + " → " + event.getEndDate().minusDays(1);
        }

        Span dateRangeSpan = new Span(dateRange);
        dateRangeSpan.getStyle()
                .set("font-size", "var(--lumo-font-size-s)")
                .set("color", "var(--lumo-secondary-text-color)");

        details.add(titleSpan, dateRangeSpan);

        // Descripción (si existe)
        if (event.getDescription() != null && !event.getDescription().isBlank()) {
            String shortDesc = event.getDescription().length() > 100 
                    ? event.getDescription().substring(0, 100) + "..." 
                    : event.getDescription();
            Span descSpan = new Span(shortDesc);
            descSpan.getStyle()
                    .set("font-size", "var(--lumo-font-size-xs)")
                    .set("color", "var(--lumo-tertiary-text-color)")
                    .set("margin-top", "var(--lumo-space-xs)");
            details.add(descSpan);
        }

        // Icono de enlace externo
        Span linkIcon = new Span("↗");
        linkIcon.getStyle()
                .set("color", "var(--lumo-secondary-text-color)")
                .set("font-size", "1.2em");

        content.add(dateBox, details, linkIcon);
        content.setFlexGrow(1, details);

        card.add(content);
        return card;
    }

    private Div createGoogleCalendarIframe(GoogleCalendarToken token) {
        Div container = new Div();
        container.setSizeFull();

        // Usar el email del usuario o el calendar ID si está disponible
        String calendarId = token.getGoogleEmail();
        if (calendarId == null || calendarId.isEmpty()) {
            calendarId = "primary";
        }

        // URL del calendario embebido con parámetros personalizados
        String embedUrl = String.format(
                "https://calendar.google.com/calendar/embed?" +
                        "src=%s&" +
                        "ctz=America/El_Salvador&" +
                        "mode=WEEK&" +
                        "showTitle=0&" +
                        "showNav=1&" +
                        "showDate=1&" +
                        "showPrint=0&" +
                        "showTabs=1&" +
                        "showCalendars=0&" +
                        "showTz=0&" +
                        "wkst=1&" +
                        "bgcolor=%%23ffffff",
                java.net.URLEncoder.encode(calendarId, java.nio.charset.StandardCharsets.UTF_8));

        // Crear iframe HTML
        Html iframe = new Html(String.format(
                "<iframe src='%s' " +
                        "style='border: 0; width: 100%%; height: 100%%; min-height: 600px;' " +
                        "frameborder='0' scrolling='no'></iframe>",
                embedUrl));

        container.add(iframe);

        return container;
    }

    private void loadCalendarEvents() {
        List<CalendarEventDTO> eventDTOs = rentalService.findAllAsCalendarEvents();

        List<CustomCalendar.CalendarEvent> customEvents = eventDTOs.stream()
                .map(dto -> new CustomCalendar.CalendarEvent(
                        dto.getRentalId(),
                        dto.getTitle(),
                        dto.getStart(),
                        dto.getEnd(),
                        dto.getColor(),
                        dto.getRentalId()))
                .collect(Collectors.toList());

        calendar.setEvents(customEvents);
        log.debug("Cargados {} eventos en el calendario", customEvents.size());
    }

    private HorizontalLayout createLegend() {
        HorizontalLayout legend = new HorizontalLayout();
        legend.setSpacing(true);
        legend.getStyle().set("font-size", "var(--lumo-font-size-s)");

        legend.add(
                createLegendItem("Pendiente", "#FFC107"),
                createLegendItem("Activa", "#4CAF50"),
                createLegendItem("Completada", "#2196F3"),
                createLegendItem("Cancelada", "#F44336"));

        return legend;
    }

    private HorizontalLayout createLegendItem(String label, String color) {
        HorizontalLayout item = new HorizontalLayout();
        item.setSpacing(true);
        item.setAlignItems(FlexComponent.Alignment.CENTER);

        Div colorBox = new Div();
        colorBox.getStyle()
                .set("width", "14px")
                .set("height", "14px")
                .set("background-color", color)
                .set("border-radius", "3px");

        Span labelSpan = new Span(label);
        item.add(colorBox, labelSpan);
        return item;
    }

    private void openRentalDetails(Long rentalId) {
        try {
            RentalDTO rental = rentalService.findById(rentalId);
            RentalDetailsDialog dialog = new RentalDetailsDialog(rentalService, rental);
            dialog.open();
        } catch (Exception e) {
            log.error("Error abriendo detalles de renta {}: {}", rentalId, e.getMessage());
        }
    }
}
