package com.rentacaresv.vehicle.ui;

import com.rentacaresv.vehicle.application.CreateVehicleCommand;
import com.rentacaresv.vehicle.application.VehicleDTO;
import com.rentacaresv.vehicle.application.VehicleService;
import com.rentacaresv.vehicle.domain.FuelType;
import com.rentacaresv.vehicle.domain.TransmissionType;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.shared.Registration;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Diálogo para crear/editar vehículos
 */
public class VehicleFormDialog extends Dialog {

    private final VehicleService vehicleService;
    private final BeanValidationBinder<CreateVehicleCommand> binder;
    private CreateVehicleCommand command;
    
    // Campos del formulario
    private TextField licensePlate;
    private TextField brand;
    private TextField model;
    private IntegerField year;
    private TextField color;
    private ComboBox<TransmissionType> transmissionType;
    private ComboBox<FuelType> fuelType;
    private IntegerField passengerCapacity;
    private IntegerField mileage;
    
    // Precios
    private NumberField priceNormal;
    private NumberField priceVip;
    private NumberField priceMoreThan15Days;
    private NumberField priceMonthly;
    
    private TextArea notes;
    
    // Botones
    private Button saveButton;
    private Button cancelButton;
    
    private final boolean isEdit;

    public VehicleFormDialog(VehicleService vehicleService, VehicleDTO vehicleToEdit) {
        this.vehicleService = vehicleService;
        this.isEdit = vehicleToEdit != null;
        this.command = new CreateVehicleCommand();
        
        this.binder = new BeanValidationBinder<>(CreateVehicleCommand.class);
        
        configureDialog();
        createForm();
        configureButtons();
        
        if (isEdit) {
            populateForm(vehicleToEdit);
        }
    }

    private void configureDialog() {
        setModal(true);
        setDraggable(false);
        setResizable(false);
        setWidth("700px");
        setMaxHeight("90vh");
    }

    private void createForm() {
        H3 title = new H3(isEdit ? "Editar Vehículo" : "Nuevo Vehículo");
        title.getStyle().set("margin", "0 0 1rem 0");
        
        // Campos básicos
        licensePlate = new TextField("Placa");
        licensePlate.setPlaceholder("Ej: P123456");
        licensePlate.setRequired(true);
        licensePlate.setMaxLength(20);
        
        brand = new TextField("Marca");
        brand.setPlaceholder("Ej: Toyota");
        brand.setRequired(true);
        brand.setMaxLength(50);
        
        model = new TextField("Modelo");
        model.setPlaceholder("Ej: Corolla");
        model.setRequired(true);
        model.setMaxLength(50);
        
        year = new IntegerField("Año");
        year.setMin(1900);
        year.setMax(LocalDateTime.now().getYear() + 1);
        year.setValue(LocalDateTime.now().getYear());
        year.setRequired(true);
        year.setStepButtonsVisible(true);
        
        color = new TextField("Color");
        color.setPlaceholder("Ej: Blanco");
        color.setMaxLength(30);
        
        // Combos
        transmissionType = new ComboBox<>("Transmisión");
        transmissionType.setItems(TransmissionType.values());
        transmissionType.setItemLabelGenerator(type -> 
            type == TransmissionType.MANUAL ? "Manual" : "Automática"
        );
        transmissionType.setRequired(true);
        
        fuelType = new ComboBox<>("Combustible");
        fuelType.setItems(FuelType.values());
        fuelType.setItemLabelGenerator(type -> {
            return switch (type) {
                case GASOLINE -> "Gasolina";
                case DIESEL -> "Diesel";
                case HYBRID -> "Híbrido";
                case ELECTRIC -> "Eléctrico";
            };
        });
        fuelType.setRequired(true);
        
        passengerCapacity = new IntegerField("Capacidad de Pasajeros");
        passengerCapacity.setMin(1);
        passengerCapacity.setMax(50);
        passengerCapacity.setValue(5);
        passengerCapacity.setRequired(true);
        passengerCapacity.setStepButtonsVisible(true);
        
        mileage = new IntegerField("Kilometraje");
        mileage.setMin(0);
        mileage.setValue(0);
        mileage.setStepButtonsVisible(true);
        mileage.setSuffixComponent(new com.vaadin.flow.component.html.Span("km"));
        
        // Precios
        priceNormal = createPriceField("Precio Normal (día)");
        priceVip = createPriceField("Precio VIP (día)");
        priceMoreThan15Days = createPriceField("Precio +15 días");
        priceMonthly = createPriceField("Precio Mensual");
        
        notes = new TextArea("Notas");
        notes.setPlaceholder("Información adicional...");
        notes.setMaxLength(500);
        notes.setHelperText("Máximo 500 caracteres");
        
        // Layout del formulario
        FormLayout formLayout = new FormLayout();
        formLayout.setResponsiveSteps(
            new FormLayout.ResponsiveStep("0", 1),
            new FormLayout.ResponsiveStep("500px", 2)
        );
        
        // Organizar campos
        formLayout.add(licensePlate, brand, model, year);
        formLayout.add(color, transmissionType, fuelType, passengerCapacity);
        formLayout.add(mileage, 1);
        
        formLayout.setColspan(licensePlate, 1);
        formLayout.setColspan(brand, 1);
        
        // Sección de precios
        FormLayout pricesLayout = new FormLayout();
        pricesLayout.setResponsiveSteps(
            new FormLayout.ResponsiveStep("0", 2)
        );
        pricesLayout.add(priceNormal, priceVip, priceMoreThan15Days, priceMonthly);
        
        com.vaadin.flow.component.html.H4 pricesTitle = 
            new com.vaadin.flow.component.html.H4("Precios por Categoría");
        pricesTitle.getStyle().set("margin", "1rem 0 0.5rem 0");
        
        // Notas
        formLayout.add(notes, 2);
        formLayout.setColspan(notes, 2);
        
        // Binding
        binder.forField(licensePlate).bind("licensePlate");
        binder.forField(brand).bind("brand");
        binder.forField(model).bind("model");
        binder.forField(year).bind("year");
        binder.forField(color).bind("color");
        binder.forField(transmissionType)
            .withConverter(
                type -> type != null ? type.name() : null,
                name -> name != null ? TransmissionType.valueOf(name) : null
            )
            .bind("transmissionType");
        binder.forField(fuelType)
            .withConverter(
                type -> type != null ? type.name() : null,
                name -> name != null ? FuelType.valueOf(name) : null
            )
            .bind("fuelType");
        binder.forField(passengerCapacity).bind("passengerCapacity");
        binder.forField(mileage).bind("mileage");
        binder.forField(priceNormal)
            .withConverter(
                value -> value != null ? BigDecimal.valueOf(value) : null,
                value -> value != null ? value.doubleValue() : null
            )
            .bind("priceNormal");
        binder.forField(priceVip)
            .withConverter(
                value -> value != null ? BigDecimal.valueOf(value) : null,
                value -> value != null ? value.doubleValue() : null
            )
            .bind("priceVip");
        binder.forField(priceMoreThan15Days)
            .withConverter(
                value -> value != null ? BigDecimal.valueOf(value) : null,
                value -> value != null ? value.doubleValue() : null
            )
            .bind("priceMoreThan15Days");
        binder.forField(priceMonthly)
            .withConverter(
                value -> value != null ? BigDecimal.valueOf(value) : null,
                value -> value != null ? value.doubleValue() : null
            )
            .bind("priceMonthly");
        binder.forField(notes).bind("notes");
        
        binder.readBean(command);
        
        // Layout principal
        VerticalLayout content = new VerticalLayout(
            title,
            formLayout,
            pricesTitle,
            pricesLayout
        );
        content.setPadding(true);
        content.setSpacing(true);
        
        add(content);
    }

