package com.rentacaresv.views.home;

import com.rentacaresv.calendar.application.CalendarEventDTO;
import com.rentacaresv.components.calendar.CustomCalendar;
import com.rentacaresv.rental.application.RentalDTO;
import com.rentacaresv.rental.application.RentalService;
import com.rentacaresv.rental.ui.RentalDetailsDialog;
import com.rentacaresv.security.AuthenticatedUser;
import com.rentacaresv.views.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import lombok.extern.slf4j.Slf4j;
import org.vaadin.lineawesome.LineAwesomeIconUrl;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Vista principal de inicio del sistema RentaCar ESV
 * Primera pantalla que ve el usuario después de hacer login
 * Ahora incluye calendario personalizado de rentas
 */
@PageTitle("Inicio")
@Route(value = "", layout = MainLayout.class)
@Menu(order = 0, icon = LineAwesomeIconUrl.HOME_SOLID)
@PermitAll
@Slf4j
public class HomeView extends VerticalLayout {

    private final RentalService rentalService;
    private CustomCalendar calendar;

    public HomeView(AuthenticatedUser authenticatedUser, RentalService rentalService) {
        this.rentalService = rentalService;

        // Actualizar último login
        authenticatedUser.updateLastLogin();

        setSpacing(true);
        setPadding(true);
        setSizeFull();

        // Log de acceso
        authenticatedUser.get().ifPresent(user -> 
            log.info("Usuario {} ({}) accedió al sistema", user.getUsername(), user.getName())
        );

        // Sección de calendario
        VerticalLayout calendarSection = createCalendarSection();

        // Agregar el calendario
        add(calendarSection);
        
        // El calendario debe expandirse para ocupar el espacio disponible
        setFlexGrow(1, calendarSection);
    }

    private VerticalLayout createCalendarSection() {
        // Crear calendario personalizado
        calendar = new CustomCalendar();
        calendar.setSizeFull();

        // Cargar eventos
        loadCalendarEvents();

        // Evento al hacer click en un evento del calendario
        calendar.addEventClickListener(event -> {
            Object data = event.getData();
            if (data instanceof Long) {
                Long rentalId = (Long) data;
                openRentalDetails(rentalId);
            }
        });

        // Botón para refrescar
        Button refreshButton = new Button("Actualizar", VaadinIcon.REFRESH.create());
        refreshButton.addThemeVariants(ButtonVariant.LUMO_SMALL);
        refreshButton.addClickListener(e -> loadCalendarEvents());

        // Leyenda de colores
        HorizontalLayout legend = createLegend();

        // Header del contenedor del calendario
        HorizontalLayout calendarHeader = new HorizontalLayout();
        calendarHeader.setWidthFull();
        calendarHeader.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        calendarHeader.setAlignItems(FlexComponent.Alignment.CENTER);

        H2 calendarTitle = new H2("Calendario de Rentas");
        calendarTitle.getStyle().set("margin", "0");

        calendarHeader.add(calendarTitle, refreshButton);

        // Contenedor del calendario
        VerticalLayout calendarContainer = new VerticalLayout();
        calendarContainer.setSizeFull();
        calendarContainer.setPadding(false);
        calendarContainer.setSpacing(true);
        calendarContainer.getStyle().set("padding-bottom", "var(--lumo-space-l)");
        calendarContainer.add(calendarHeader, legend, calendar);
        
        // El calendario interno debe expandirse
        calendarContainer.setFlexGrow(1, calendar);

        return calendarContainer;
    }

    private void loadCalendarEvents() {
        // Obtener rentas como eventos DTO desde el servicio
        List<CalendarEventDTO> eventDTOs = rentalService.findAllAsCalendarEvents();

        // Mapear a eventos del CustomCalendar
        List<CustomCalendar.CalendarEvent> customEvents = eventDTOs.stream()
                .map(dto -> new CustomCalendar.CalendarEvent(
                        dto.getRentalId(),
                        dto.getTitle(),
                        dto.getStart(),
                        dto.getEnd(),
                        dto.getColor(),
                        dto.getRentalId() // Pasamos el rentalId como data
                ))
                .collect(Collectors.toList());

        calendar.setEvents(customEvents);

        log.info("Cargados {} eventos en el calendario personalizado", customEvents.size());
    }

    private HorizontalLayout createLegend() {
        HorizontalLayout legend = new HorizontalLayout();
        legend.setSpacing(true);
        legend.getStyle()
                .set("font-size", "var(--lumo-font-size-s)")
                .set("margin", "var(--lumo-space-xs) 0");

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
                .set("width", "16px")
                .set("height", "16px")
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
            log.error("Error abriendo detalles de renta {}: {}", rentalId, e.getMessage(), e);
        }
    }
}
