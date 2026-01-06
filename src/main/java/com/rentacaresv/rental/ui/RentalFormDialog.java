package com.rentacaresv.rental.ui;

import com.rentacaresv.customer.application.CustomerDTO;
import com.rentacaresv.customer.application.CustomerService;
import com.rentacaresv.rental.application.CreateRentalCommand;
import com.rentacaresv.rental.application.RentalService;
import com.rentacaresv.shared.util.FormatUtils;
import com.rentacaresv.vehicle.application.VehicleDTO;
import com.rentacaresv.vehicle.application.VehiclePhotoService;
import com.rentacaresv.vehicle.application.VehicleService;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.shared.Registration;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;

/**
 * Diálogo para crear nuevas rentas
 * Con preview del vehículo seleccionado
 */
public class RentalFormDialog extends Dialog {

    private final RentalService rentalService;
    private final VehicleService vehicleService;
    private final CustomerService customerService;
    private final VehiclePhotoService vehiclePhotoService;
    private final BeanValidationBinder<CreateRentalCommand> binder;
    private CreateRentalCommand command;
    
    // Lista de vehículos disponibles
    private List<VehicleDTO> availableVehicles;
    private List<CustomerDTO> activeCustomers;
    
    // Campos básicos del formulario
    private ComboBox<VehicleDTO> vehicleCombo;
    private ComboBox<CustomerDTO> customerCombo;
    private DatePicker startDate;
    private DatePicker endDate;
    private TextArea notes;
    
    // Panel de preview del vehículo
    private VerticalLayout vehiclePreviewPanel;
    
    // Campos de información de viaje (opcionales)
    private TextField flightNumberField;
    private TextArea itineraryField;
    private TextField accommodationField;
    private TextField contactPhoneField;
    
    // Información calculada
    private Span daysLabel;
    private Span priceLabel;
    
    // Botones
    private Button saveButton;
    private Button cancelButton;

    public RentalFormDialog(
            RentalService rentalService,
            VehicleService vehicleService,
            CustomerService customerService) {
        this(rentalService, vehicleService, customerService, null);
    }

    public RentalFormDialog(
            RentalService rentalService,
            VehicleService vehicleService,
            CustomerService customerService,
            VehiclePhotoService vehiclePhotoService) {
        
        this.rentalService = rentalService;
        this.vehicleService = vehicleService;
        this.customerService = customerService;
        this.vehiclePhotoService = vehiclePhotoService;
        this.command = new CreateRentalCommand();
        this.binder = new BeanValidationBinder<>(CreateRentalCommand.class);
        
        configureDialog();
        loadData();
        createForm();
        configureButtons();
    }

    private void configureDialog() {
        setModal(true);
        setDraggable(false);
        setResizable(false);
        setWidth("800px");
        setMaxHeight("90vh");
    }

    private void loadData() {
        // Cargar SOLO vehículos disponibles (no rentados, no en mantenimiento)
        availableVehicles = vehicleService.findAvailableVehicles();
        
        // Cargar clientes activos
        activeCustomers = customerService.findAll().stream()
            .filter(CustomerDTO::getIsActiveCustomer)
            .toList();
    }

