package com.rentacaresv.rental.ui;

import com.rentacaresv.customer.application.CustomerService;
import com.rentacaresv.rental.application.CreateRentalCommand;
import com.rentacaresv.rental.application.RentalService;
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
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
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

import java.time.LocalDate;
import java.util.Locale;

/**
 * Diálogo para crear nuevas rentas
 */
public class RentalFormDialog extends Dialog {

    private final RentalService rentalService;
    private final VehicleService vehicleService;
    private final CustomerService customerService;
    private final BeanValidationBinder<CreateRentalCommand> binder;
    private CreateRentalCommand command;
    
    // Campos básicos del formulario
    private ComboBox<VehicleItem> vehicleCombo;
    private ComboBox<CustomerItem> customerCombo;
    private DatePicker startDate;
    private DatePicker endDate;
    private TextArea notes;
    
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
        
        this.rentalService = rentalService;
        this.vehicleService = vehicleService;
        this.customerService = customerService;
        this.command = new CreateRentalCommand();
        this.binder = new BeanValidationBinder<>(CreateRentalCommand.class);
        
        configureDialog();
        createForm();
        configureButtons();
        loadData();
    }

    private void configureDialog() {
        setModal(true);
        setDraggable(false);
        setResizable(false);
        setWidth("700px");
        setMaxHeight("90vh");
    }

    private void createForm() {
        H3 title = new H3("Nueva Renta");
        title.getStyle().set("margin", "0 0 1rem 0");
        
        // ========================================
        // Información básica de la renta
        // ========================================
        
        vehicleCombo = new ComboBox<>("Vehículo");
        vehicleCombo.setPlaceholder("Seleccionar vehículo disponible");
        vehicleCombo.setRequired(true);
        vehicleCombo.setItemLabelGenerator(VehicleItem::getLabel);
        vehicleCombo.addValueChangeListener(e -> calculatePrice());
        
        customerCombo = new ComboBox<>("Cliente");
        customerCombo.setPlaceholder("Seleccionar cliente");
        customerCombo.setRequired(true);
        customerCombo.setItemLabelGenerator(CustomerItem::getLabel);
        customerCombo.addValueChangeListener(e -> calculatePrice());
        
        startDate = new DatePicker("Fecha de Inicio");
        startDate.setLocale(new Locale("es", "SV"));
        startDate.setMin(LocalDate.now());
        startDate.setRequired(true);
        startDate.addValueChangeListener(e -> calculatePrice());
        
        endDate = new DatePicker("Fecha de Fin");
        endDate.setLocale(new Locale("es", "SV"));
        endDate.setMin(LocalDate.now().plusDays(1));
        endDate.setRequired(true);
        endDate.addValueChangeListener(e -> {
            if (startDate.getValue() != null && e.getValue() != null) {
                if (e.getValue().isBefore(startDate.getValue())) {
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
            .set("color", "var(--lumo-primary-color)");
        
        priceLabel = new Span("Total estimado: -");
        priceLabel.getStyle()
            .set("font-weight", "bold")
            .set("font-size", "1.2rem")
            .set("color", "var(--lumo-success-color)");
        
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
        itineraryField.setHelperText("Lugares que visitará el cliente (hoteles, ciudades, playas, etc.)");
        
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
        
        // Crear Details (acordeón colapsable)
        Details travelDetails = new Details(
            "✈️ Información de Viaje (Opcional - Para turistas)",
            travelFormLayout
        );
        travelDetails.setOpened(false);
        travelDetails.getStyle()
            .set("border", "1px solid var(--lumo-contrast-20pct)")
            .set("border-radius", "var(--lumo-border-radius-m)")
            .set("padding", "0.5rem");
        
        // Agregar tooltip informativo
        Span travelInfo = new Span("💡 Complete esta sección solo si el cliente es turista o viene del aeropuerto");
        travelInfo.getStyle()
            .set("font-size", "0.875rem")
            .set("color", "var(--lumo-secondary-text-color)")
            .set("font-style", "italic");
        
        // ========================================
        // Binding de campos
        // ========================================
        
        binder.forField(vehicleCombo)
            .withConverter(
                item -> item != null ? item.getId() : null,
                id -> vehicleCombo.getListDataView().getItems()
                    .filter(item -> item.getId().equals(id))
                    .findFirst()
                    .orElse(null)
            )
            .bind("vehicleId");
        
        binder.forField(customerCombo)
            .withConverter(
                item -> item != null ? item.getId() : null,
                id -> customerCombo.getListDataView().getItems()
                    .filter(item -> item.getId().equals(id))
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
            infoLayout,
            travelInfo,
            travelDetails
        );
        content.setPadding(true);
        content.setSpacing(true);
        
        add(content);
    }

    private void configureButtons() {
        saveButton = new Button("Crear Renta");
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveButton.addClickListener(e -> save());
        
        cancelButton = new Button("Cancelar");
        cancelButton.addClickListener(e -> close());
        
        HorizontalLayout buttons = new HorizontalLayout(saveButton, cancelButton);
        buttons.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        buttons.setWidthFull();
        buttons.getStyle().set("padding", "1rem");
        
        getFooter().add(buttons);
    }

    private void loadData() {
        if (vehicleService != null) {
            var vehicles = vehicleService.findAvailableVehicles().stream()
                .map(v -> new VehicleItem(v.getId(), v.getLicensePlate(), v.getFullDescription()))
                .toList();
            vehicleCombo.setItems(vehicles);
        }
        
        if (customerService != null) {
            var customers = customerService.findAll().stream()
                .filter(c -> c.getIsActiveCustomer())
                .map(c -> new CustomerItem(c.getId(), c.getFullName(), c.getDocumentNumber(), c.getIsVip()))
                .toList();
            customerCombo.setItems(customers);
        }
    }

    private void calculatePrice() {
        if (startDate.getValue() == null || endDate.getValue() == null) {
            daysLabel.setText("Días: -");
            priceLabel.setText("Total estimado: -");
            return;
        }
        
        long days = java.time.temporal.ChronoUnit.DAYS.between(
            startDate.getValue(),
            endDate.getValue()
        );
        
        if (days <= 0) {
            daysLabel.setText("Días: -");
            priceLabel.setText("Total estimado: -");
            return;
        }
        
        daysLabel.setText("Días: " + days);
        priceLabel.setText("Total estimado: (selecciona vehículo y cliente)");
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

    // Clases auxiliares
    private record VehicleItem(Long id, String licensePlate, String description) {
        public String getLabel() {
            return licensePlate + " - " + description;
        }
        public Long getId() { return id; }
    }

    private record CustomerItem(Long id, String name, String document, Boolean isVip) {
        public String getLabel() {
            String vipBadge = isVip ? " ⭐ VIP" : "";
            return name + " (" + document + ")" + vipBadge;
        }
        public Long getId() { return id; }
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
