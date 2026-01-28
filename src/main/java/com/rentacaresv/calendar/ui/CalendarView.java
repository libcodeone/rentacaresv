package com.rentacaresv.calendar.ui;

import com.rentacaresv.calendar.application.CalendarEventDTO;
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
import jakarta.annotation.security.PermitAll;
import lombok.extern.slf4j.Slf4j;
import org.vaadin.lineawesome.LineAwesomeIconUrl;

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
@PermitAll
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

        // Lado derecho: Botones
        HorizontalLayout rightSide = new HorizontalLayout();
        rightSide.setSpacing(true);

        linkGoogleButton = new Button("Vincular Google Calendar", VaadinIcon.LINK.create());
        linkGoogleButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        linkGoogleButton.addClickListener(e -> linkGoogleCalendar());

        unlinkGoogleButton = new Button("Desvincular", VaadinIcon.UNLINK.create());
        unlinkGoogleButton.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);
        unlinkGoogleButton.addClickListener(e -> confirmUnlinkGoogle());

        rightSide.add(linkGoogleButton, unlinkGoogleButton);

        content.add(leftSide, rightSide);
        panel.add(content);

        // Actualizar estado
        updateGoogleCalendarStatus();

        return panel;
    }

    private void updateGoogleCalendarStatus() {
        if (!googleCalendarService.isConfigured()) {
            googleCalendarStatus.setText("No configurado (contacte al administrador)");
            googleCalendarStatus.getStyle().set("color", "var(--lumo-secondary-text-color)");
            linkGoogleButton.setEnabled(false);
            unlinkGoogleButton.setVisible(false);
            return;
        }

        Optional<GoogleCalendarToken> tokenOpt = googleCalendarService.getUserToken(currentUser.getId());

        if (tokenOpt.isPresent()) {
            GoogleCalendarToken token = tokenOpt.get();
            String email = token.getGoogleEmail() != null ? token.getGoogleEmail() : "Cuenta vinculada";

            if (token.getSyncEnabled()) {
                googleCalendarStatus.setText("Vinculado: " + email);
                googleCalendarStatus.getStyle().set("color", "var(--lumo-success-color)");
            } else {
                googleCalendarStatus.setText("Vinculado (sincronización pausada): " + email);
                googleCalendarStatus.getStyle().set("color", "var(--lumo-warning-color)");
            }

            linkGoogleButton.setVisible(false);
            unlinkGoogleButton.setVisible(true);
        } else {
            googleCalendarStatus.setText("No vinculado - Las rentas se sincronizarán automáticamente");
            googleCalendarStatus.getStyle().set("color", "var(--lumo-secondary-text-color)");
            linkGoogleButton.setVisible(true);
            unlinkGoogleButton.setVisible(false);
        }

        // Refrescar la vista después de vincular/desvincular
        if (tabSheet != null) {
            buildView();
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

        // Verificar si el usuario tiene Google Calendar vinculado
        Optional<GoogleCalendarToken> tokenOpt = googleCalendarService.getUserToken(currentUser.getId());

        if (tokenOpt.isEmpty()) {
            // Mostrar mensaje para vincular Google Calendar
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

            H3 title = new H3("Vincula tu Google Calendar");
            title.getStyle().set("margin", "var(--lumo-space-s) 0");

            Paragraph description = new Paragraph(
                    "Para ver tu Google Calendar aquí, primero debes vincular tu cuenta de Google.");
            description.getStyle().set("color", "var(--lumo-secondary-text-color)");

            Button linkButton = new Button("Vincular Google Calendar", VaadinIcon.LINK.create());
            linkButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_LARGE);
            linkButton.addClickListener(e -> linkGoogleCalendar());

            messageLayout.add(icon, title, description, linkButton);
            container.add(messageLayout);
        } else {
            // Crear iframe con Google Calendar embebido
            googleCalendarIframe = createGoogleCalendarIframe(tokenOpt.get());
            container.add(googleCalendarIframe);
            container.setFlexGrow(1, googleCalendarIframe);
        }

        return container;
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