    private void createForm() {
        H3 title = new H3("Nueva Renta");
        title.getStyle().set("margin", "0 0 1rem 0");
        
        // ========================================
        // Selección de Vehículo con Preview
        // ========================================
        
        vehicleCombo = new ComboBox<>("Vehículo Disponible");
        vehicleCombo.setPlaceholder("Seleccionar vehículo");
        vehicleCombo.setRequired(true);
        vehicleCombo.setItems(availableVehicles);
        vehicleCombo.setItemLabelGenerator(v -> v.getLicensePlate() + " - " + v.getBrand() + " " + v.getModel());
        vehicleCombo.addValueChangeListener(e -> {
            updateVehiclePreview(e.getValue());
            calculatePrice();
        });
        vehicleCombo.setWidthFull();
        
        // Helper text indicando que solo se muestran disponibles
        vehicleCombo.setHelperText("Solo se muestran vehículos disponibles (" + availableVehicles.size() + " disponibles)");
        
        // Panel de preview del vehículo
        vehiclePreviewPanel = new VerticalLayout();
        vehiclePreviewPanel.setPadding(true);
        vehiclePreviewPanel.setSpacing(true);
        vehiclePreviewPanel.setVisible(false);
        vehiclePreviewPanel.getStyle()
            .set("background", "var(--lumo-contrast-5pct)")
            .set("border-radius", "var(--lumo-border-radius-m)")
            .set("border", "1px solid var(--lumo-contrast-20pct)");
        
        // ========================================
        // Selección de Cliente
        // ========================================
        
        customerCombo = new ComboBox<>("Cliente");
        customerCombo.setPlaceholder("Seleccionar cliente");
        customerCombo.setRequired(true);
        customerCombo.setItems(activeCustomers);
        customerCombo.setItemLabelGenerator(c -> {
            String vipBadge = c.getIsVip() ? " [VIP]" : "";
            return c.getFullName() + " (" + c.getDocumentNumber() + ")" + vipBadge;
        });
        customerCombo.addValueChangeListener(e -> calculatePrice());
        customerCombo.setWidthFull();
        
        // ========================================
        // Fechas
        // ========================================
        
        startDate = new DatePicker("Fecha de Inicio");
        startDate.setLocale(new Locale("es", "SV"));
        startDate.setMin(LocalDate.now());
        startDate.setRequired(true);
        startDate.addValueChangeListener(e -> {
            if (e.getValue() != null) {
                endDate.setMin(e.getValue().plusDays(1));
            }
            calculatePrice();
        });
        
        endDate = new DatePicker("Fecha de Fin");
        endDate.setLocale(new Locale("es", "SV"));
        endDate.setMin(LocalDate.now().plusDays(1));
        endDate.setRequired(true);
        endDate.addValueChangeListener(e -> {
            if (startDate.getValue() != null && e.getValue() != null) {
                if (e.getValue().isBefore(startDate.getValue()) || e.getValue().isEqual(startDate.getValue())) {
                    endDate.setInvalid(true);
                    endDate.setErrorMessage("Debe ser posterior a la fecha de inicio");
                } else {
                    endDate.setInvalid(false);
                }
            }
            calculatePrice();
        });
        
        notes = new TextArea("Notas");
        notes.setPlaceholder("Información adicional...");
        notes.setMaxLength(500);
        
        // Información calculada
        daysLabel = new Span("Días: -");
        daysLabel.getStyle()
            .set("font-weight", "bold")
            .set("color", "var(--lumo-primary-text-color)");
        
        priceLabel = new Span("Total estimado: -");
        priceLabel.getStyle()
            .set("font-weight", "bold")
            .set("font-size", "1.2rem")
            .set("color", "var(--lumo-success-text-color)");
        
        HorizontalLayout infoLayout = new HorizontalLayout(daysLabel, priceLabel);
        infoLayout.setWidthFull();
        infoLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        infoLayout.getStyle()
            .set("padding", "1rem")
            .set("background", "var(--lumo-contrast-5pct)")
            .set("border-radius", "var(--lumo-border-radius-m)");
        
        // Layout del formulario básico
        FormLayout basicFormLayout = new FormLayout();
        basicFormLayout.setResponsiveSteps(
            new FormLayout.ResponsiveStep("0", 1),
            new FormLayout.ResponsiveStep("500px", 2)
        );
        
        basicFormLayout.add(vehicleCombo, 2);
        basicFormLayout.add(customerCombo, 2);
        basicFormLayout.add(startDate, endDate);
        basicFormLayout.add(notes, 2);
        
        // ========================================
        // Sección colapsable: Información de Viaje
        // ========================================
        
        flightNumberField = new TextField("Número de Vuelo");
        flightNumberField.setPlaceholder("Ej: AA1234");
        flightNumberField.setMaxLength(20);
        flightNumberField.setPrefixComponent(VaadinIcon.AIRPLANE.create());
        flightNumberField.setHelperText("Para clientes que llegan del aeropuerto");
        
        itineraryField = new TextArea("Itinerario de Viaje");
        itineraryField.setPlaceholder("Destinos, fechas, actividades planeadas...");
        itineraryField.setMaxLength(2000);
        itineraryField.setHelperText("Lugares que visitará el cliente");
        
        accommodationField = new TextField("Hotel / Hospedaje");
        accommodationField.setPlaceholder("Ej: Hotel Sheraton San Salvador");
        accommodationField.setMaxLength(200);
        accommodationField.setPrefixComponent(VaadinIcon.BED.create());
        
        contactPhoneField = new TextField("Teléfono de Contacto");
        contactPhoneField.setPlaceholder("Ej: +503 7123-4567");
        contactPhoneField.setMaxLength(20);
        contactPhoneField.setPrefixComponent(VaadinIcon.PHONE.create());
        contactPhoneField.setHelperText("Teléfono donde contactar al cliente durante el viaje");
        
        FormLayout travelFormLayout = new FormLayout();
        travelFormLayout.setResponsiveSteps(
            new FormLayout.ResponsiveStep("0", 1),
            new FormLayout.ResponsiveStep("500px", 2)
        );
        
        travelFormLayout.add(flightNumberField, accommodationField);
        travelFormLayout.add(contactPhoneField, 2);
        travelFormLayout.add(itineraryField, 2);
        
        // Crear Details (acordeón colapsable) con icono
        HorizontalLayout travelHeader = new HorizontalLayout();
        travelHeader.setAlignItems(FlexComponent.Alignment.CENTER);
        Icon airplaneIcon = VaadinIcon.AIRPLANE.create();
        airplaneIcon.setSize("16px");
        travelHeader.add(airplaneIcon, new Span("Información de Viaje (Opcional - Para turistas)"));
        
        Details travelDetails = new Details(travelHeader, travelFormLayout);
        travelDetails.setOpened(false);
        travelDetails.getStyle()
            .set("border", "1px solid var(--lumo-contrast-20pct)")
            .set("border-radius", "var(--lumo-border-radius-m)")
            .set("padding", "0.5rem");
        
        // ========================================
        // Binding de campos
        // ========================================
        
        binder.forField(vehicleCombo)
            .withConverter(
                vehicle -> vehicle != null ? vehicle.getId() : null,
                id -> availableVehicles.stream()
                    .filter(v -> v.getId().equals(id))
                    .findFirst()
                    .orElse(null)
            )
            .bind("vehicleId");
        
        binder.forField(customerCombo)
            .withConverter(
                customer -> customer != null ? customer.getId() : null,
                id -> activeCustomers.stream()
                    .filter(c -> c.getId().equals(id))
                    .findFirst()
                    .orElse(null)
            )
            .bind("customerId");
        
        binder.forField(startDate).bind("startDate");
        binder.forField(endDate).bind("endDate");
        binder.forField(notes).bind("notes");
        
        // Binding campos de viaje (opcionales)
        binder.forField(flightNumberField).bind("flightNumber");
        binder.forField(itineraryField).bind("travelItinerary");
        binder.forField(accommodationField).bind("accommodation");
        binder.forField(contactPhoneField).bind("contactPhone");
        
        binder.readBean(command);
        
        // Layout principal
        VerticalLayout content = new VerticalLayout(
            title,
            basicFormLayout,
            vehiclePreviewPanel,
            infoLayout,
            travelDetails
        );
        content.setPadding(true);
        content.setSpacing(true);
        
        add(content);
    }

