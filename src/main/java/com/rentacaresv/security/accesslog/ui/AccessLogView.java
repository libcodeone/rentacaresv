package com.rentacaresv.security.accesslog.ui;

import com.rentacaresv.security.accesslog.application.AccessLogService;
import com.rentacaresv.security.accesslog.domain.AccessEventType;
import com.rentacaresv.security.accesslog.domain.AccessLog;
import com.rentacaresv.views.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.vaadin.lineawesome.LineAwesomeIconUrl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Vista para visualizar el registro de accesos al sistema
 */
@Route(value = "settings/access-log", layout = MainLayout.class)
@PageTitle("Registro de Accesos | RentaCarESV")
@Menu(order = 99.2, icon = LineAwesomeIconUrl.CLIPBOARD_LIST_SOLID)
@RolesAllowed("ADMIN")
@Slf4j
public class AccessLogView extends VerticalLayout {

    private final AccessLogService accessLogService;
    private final Grid<AccessLog> grid;

    // Filtros
    private TextField usernameFilter;
    private ComboBox<AccessEventType> eventTypeFilter;
    private DatePicker startDateFilter;
    private DatePicker endDateFilter;

    // Paginación
    private int currentPage = 0;
    private final int pageSize = 50;
    private Span pageInfo;
    private Button prevButton;
    private Button nextButton;
    private long totalElements;

    private static final DateTimeFormatter DATE_TIME_FORMATTER = 
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    public AccessLogView(AccessLogService accessLogService) {
        this.accessLogService = accessLogService;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        // Header con estadísticas
        add(createHeader());

        // Filtros
        add(createFilters());

        // Grid
        grid = createGrid();
        add(grid);

        // Paginación
        add(createPagination());

        // Cargar datos
        refreshGrid();
    }

    private HorizontalLayout createHeader() {
        H3 title = new H3("Registro de Accesos al Sistema");
        title.getStyle().set("margin", "0");

        // Estadísticas rápidas
        List<String> activeUsers = accessLogService.getActiveUsers(24);
        Span activeUsersLabel = new Span("Usuarios activos (24h): " + activeUsers.size());
        activeUsersLabel.getStyle()
                .set("background-color", "var(--lumo-success-color-10pct)")
                .set("color", "var(--lumo-success-text-color)")
                .set("padding", "var(--lumo-space-xs) var(--lumo-space-m)")
                .set("border-radius", "var(--lumo-border-radius-m)");

        Button refreshBtn = new Button(VaadinIcon.REFRESH.create());
        refreshBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        refreshBtn.setTooltipText("Actualizar");
        refreshBtn.addClickListener(e -> refreshGrid());

        HorizontalLayout header = new HorizontalLayout(title, activeUsersLabel, refreshBtn);
        header.setWidthFull();
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.expand(title);

        return header;
    }

    private HorizontalLayout createFilters() {
        usernameFilter = new TextField();
        usernameFilter.setPlaceholder("Buscar por usuario...");
        usernameFilter.setPrefixComponent(VaadinIcon.SEARCH.create());
        usernameFilter.setClearButtonVisible(true);
        usernameFilter.setValueChangeMode(ValueChangeMode.LAZY);
        usernameFilter.setValueChangeTimeout(500);
        usernameFilter.addValueChangeListener(e -> {
            currentPage = 0;
            refreshGrid();
        });

        eventTypeFilter = new ComboBox<>("Tipo de Evento");
        eventTypeFilter.setItems(AccessEventType.values());
        eventTypeFilter.setItemLabelGenerator(AccessEventType::getDescription);
        eventTypeFilter.setClearButtonVisible(true);
        eventTypeFilter.addValueChangeListener(e -> {
            currentPage = 0;
            refreshGrid();
        });

        startDateFilter = new DatePicker("Desde");
        startDateFilter.setLocale(new Locale("es", "SV"));
        startDateFilter.setClearButtonVisible(true);
        startDateFilter.addValueChangeListener(e -> {
            currentPage = 0;
            refreshGrid();
        });

        endDateFilter = new DatePicker("Hasta");
        endDateFilter.setLocale(new Locale("es", "SV"));
        endDateFilter.setClearButtonVisible(true);
        endDateFilter.addValueChangeListener(e -> {
            currentPage = 0;
            refreshGrid();
        });

        Button clearFilters = new Button("Limpiar", VaadinIcon.CLOSE_SMALL.create());
        clearFilters.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        clearFilters.addClickListener(e -> {
            usernameFilter.clear();
            eventTypeFilter.clear();
            startDateFilter.clear();
            endDateFilter.clear();
            currentPage = 0;
            refreshGrid();
        });

        HorizontalLayout filters = new HorizontalLayout(
                usernameFilter, eventTypeFilter, startDateFilter, endDateFilter, clearFilters
        );
        filters.setAlignItems(FlexComponent.Alignment.END);
        filters.setWidthFull();
        filters.setFlexGrow(1, usernameFilter);

        return filters;
    }

    private Grid<AccessLog> createGrid() {
        Grid<AccessLog> grid = new Grid<>(AccessLog.class, false);
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES, GridVariant.LUMO_COMPACT);
        grid.setSizeFull();

