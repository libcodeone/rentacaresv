package com.rentacaresv.rental.ui;

import com.rentacaresv.customer.application.CustomerService;
import com.rentacaresv.payment.application.PaymentService;
import com.rentacaresv.payment.ui.RegisterPaymentDialog;
import com.rentacaresv.rental.application.RentalContractPdfGenerator;
import com.rentacaresv.rental.application.RentalDTO;
import com.rentacaresv.rental.application.RentalPhotoService;
import com.rentacaresv.rental.application.RentalService;
import com.rentacaresv.rental.domain.RentalStatus;
import com.rentacaresv.shared.storage.StorageInitializer;
import com.rentacaresv.shared.util.FormatUtils;
import com.rentacaresv.vehicle.application.VehiclePhotoService;
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
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import org.vaadin.lineawesome.LineAwesomeIconUrl;

import java.util.ArrayList;
import java.util.List;

/**
 * Vista de gestión de rentas.
 * Carga todos los datos y usa ordenamiento client-side.
 */
@Route(value = "rentals", layout = MainLayout.class)
@PageTitle("Gestión de Rentas")
@Menu(order = 3, icon = LineAwesomeIconUrl.FILE_CONTRACT_SOLID)
@PermitAll
public class RentalListView extends VerticalLayout {

    private final RentalService rentalService;
    private final VehicleService vehicleService;
    private final VehiclePhotoService vehiclePhotoService;
    private final CustomerService customerService;
    private final PaymentService paymentService;
    private final RentalPhotoService rentalPhotoService;
    private final StorageInitializer storageInitializer;
    private final RentalContractPdfGenerator pdfGenerator;

    private Grid<RentalDTO> grid;
    private TextField searchField;
    private ComboBox<RentalStatus> statusFilter;
    private ListDataProvider<RentalDTO> dataProvider;
    private final List<RentalDTO> allRentals = new ArrayList<>();

    public RentalListView(
            RentalService rentalService,
            VehicleService vehicleService,
            VehiclePhotoService vehiclePhotoService,
            CustomerService customerService,
            PaymentService paymentService,
            RentalPhotoService rentalPhotoService,
            StorageInitializer storageInitializer,
            RentalContractPdfGenerator pdfGenerator) {

        this.rentalService = rentalService;
        this.vehicleService = vehicleService;
        this.vehiclePhotoService = vehiclePhotoService;
        this.customerService = customerService;
        this.paymentService = paymentService;
        this.rentalPhotoService = rentalPhotoService;
        this.storageInitializer = storageInitializer;
        this.pdfGenerator = pdfGenerator;

        setSizeFull();
        setPadding(true);

        add(createHeader(), createToolbar(), createGrid());

        loadData();
    }