    /**
     * Actualiza el panel de preview del vehículo seleccionado
     */
    private void updateVehiclePreview(VehicleDTO vehicle) {
        vehiclePreviewPanel.removeAll();
        
        if (vehicle == null) {
            vehiclePreviewPanel.setVisible(false);
            return;
        }
        
        vehiclePreviewPanel.setVisible(true);
        
        // Layout horizontal: Foto + Detalles
        HorizontalLayout previewLayout = new HorizontalLayout();
        previewLayout.setWidthFull();
        previewLayout.setSpacing(true);
        previewLayout.setAlignItems(FlexComponent.Alignment.START);
        
        // Foto del vehículo
        String photoUrl = null;
        if (vehiclePhotoService != null) {
            photoUrl = vehiclePhotoService.getPrimaryPhotoUrl(vehicle.getId());
        }
        
        if (photoUrl != null) {
            Image vehicleImage = new Image(photoUrl, "Vehículo");
            vehicleImage.setWidth("180px");
            vehicleImage.setHeight("120px");
            vehicleImage.getStyle()
                .set("object-fit", "cover")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("border", "2px solid var(--lumo-contrast-20pct)");
            previewLayout.add(vehicleImage);
        } else {
            // Placeholder si no hay foto
            VerticalLayout placeholder = new VerticalLayout();
            placeholder.setWidth("180px");
            placeholder.setHeight("120px");
            placeholder.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
            placeholder.setAlignItems(FlexComponent.Alignment.CENTER);
            placeholder.getStyle()
                .set("background", "var(--lumo-contrast-10pct)")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("border", "2px solid var(--lumo-contrast-20pct)");
            
            Icon carIcon = VaadinIcon.CAR.create();
            carIcon.setSize("48px");
            carIcon.getStyle().set("color", "var(--lumo-contrast-50pct)");
            
            Span noPhotoText = new Span("Sin foto");
            noPhotoText.getStyle()
                .set("font-size", "0.75rem")
                .set("color", "var(--lumo-contrast-50pct)");
            
            placeholder.add(carIcon, noPhotoText);
            previewLayout.add(placeholder);
        }
        
        // Detalles del vehículo
        VerticalLayout detailsLayout = new VerticalLayout();
        detailsLayout.setPadding(false);
        detailsLayout.setSpacing(false);
        detailsLayout.setFlexGrow(1, detailsLayout);
        
        // Título
        H4 vehicleTitle = new H4(vehicle.getBrand() + " " + vehicle.getModel() + " " + vehicle.getYear());
        vehicleTitle.getStyle().set("margin", "0 0 0.5rem 0");
        
        // Placa destacada
        Span plateSpan = new Span("Placa: " + vehicle.getLicensePlate());
        plateSpan.getStyle()
            .set("font-weight", "bold")
            .set("color", "var(--lumo-primary-text-color)");
        
        // Detalles en grid
        Div detailsGrid = new Div();
        detailsGrid.getStyle()
            .set("display", "grid")
            .set("grid-template-columns", "1fr 1fr")
            .set("gap", "0.25rem")
            .set("margin-top", "0.5rem");
        
        addDetailRow(detailsGrid, "Color:", vehicle.getColor());
        addDetailRow(detailsGrid, "Transmisión:", getTransmissionLabel(vehicle.getTransmissionType()));
        addDetailRow(detailsGrid, "Combustible:", getFuelLabel(vehicle.getFuelType()));
        addDetailRow(detailsGrid, "Pasajeros:", vehicle.getPassengerCapacity() + " personas");
        
        // Precios
        Div pricesDiv = new Div();
        pricesDiv.getStyle()
            .set("margin-top", "0.5rem")
            .set("padding-top", "0.5rem")
            .set("border-top", "1px solid var(--lumo-contrast-20pct)");
        
        Span priceNormalSpan = new Span("Normal: " + FormatUtils.formatPrice(vehicle.getPriceNormal()) + "/día");
        priceNormalSpan.getStyle().set("display", "block").set("font-size", "0.875rem");
        
        Span priceVipSpan = new Span("VIP: " + FormatUtils.formatPrice(vehicle.getPriceVip()) + "/día");
        priceVipSpan.getStyle()
            .set("display", "block")
            .set("font-size", "0.875rem")
            .set("color", "#7c5800");
        
        pricesDiv.add(priceNormalSpan, priceVipSpan);
        
        detailsLayout.add(vehicleTitle, plateSpan, detailsGrid, pricesDiv);
        
        previewLayout.add(detailsLayout);
        previewLayout.setFlexGrow(1, detailsLayout);
        
        vehiclePreviewPanel.add(previewLayout);
    }

