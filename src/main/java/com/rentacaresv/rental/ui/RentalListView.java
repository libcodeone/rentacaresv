package com.rentacaresv.rental.ui;

import com.rentacaresv.customer.application.CustomerService;
import com.rentacaresv.payment.application.PaymentService;
import com.rentacaresv.payment.ui.RegisterPaymentDialog;
import com.rentacaresv.rental.application.RentalDTO;
import com.rentacaresv.rental.application.RentalService;
import com.rentacaresv.rental.domain.RentalStatus;
import com.rentacaresv.shared.util.FormatUtils;
import com.rentacaresv.vehicle.application.VehicleService;
import com.rentacaresv.views.MainLayout;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import org.vaadin.lineawesome.LineAwesomeIconUrl;

import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Vista de gestión de rentas
 */
@Route(value = "rentals", layout = MainLayout.class)
@PageTitle("Gestión de Rentas")
@Menu(order = 3, icon = LineAwesomeIconUrl.FILE_CONTRACT_SOLID)
@PermitAll
public class RentalListView extends VerticalLayout {

    private final RentalService rentalService;
    private final VehicleService vehicleService;
    private final CustomerService customerService;
    private final PaymentService paymentService;
    
    private Grid<RentalDTO> grid;
    private TextField searchField;
    private ComboBox<RentalStatus> statusFilter;
    private Button addButton;
    private Button showOverdueButton;

    public RentalListView(
            RentalService rentalService,
            VehicleService vehicleService,
            CustomerService customerService,
            PaymentService paymentService) {
        
        this.rentalService = rentalService;
        this.vehicleService = vehicleService;
        this.customerService = customerService;
        this.paymentService = paymentService;
        
        setSizeFull();
        setPadding(true);
        
        add(
            createHeader(),
            createToolbar(),
            createGrid()
        );
        
        updateGrid();
    }

    private Component createHeader() {
        H2 title = new H2("Gestión de Rentas");
        title.getStyle()
            .set("margin", "0")
            .set("font-size", "1.5rem");
        
        // Contadores
        long activeCount = rentalService.countActiveRentals();
        long pendingCount = rentalService.countByStatus(RentalStatus.PENDING);
        long overdueCount = rentalService.findOverdueRentals().size();
        
        Span activeCounter = new Span(String.format("Activas: %d", activeCount));
        activeCounter.getStyle()
            .set("color", "var(--lumo-success-color)")
            .set("font-weight", "bold")
            .set("padding", "0.5rem 1rem")
            .set("background", "var(--lumo-success-color-10pct)")
            .set("border-radius", "var(--lumo-border-radius-m)");
        
        Span pendingCounter = new Span(String.format("Pendientes: %d", pendingCount));
        pendingCounter.getStyle()
            .set("color", "var(--lumo-warning-color)")
            .set("font-weight", "bold")
            .set("padding", "0.5rem 1rem")
            .set("background", "var(--lumo-warning-color-10pct)")
            .set("border-radius", "var(--lumo-border-radius-m)");
        
        Span overdueCounter = new Span(String.format("Atrasadas: %d", overdueCount));
        if (overdueCount > 0) {
            overdueCounter.getStyle()
                .set("color", "var(--lumo-error-color)")
                .set("font-weight", "bold")
                .set("padding", "0.5rem 1rem")
                .set("background", "var(--lumo-error-color-10pct)")
                .set("border-radius", "var(--lumo-border-radius-m)");
        } else {
            overdueCounter.getStyle()
                .set("color", "var(--lumo-contrast-60pct)")
                .set("padding", "0.5rem 1rem")
                .set("border-radius", "var(--lumo-border-radius-m)");
        }
        
        HorizontalLayout counters = new HorizontalLayout(activeCounter, pendingCounter, overdueCounter);
        counters.setSpacing(true);
        
        HorizontalLayout header = new HorizontalLayout(title, counters);
        header.setWidthFull();
        header.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.getStyle()
            .set("padding-bottom", "1rem")
            .set("border-bottom", "1px solid var(--lumo-contrast-10pct)");
        
        return header;
    }