    private Component createHeader() {
        H2 title = new H2("Gestión de Rentas");
        title.getStyle()
                .set("margin", "0")
                .set("font-size", "1.5rem");

        long activeCount = rentalService.countActiveRentals();
        long pendingCount = rentalService.countByStatus(RentalStatus.PENDING);
        long overdueCount = rentalService.findOverdueRentals().size();

        Span activeCounter = new Span(String.format("Activas: %d", activeCount));
        activeCounter.getStyle()
                .set("color", "var(--lumo-success-text-color)")
                .set("font-weight", "bold")
                .set("padding", "0.5rem 1rem")
                .set("background", "var(--lumo-success-color-10pct)")
                .set("border-radius", "var(--lumo-border-radius-m)");

        Span pendingCounter = new Span(String.format("Pendientes: %d", pendingCount));
        pendingCounter.getStyle()
                .set("color", "#7c5800")
                .set("font-weight", "bold")
                .set("padding", "0.5rem 1rem")
                .set("background", "var(--lumo-warning-color-10pct)")
                .set("border-radius", "var(--lumo-border-radius-m)");

        Span overdueCounter = new Span(String.format("Atrasadas: %d", overdueCount));
        if (overdueCount > 0) {
            overdueCounter.getStyle()
                    .set("color", "var(--lumo-error-text-color)")
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
        searchField.addValueChangeListener(e -> applyFilters());
        searchField.setWidth("350px");

        statusFilter = new ComboBox<>("Estado");
        statusFilter.setItems(RentalStatus.values());
        statusFilter.setItemLabelGenerator(RentalStatus::getLabel);
        statusFilter.setPlaceholder("Todos");
        statusFilter.setClearButtonVisible(true);
        statusFilter.addValueChangeListener(e -> applyFilters());
        statusFilter.setWidth("150px");

        Button showOverdueButton = new Button("Atrasadas", VaadinIcon.CLOCK.create());
        showOverdueButton.addThemeVariants(ButtonVariant.LUMO_ERROR);
        showOverdueButton.addClickListener(e -> showOverdueRentals());

        Button addButton = new Button("Nueva Renta", VaadinIcon.PLUS.create());
        addButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        addButton.addClickListener(e -> openRentalDialog());

        HorizontalLayout toolbar = new HorizontalLayout(searchField, statusFilter, showOverdueButton, addButton);
        toolbar.setWidthFull();
        toolbar.setJustifyContentMode(FlexComponent.JustifyContentMode.START);
        toolbar.setAlignItems(FlexComponent.Alignment.END);
        toolbar.getStyle().set("padding", "1rem 0");

        return toolbar;
    }

    private Component createGrid() {
        grid = new Grid<>(RentalDTO.class, false);
        grid.setSizeFull();

        // # - Número de contrato (sin prefijo RENT-)
        grid.addColumn(rental -> formatContractNumber(rental.getContractNumber()))
                .setHeader("#")
                .setComparator(RentalDTO::getContractNumber)
                .setAutoWidth(true)
                .setFlexGrow(1)
                .setSortable(true);

        // Vehículo - Placa
        grid.addColumn(RentalDTO::getVehicleLicensePlate)
                .setHeader("Vehículo")
                .setComparator(RentalDTO::getVehicleLicensePlate)
                .setAutoWidth(true)
                .setFlexGrow(1)
                .setSortable(true);

        // Cliente
        grid.addColumn(RentalDTO::getCustomerName)
                .setHeader("Cliente")
                .setComparator(RentalDTO::getCustomerName)
                .setAutoWidth(true)
                .setFlexGrow(2)
                .setSortable(true);

        // Fechas
        grid.addColumn(rental -> FormatUtils.formatDate(rental.getStartDate()))
                .setHeader("Inicio")
                .setComparator(RentalDTO::getStartDate)
                .setAutoWidth(true)
                .setFlexGrow(1)
                .setSortable(true);

        grid.addColumn(rental -> FormatUtils.formatDate(rental.getEndDate()))
                .setHeader("Fin")
                .setComparator(RentalDTO::getEndDate)
                .setAutoWidth(true)
                .setFlexGrow(1)
                .setSortable(true);

        // Días
        grid.addColumn(RentalDTO::getTotalDays)
                .setHeader("Días")
                .setComparator(RentalDTO::getTotalDays)
                .setAutoWidth(true)
                .setFlexGrow(0)
                .setSortable(true);

        // Total
        grid.addColumn(rental -> FormatUtils.formatPrice(rental.getTotalAmount()))
                .setHeader("Total")
                .setComparator(RentalDTO::getTotalAmount)
                .setAutoWidth(true)
                .setFlexGrow(1)
                .setSortable(true);

        // Estado
        grid.addComponentColumn(this::createStatusBadge)
                .setHeader("Estado")
                .setComparator(RentalDTO::getStatus)
                .setAutoWidth(true)
                .setFlexGrow(1)
                .setSortable(true);

        // Acciones (no ordenable)
        grid.addComponentColumn(this::createActionButtons)
                .setHeader("Acciones")
                .setAutoWidth(true)
                .setFlexGrow(1)
                .setSortable(false);

        grid.getStyle()
                .set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("border-radius", "var(--lumo-border-radius-m)");

        return grid;
    }

    /**
     * Carga todos los datos y configura el DataProvider
     */
    private void loadData() {
        allRentals.clear();
        allRentals.addAll(rentalService.findAll());
        
        dataProvider = DataProvider.ofCollection(allRentals);
        grid.setDataProvider(dataProvider);
    }

    /**
     * Aplica filtros de búsqueda y estado sobre los datos cargados
     */
    private void applyFilters() {
        dataProvider.setFilter(rental -> {
            boolean matchesStatus = true;
            boolean matchesSearch = true;
            
            // Filtro por estado
            RentalStatus status = statusFilter.getValue();
            if (status != null) {
                matchesStatus = rental.getStatus().equals(status.name());
            }
            
            // Filtro por búsqueda
            String searchTerm = searchField.getValue();
            if (searchTerm != null && !searchTerm.isBlank()) {
                String lowerSearch = searchTerm.toLowerCase();
                matchesSearch = rental.getContractNumber().toLowerCase().contains(lowerSearch) ||
                               rental.getCustomerName().toLowerCase().contains(lowerSearch) ||
                               rental.getVehicleLicensePlate().toLowerCase().contains(lowerSearch);
            }
            
            return matchesStatus && matchesSearch;
        });
    }

    /**
     * Recarga los datos desde la BD
     */
    private void refreshData() {
        loadData();
        applyFilters();
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
            badge.getElement().setAttribute("title", "ATRASADA - " + rental.getDelayDays() + " días");
        }

        return badge;
    }