    private NumberField createPriceField(String label) {
        NumberField field = new NumberField(label);
        field.setPrefixComponent(new com.vaadin.flow.component.html.Span("$"));
        field.setMin(0.01);
        field.setStep(0.01);
        field.setRequired(true);
        return field;
    }

    private void configureButtons() {
        saveButton = new Button("Guardar");
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

    private void populateForm(VehicleDTO vehicle) {
        licensePlate.setValue(vehicle.getLicensePlate());
        licensePlate.setReadOnly(true); // No se puede cambiar la placa en edición
        
        brand.setValue(vehicle.getBrand());
        model.setValue(vehicle.getModel());
        year.setValue(vehicle.getYear());
        if (vehicle.getColor() != null) color.setValue(vehicle.getColor());
        transmissionType.setValue(TransmissionType.valueOf(vehicle.getTransmissionType()));
        fuelType.setValue(FuelType.valueOf(vehicle.getFuelType()));
        passengerCapacity.setValue(vehicle.getPassengerCapacity());
        mileage.setValue(vehicle.getMileage());
        
        priceNormal.setValue(vehicle.getPriceNormal().doubleValue());
        priceVip.setValue(vehicle.getPriceVip().doubleValue());
        priceMoreThan15Days.setValue(vehicle.getPriceMoreThan15Days().doubleValue());
        priceMonthly.setValue(vehicle.getPriceMonthly().doubleValue());
        
        if (vehicle.getNotes() != null) notes.setValue(vehicle.getNotes());
    }

    private void save() {
        try {
            binder.writeBean(command);
            vehicleService.createVehicle(command);
            fireEvent(new SaveEvent(this));
            close();
        } catch (ValidationException e) {
            showErrorNotification("Por favor corrige los errores en el formulario");
        } catch (Exception e) {
            showErrorNotification("Error al guardar: " + e.getMessage());
        }
    }

    private void showErrorNotification(String message) {
        Notification notification = Notification.show(message, 5000, Notification.Position.TOP_CENTER);
        notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
    }

    // Eventos
    public static class SaveEvent extends ComponentEvent<VehicleFormDialog> {
        public SaveEvent(VehicleFormDialog source) {
            super(source, false);
        }
    }

    public Registration addSaveListener(ComponentEventListener<SaveEvent> listener) {
        return addListener(SaveEvent.class, listener);
    }
}