    private Component createToolbar() {
        searchField = new TextField();
        searchField.setPlaceholder("Buscar por contrato, cliente o vehículo...");
        searchField.setPrefixComponent(VaadinIcon.SEARCH.create());
        searchField.setValueChangeMode(ValueChangeMode.LAZY);
        searchField.addValueChangeListener(e -> updateGrid());
        searchField.setWidth("350px");

        statusFilter = new ComboBox<>("Estado");
        statusFilter.setItems(RentalStatus.values());
        statusFilter.setItemLabelGenerator(RentalStatus::getLabel);
        statusFilter.setPlaceholder("Todos");
        statusFilter.setClearButtonVisible(true);
        statusFilter.addValueChangeListener(e -> updateGrid());
        statusFilter.setWidth("150px");

        showOverdueButton = new Button("Atrasadas", VaadinIcon.CLOCK.create());
        showOverdueButton.addThemeVariants(ButtonVariant.LUMO_ERROR);
        showOverdueButton.addClickListener(e -> showOverdueRentals());

        addButton = new Button("Nueva Renta", VaadinIcon.PLUS.create());
        addButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        addButton.addClickListener(e -> openRentalDialog());

        HorizontalLayout toolbar = new HorizontalLayout(
            searchField, statusFilter, showOverdueButton, addButton
        );
        toolbar.setWidthFull();
        toolbar.setJustifyContentMode(FlexComponent.JustifyContentMode.START);
        toolbar.setAlignItems(FlexComponent.Alignment.END);
        toolbar.getStyle().set("padding", "1rem 0");

        return toolbar;
    }

    private Component createGrid() {
        grid = new Grid<>(RentalDTO.class, false);
        grid.setSizeFull();
        
        grid.addColumn(RentalDTO::getContractNumber)
            .setHeader("Contrato")
            .setWidth("150px")
            .setFlexGrow(0);
        
        grid.addColumn(RentalDTO::getVehicleLicensePlate)
            .setHeader("Vehículo")
            .setWidth("120px")
            .setFlexGrow(0);
        
        grid.addColumn(RentalDTO::getCustomerName)
            .setHeader("Cliente")
            .setAutoWidth(true)
            .setFlexGrow(1);
        
        grid.addColumn(rental -> FormatUtils.formatDate(rental.getStartDate()))
            .setHeader("Inicio")
            .setWidth("110px")
            .setFlexGrow(0);
        
        grid.addColumn(rental -> FormatUtils.formatDate(rental.getEndDate()))
            .setHeader("Fin")
            .setWidth("110px")
            .setFlexGrow(0);
        
        grid.addColumn(RentalDTO::getTotalDays)
            .setHeader("Días")
            .setWidth("70px")
            .setFlexGrow(0);
        
        grid.addColumn(rental -> FormatUtils.formatPrice(rental.getTotalAmount()))
            .setHeader("Total")
            .setWidth("120px")
            .setFlexGrow(0);
        
        grid.addComponentColumn(this::createStatusBadge)
            .setHeader("Estado")
            .setWidth("120px")
            .setFlexGrow(0);
        
        grid.addComponentColumn(this::createActionButtons)
            .setHeader("Acciones")
            .setWidth("200px")
            .setFlexGrow(0);
        
        grid.addThemeVariants();
        grid.getStyle()
            .set("border", "1px solid var(--lumo-contrast-10pct)")
            .set("border-radius", "var(--lumo-border-radius-m)");

        return grid;
    }

    private Component createStatusBadge(RentalDTO rental) {
        Span badge = new Span(rental.getStatusLabel());
        badge.getElement().getThemeList().add("badge");
        
        String color = switch (rental.getStatus()) {
            case "PENDING" -> "contrast";
            case "ACTIVE" -> "success";
            case "COMPLETED" -> "primary";
            case "CANCELLED" -> "error";
            default -> "contrast";
        };
        
        badge.getElement().getThemeList().add(color);
        
        if (rental.getIsDelayed()) {
            badge.getElement().setAttribute("title", 
                "ATRASADA - " + rental.getDelayDays() + " días");
        }
        
        return badge;
    }

    private Component createActionButtons(RentalDTO rental) {
        HorizontalLayout actions = new HorizontalLayout();
        actions.setSpacing(false);

        // Botón de Ver Detalles (SIEMPRE visible)
        Button detailsButton = new Button(VaadinIcon.EYE.create());
        detailsButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        detailsButton.getElement().setAttribute("title", "Ver Detalles");
        detailsButton.addClickListener(e -> showDetails(rental));
        actions.add(detailsButton);

        // Botón de Registrar Pago (si tiene saldo pendiente)
        if (rental.getBalance() != null && rental.getBalance().compareTo(java.math.BigDecimal.ZERO) > 0) {
            Button paymentButton = new Button(VaadinIcon.MONEY.create());
            paymentButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_SUCCESS);
            paymentButton.getElement().setAttribute("title", "Registrar Pago");
            paymentButton.addClickListener(e -> registerPayment(rental));
            actions.add(paymentButton);
        }

