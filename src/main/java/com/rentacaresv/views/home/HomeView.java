package com.rentacaresv.views.home;

import com.rentacaresv.rental.application.RentalService;
import com.rentacaresv.rental.domain.RentalStatus;
import com.rentacaresv.security.AuthenticatedUser;
import com.rentacaresv.views.MainLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import jakarta.annotation.security.PermitAll;
import lombok.extern.slf4j.Slf4j;
import org.vaadin.lineawesome.LineAwesomeIconUrl;

import java.time.LocalDate;

/**
 * Vista principal de inicio del sistema RentaCar ESV
 * Muestra un dashboard con estadísticas y accesos rápidos
 */
@PageTitle("Inicio")
@Route(value = "", layout = MainLayout.class)
@Menu(order = 0, icon = LineAwesomeIconUrl.HOME_SOLID)
@PermitAll
@Slf4j
public class HomeView extends VerticalLayout {

    private final RentalService rentalService;

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

        // Saludo
        authenticatedUser.get().ifPresent(user -> {
            H2 greeting = new H2("¡Bienvenido, " + user.getName() + "!");
            greeting.getStyle().set("margin-top", "0");
            add(greeting);
        });

        // Dashboard de estadísticas
        HorizontalLayout statsRow = createStatsRow();
        add(statsRow);

        // Accesos rápidos
        H3 quickAccessTitle = new H3("Accesos Rápidos");
        quickAccessTitle.getStyle().set("margin-bottom", "0");
        add(quickAccessTitle);

        HorizontalLayout quickAccess = createQuickAccessRow();
        add(quickAccess);
    }

    private HorizontalLayout createStatsRow() {
        HorizontalLayout row = new HorizontalLayout();
        row.setWidthFull();
        row.setSpacing(true);

        // Rentas activas hoy
        long activeToday = rentalService.countByStatus(RentalStatus.ACTIVE);
        row.add(createStatCard("Rentas Activas", String.valueOf(activeToday), 
                VaadinIcon.CAR, "#4CAF50"));

        // Rentas pendientes
        long pending = rentalService.countByStatus(RentalStatus.PENDING);
        row.add(createStatCard("Pendientes", String.valueOf(pending), 
                VaadinIcon.CLOCK, "#FFC107"));

        // Entregas hoy
        long deliveriesToday = rentalService.countEndingOn(LocalDate.now());
        row.add(createStatCard("Entregas Hoy", String.valueOf(deliveriesToday), 
                VaadinIcon.CALENDAR_CLOCK, "#2196F3"));

        // Completadas este mes
        long completedThisMonth = rentalService.countCompletedThisMonth();
        row.add(createStatCard("Completadas (Mes)", String.valueOf(completedThisMonth), 
                VaadinIcon.CHECK_CIRCLE, "#9C27B0"));

        return row;
    }

    private Div createStatCard(String title, String value, VaadinIcon icon, String color) {
        Div card = new Div();
        card.getStyle()
                .set("background", "var(--lumo-base-color)")
                .set("border-radius", "var(--lumo-border-radius-l)")
                .set("padding", "var(--lumo-space-l)")
                .set("box-shadow", "var(--lumo-box-shadow-s)")
                .set("flex", "1")
                .set("min-width", "180px")
                .set("text-align", "center");

        // Icono
        var iconElement = icon.create();
        iconElement.setSize("40px");
        iconElement.getStyle().set("color", color);

        // Valor
        Span valueSpan = new Span(value);
        valueSpan.getStyle()
                .set("display", "block")
                .set("font-size", "var(--lumo-font-size-xxxl)")
                .set("font-weight", "bold")
                .set("color", color)
                .set("margin", "var(--lumo-space-s) 0");

        // Título
        Span titleSpan = new Span(title);
        titleSpan.getStyle()
                .set("color", "var(--lumo-secondary-text-color)")
                .set("font-size", "var(--lumo-font-size-s)");

        card.add(iconElement, valueSpan, titleSpan);
        return card;
    }

    private HorizontalLayout createQuickAccessRow() {
        HorizontalLayout row = new HorizontalLayout();
        row.setWidthFull();
        row.setSpacing(true);
        row.setWrap(true);

        row.add(createQuickAccessCard("Calendario", "Ver calendario de rentas", 
                VaadinIcon.CALENDAR, "calendar", "#2196F3"));
        row.add(createQuickAccessCard("Nueva Renta", "Crear una nueva renta", 
                VaadinIcon.PLUS_CIRCLE, "rentals", "#4CAF50"));
        row.add(createQuickAccessCard("Clientes", "Gestionar clientes", 
                VaadinIcon.USERS, "customers", "#FF9800"));
        row.add(createQuickAccessCard("Vehículos", "Gestionar flota", 
                VaadinIcon.CAR, "vehicles", "#9C27B0"));

        return row;
    }

    private Div createQuickAccessCard(String title, String description, VaadinIcon icon, 
                                       String route, String color) {
        Div card = new Div();
        card.getStyle()
                .set("background", "var(--lumo-base-color)")
                .set("border-radius", "var(--lumo-border-radius-l)")
                .set("padding", "var(--lumo-space-l)")
                .set("box-shadow", "var(--lumo-box-shadow-s)")
                .set("cursor", "pointer")
                .set("transition", "transform 0.2s, box-shadow 0.2s")
                .set("min-width", "200px")
                .set("flex", "1");

        card.getElement().addEventListener("mouseover", e -> {
            card.getStyle().set("transform", "translateY(-2px)");
            card.getStyle().set("box-shadow", "var(--lumo-box-shadow-m)");
        });
        card.getElement().addEventListener("mouseout", e -> {
            card.getStyle().set("transform", "translateY(0)");
            card.getStyle().set("box-shadow", "var(--lumo-box-shadow-s)");
        });

        VerticalLayout content = new VerticalLayout();
        content.setPadding(false);
        content.setSpacing(false);

        var iconElement = icon.create();
        iconElement.setSize("32px");
        iconElement.getStyle().set("color", color);

        Span titleSpan = new Span(title);
        titleSpan.getStyle()
                .set("font-weight", "600")
                .set("font-size", "var(--lumo-font-size-l)")
                .set("margin-top", "var(--lumo-space-s)");

        Span descSpan = new Span(description);
        descSpan.getStyle()
                .set("color", "var(--lumo-secondary-text-color)")
                .set("font-size", "var(--lumo-font-size-s)");

        content.add(iconElement, titleSpan, descSpan);
        card.add(content);

        // Hacer clickeable
        card.addClickListener(e -> card.getUI().ifPresent(ui -> ui.navigate(route)));

        return card;
    }
}