    private void addDetailRow(Div container, String label, String value) {
        Span labelSpan = new Span(label);
        labelSpan.getStyle()
            .set("font-size", "0.75rem")
            .set("color", "var(--lumo-secondary-text-color)");
        
        Span valueSpan = new Span(value != null ? value : "-");
        valueSpan.getStyle().set("font-size", "0.875rem");
        
        container.add(labelSpan, valueSpan);
    }

    private String getTransmissionLabel(String type) {
        if (type == null) return "-";
        return switch (type) {
            case "MANUAL" -> "Manual";
            case "AUTOMATIC" -> "Automática";
            default -> type;
        };
    }

    private String getFuelLabel(String type) {
        if (type == null) return "-";
        return switch (type) {
            case "GASOLINE" -> "Gasolina";
            case "DIESEL" -> "Diesel";
            case "HYBRID" -> "Híbrido";
            case "ELECTRIC" -> "Eléctrico";
            default -> type;
        };
    }

    private void configureButtons() {
        saveButton = new Button("Crear Renta", VaadinIcon.CHECK.create());
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveButton.addClickListener(e -> save());
        
        cancelButton = new Button("Cancelar", VaadinIcon.CLOSE.create());
        cancelButton.addClickListener(e -> close());
        
        HorizontalLayout buttons = new HorizontalLayout(saveButton, cancelButton);
        buttons.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        buttons.setWidthFull();
        buttons.getStyle().set("padding", "1rem");
        
        getFooter().add(buttons);
    }