        if ("PENDING".equals(rental.getStatus())) {
            Button deliverButton = new Button(VaadinIcon.CAR.create());
            deliverButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_SUCCESS);
            deliverButton.getElement().setAttribute("title", "Entregar");
            deliverButton.addClickListener(e -> deliverVehicle(rental));
            actions.add(deliverButton);
            
            Button cancelButton = new Button(VaadinIcon.CLOSE.create());
            cancelButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR);
            cancelButton.getElement().setAttribute("title", "Cancelar");
            cancelButton.addClickListener(e -> cancelRental(rental));
            actions.add(cancelButton);
        }

        if ("ACTIVE".equals(rental.getStatus())) {
            Button returnButton = new Button(VaadinIcon.SIGN_IN.create());
            returnButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_PRIMARY);
            returnButton.getElement().setAttribute("title", "Devolver");
            returnButton.addClickListener(e -> returnVehicle(rental));
            actions.add(returnButton);
        }

        return actions;
    }

    private void openRentalDialog() {
        RentalFormDialog dialog = new RentalFormDialog(rentalService, vehicleService, customerService);
        dialog.addSaveListener(e -> {
            updateGrid();
            showSuccessNotification("Renta creada exitosamente");
        });
        dialog.open();
    }

    private void showDetails(RentalDTO rental) {
        RentalDetailsDialog dialog = new RentalDetailsDialog(rentalService, rental);
        dialog.open();
    }

    private void registerPayment(RentalDTO rental) {
        RegisterPaymentDialog dialog = new RegisterPaymentDialog(paymentService, rental);
        dialog.addSaveListener(e -> {
            updateGrid();
            showSuccessNotification("Pago registrado exitosamente");
        });
        dialog.open();
    }

    private void deliverVehicle(RentalDTO rental) {
        try {
            rentalService.deliverVehicle(rental.getId());
            updateGrid();
            showSuccessNotification("Vehículo entregado");
        } catch (Exception e) {
            showErrorNotification("Error: " + e.getMessage());
        }
    }

    private void returnVehicle(RentalDTO rental) {
        try {
            rentalService.returnVehicle(rental.getId());
            updateGrid();
            showSuccessNotification("Vehículo devuelto");
        } catch (Exception e) {
            showErrorNotification("Error: " + e.getMessage());
        }
    }

    private void cancelRental(RentalDTO rental) {
        try {
            rentalService.cancelRental(rental.getId());
            updateGrid();
            showSuccessNotification("Renta cancelada");
        } catch (Exception e) {
            showErrorNotification("Error: " + e.getMessage());
        }
    }

    private void showOverdueRentals() {
        List<RentalDTO> overdue = rentalService.findOverdueRentals();
        
        if (overdue.isEmpty()) {
            showSuccessNotification("¡No hay rentas atrasadas!");
        } else {
            statusFilter.clear();
            searchField.clear();
            grid.setItems(overdue);
        }
    }

    private void updateGrid() {
        List<RentalDTO> rentals;
        
        RentalStatus status = statusFilter.getValue();
        String searchTerm = searchField.getValue();
        
        if (status != null) {
            rentals = rentalService.findByStatus(status);
        } else {
            rentals = rentalService.findAll();
        }
        
        if (searchTerm != null && !searchTerm.isBlank()) {
            String lowerSearch = searchTerm.toLowerCase();
            rentals = rentals.stream()
                .filter(r -> 
                    r.getContractNumber().toLowerCase().contains(lowerSearch) ||
                    r.getCustomerName().toLowerCase().contains(lowerSearch) ||
                    r.getVehicleLicensePlate().toLowerCase().contains(lowerSearch)
                )
                .toList();
        }
        
        grid.setItems(rentals);
    }

    private void showSuccessNotification(String message) {
        Notification notification = Notification.show(message, 3000, Notification.Position.TOP_CENTER);
        notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }

    private void showErrorNotification(String message) {
        Notification notification = Notification.show(message, 5000, Notification.Position.TOP_CENTER);
        notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
    }
}