        // Columna de fecha/hora
        grid.addColumn(log -> log.getTimestamp().format(DATE_TIME_FORMATTER))
                .setHeader("Fecha/Hora")
                .setAutoWidth(true)
                .setSortable(true);

        // Columna de usuario
        grid.addColumn(AccessLog::getUsername)
                .setHeader("Usuario")
                .setAutoWidth(true)
                .setSortable(true);

        // Columna de tipo de evento con badge de color
        grid.addColumn(new ComponentRenderer<>(log -> {
            Span badge = new Span(log.getEventType().getDescription());
            String bgColor;
            String textColor = "white";

            switch (log.getEventType()) {
                case LOGIN_SUCCESS:
                    bgColor = "var(--lumo-success-color)";
                    break;
                case LOGIN_FAILED:
                    bgColor = "var(--lumo-error-color)";
                    break;
                case LOGOUT:
                    bgColor = "var(--lumo-contrast-50pct)";
                    break;
                case SESSION_EXPIRED:
                    bgColor = "var(--lumo-warning-color)";
                    textColor = "var(--lumo-contrast)";
                    break;
                case ACCOUNT_LOCKED:
                    bgColor = "var(--lumo-error-color)";
                    break;
                default:
                    bgColor = "var(--lumo-primary-color)";
            }

            badge.getStyle()
                    .set("background-color", bgColor)
                    .set("color", textColor)
                    .set("padding", "2px 8px")
                    .set("border-radius", "4px")
                    .set("font-size", "12px")
                    .set("font-weight", "500");

            return badge;
        })).setHeader("Evento").setAutoWidth(true);

        // Columna de IP
        grid.addColumn(AccessLog::getIpAddress)
                .setHeader("Dirección IP")
                .setAutoWidth(true);

        // Columna de resultado
        grid.addColumn(new ComponentRenderer<>(log -> {
            if (log.isSuccessful()) {
                Span success = new Span("✓");
                success.getStyle().set("color", "var(--lumo-success-color)");
                return success;
            } else {
                Span failed = new Span("✗");
                failed.getStyle().set("color", "var(--lumo-error-color)");
                return failed;
            }
        })).setHeader("OK").setAutoWidth(true);

        // Columna de detalles
        grid.addColumn(AccessLog::getDetails)
                .setHeader("Detalles")
                .setFlexGrow(1);

        // Columna de User Agent (abreviado)
        grid.addColumn(log -> {
            String ua = log.getUserAgent();
            if (ua == null || ua.isEmpty()) return "";
            // Extraer navegador simplificado
            if (ua.contains("Chrome")) return "Chrome";
            if (ua.contains("Firefox")) return "Firefox";
            if (ua.contains("Safari")) return "Safari";
            if (ua.contains("Edge")) return "Edge";
            if (ua.length() > 30) return ua.substring(0, 30) + "...";
            return ua;
        }).setHeader("Navegador").setAutoWidth(true);

        return grid;
    }

    private HorizontalLayout createPagination() {
        prevButton = new Button(VaadinIcon.ANGLE_LEFT.create());
        prevButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        prevButton.addClickListener(e -> {
            if (currentPage > 0) {
                currentPage--;
                refreshGrid();
            }
        });

        nextButton = new Button(VaadinIcon.ANGLE_RIGHT.create());
        nextButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        nextButton.addClickListener(e -> {
            int totalPages = (int) Math.ceil((double) totalElements / pageSize);
            if (currentPage < totalPages - 1) {
                currentPage++;
                refreshGrid();
            }
        });

        pageInfo = new Span();

        HorizontalLayout pagination = new HorizontalLayout(prevButton, pageInfo, nextButton);
        pagination.setAlignItems(FlexComponent.Alignment.CENTER);
        pagination.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        pagination.setWidthFull();

        return pagination;
    }

    private void refreshGrid() {
        // Obtener valores de filtros
        String username = usernameFilter.getValue();
        if (username != null && username.isEmpty()) username = null;

        AccessEventType eventType = eventTypeFilter.getValue();

        LocalDateTime startDate = null;
        LocalDateTime endDate = null;

        if (startDateFilter.getValue() != null) {
            startDate = startDateFilter.getValue().atStartOfDay();
        }

        if (endDateFilter.getValue() != null) {
            endDate = endDateFilter.getValue().atTime(LocalTime.MAX);
        }

        // Consultar con filtros y paginación
        PageRequest pageRequest = PageRequest.of(currentPage, pageSize, Sort.by(Sort.Direction.DESC, "timestamp"));
        Page<AccessLog> page = accessLogService.searchLogs(username, eventType, startDate, endDate, pageRequest);

        grid.setItems(page.getContent());
        totalElements = page.getTotalElements();

        // Actualizar paginación
        int totalPages = page.getTotalPages();
        prevButton.setEnabled(currentPage > 0);
        nextButton.setEnabled(currentPage < totalPages - 1);
        pageInfo.setText(String.format("Página %d de %d (%d registros)",
                currentPage + 1,
                Math.max(totalPages, 1),
                totalElements));
    }
}