    private void calculatePrice() {
        if (startDate.getValue() == null || endDate.getValue() == null) {
            daysLabel.setText("Días: -");
            priceLabel.setText("Total estimado: -");
            return;
        }
        
        long days = ChronoUnit.DAYS.between(startDate.getValue(), endDate.getValue());
        
        if (days <= 0) {
            daysLabel.setText("Días: -");
            priceLabel.setText("Total estimado: -");
            return;
        }
        
        daysLabel.setText("Días: " + days);
        
        // Calcular precio si tenemos vehículo y cliente
        VehicleDTO vehicle = vehicleCombo.getValue();
        CustomerDTO customer = customerCombo.getValue();
        
        if (vehicle != null && customer != null) {
            BigDecimal dailyRate;
            String rateType;
            
            if (customer.getIsVip()) {
                dailyRate = vehicle.getPriceVip();
                rateType = " (tarifa VIP)";
            } else if (days >= 30) {
                dailyRate = vehicle.getPriceMonthly().divide(BigDecimal.valueOf(30), 2, java.math.RoundingMode.HALF_UP);
                rateType = " (tarifa mensual)";
            } else if (days >= 15) {
                dailyRate = vehicle.getPriceMoreThan15Days();
                rateType = " (tarifa +15 días)";
            } else {
                dailyRate = vehicle.getPriceNormal();
                rateType = "";
            }
            
            BigDecimal total = dailyRate.multiply(BigDecimal.valueOf(days));
            priceLabel.setText("Total estimado: " + FormatUtils.formatPrice(total) + rateType);
        } else {
            priceLabel.setText("Total estimado: (selecciona vehículo y cliente)");
        }
    }

    private void save() {
        try {
            binder.writeBean(command);
            command.validate();
            rentalService.createRental(command);
            fireEvent(new SaveEvent(this));
            close();
        } catch (ValidationException e) {
            showErrorNotification("Por favor corrige los errores en el formulario");
        } catch (Exception e) {
            showErrorNotification("Error al crear renta: " + e.getMessage());
        }
    }

    private void showErrorNotification(String message) {
        Notification notification = Notification.show(message, 5000, Notification.Position.TOP_CENTER);
        notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
    }

    // Eventos
    public static class SaveEvent extends ComponentEvent<RentalFormDialog> {
        public SaveEvent(RentalFormDialog source) {
            super(source, false);
        }
    }

    public Registration addSaveListener(ComponentEventListener<SaveEvent> listener) {
        return addListener(SaveEvent.class, listener);
    }
}