    private Component createActionButtons(RentalDTO rental) {
        HorizontalLayout actions = new HorizontalLayout();
        actions.setSpacing(false);

        Button detailsButton = new Button(VaadinIcon.EYE.create());
        detailsButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        detailsButton.getElement().setAttribute("title", "Ver Detalles");
        detailsButton.addClickListener(e -> showDetails(rental));
        actions.add(detailsButton);

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
            deliverButton.getElement().setAttribute("title", "Entregar Vehículo");
            deliverButton.addClickListener(e -> openDeliveryDialog(rental));
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
            returnButton.getElement().setAttribute("title", "Recibir Devolución");
            returnButton.addClickListener(e -> openReturnDialog(rental));
            actions.add(returnButton);
        }

        return actions;
    }

    private void openRentalDialog() {
        RentalFormDialog dialog = new RentalFormDialog(
                rentalService, vehicleService, customerService, vehiclePhotoService);
        dialog.addSaveListener(e -> {
            refreshData();
            showSuccessNotification("Renta creada exitosamente");
        });
        dialog.open();
    }

    private void showDetails(RentalDTO rental) {
        RentalDetailsDialog dialog = new RentalDetailsDialog(rentalService, rentalPhotoService, rental);
        dialog.open();
    }

    private void registerPayment(RentalDTO rental) {
        RegisterPaymentDialog dialog = new RegisterPaymentDialog(paymentService, rental);
        dialog.addSaveListener(e -> {
            refreshData();
            showSuccessNotification("Pago registrado exitosamente");
        });
        dialog.open();
    }

    private void openDeliveryDialog(RentalDTO rental) {
        DeliveryDialog dialog = new DeliveryDialog(
                rentalService, rentalPhotoService, storageInitializer, pdfGenerator, rental);
        dialog.addDeliveryConfirmedListener(e -> {
            refreshData();
            showSuccessNotification("Vehículo entregado exitosamente");
        });
        dialog.open();
    }

    private void openReturnDialog(RentalDTO rental) {
        ReturnDialog dialog = new ReturnDialog(
                rentalService, rentalPhotoService, storageInitializer, rental);
        dialog.addReturnConfirmedListener(e -> {
            refreshData();
            showSuccessNotification("Devolución registrada exitosamente");
        });
        dialog.open();
    }

    private void cancelRental(RentalDTO rental) {
        com.vaadin.flow.component.confirmdialog.ConfirmDialog confirmDialog = 
            new com.vaadin.flow.component.confirmdialog.ConfirmDialog();

        confirmDialog.setHeader("Cancelar Renta");
        confirmDialog.setText(String.format(
            "¿Está seguro de cancelar la renta %s?\n\nCliente: %s\nVehículo: %s\n\nEsta acción no se puede deshacer.",
            rental.getContractNumber(), rental.getCustomerName(), rental.getVehicleLicensePlate()));

        confirmDialog.setCancelable(true);
        confirmDialog.setCancelText("No, mantener");
        confirmDialog.setConfirmText("Sí, cancelar renta");
        confirmDialog.setConfirmButtonTheme("error primary");

        confirmDialog.addConfirmListener(event -> {
            try {
                rentalService.cancelRental(rental.getId());
                refreshData();
                showSuccessNotification("Renta cancelada exitosamente");
            } catch (Exception e) {
                showErrorNotification("Error: " + e.getMessage());
            }
        });

        confirmDialog.open();
    }

    private void showOverdueRentals() {
        List<RentalDTO> overdue = rentalService.findOverdueRentals();

        if (overdue.isEmpty()) {
            showSuccessNotification("¡No hay rentas atrasadas!");
        } else {
            statusFilter.clear();
            searchField.clear();
            allRentals.clear();
            allRentals.addAll(overdue);
            dataProvider.refreshAll();
        }
    }

    /**
     * Formatea el número de contrato removiendo el prefijo "RENT-"
     */
    private String formatContractNumber(String contractNumber) {
        if (contractNumber == null) return "";
        return contractNumber.startsWith("RENT-") 
            ? contractNumber.substring(5) 
            : contractNumber;
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
