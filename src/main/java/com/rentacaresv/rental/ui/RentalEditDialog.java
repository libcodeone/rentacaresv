package com.rentacaresv.rental.ui;

import com.rentacaresv.customer.application.CustomerDTO;
import com.rentacaresv.customer.application.CustomerService;
import com.rentacaresv.rental.application.RentalDTO;
import com.rentacaresv.rental.application.RentalService;
import com.rentacaresv.rental.application.UpdateRentalCommand;
import com.rentacaresv.shared.ui.DateRange;
import com.rentacaresv.shared.ui.ModernDateRangePicker;
import com.rentacaresv.shared.util.FormatUtils;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.shared.Registration;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Diálogo para editar una renta existente en estado PENDING.
 * Permite modificar fechas, cliente, itinerario y salida del país.
 * El vehículo no se puede cambiar.
 */
public class RentalEditDialog extends Dialog {

    private final RentalService rentalService;
    private final CustomerService customerService;
    private final BigDecimal tarifaSacarPais;
    private final RentalDTO rental;

    private final BeanValidationBinder<UpdateRentalCommand> binder;
    private UpdateRentalCommand command;
    private List<CustomerDTO> activeCustomers;

    // Campos del formulario
    private ComboBox<CustomerDTO> customerCombo;
    private ModernDateRangePicker dateRangePicker;
    private TextArea notesField;

    // Información de viaje
    private TextField flightNumberField;
    private TextArea itineraryField;
    private TextField accommodationField;
    private TextField contactPhoneField;

    // Salida del país
    private Checkbox sacarPaisCheckbox;
    private TextField destinosField;
    private IntegerField diasFueraPaisField;

    // Info calculada
    private Span daysLabel;
    private Span priceLabel;

    public RentalEditDialog(RentalService rentalService,
                            CustomerService customerService,
                            BigDecimal tarifaSacarPais,
                            RentalDTO rental) {
        this.rentalService = rentalService;
        this.customerService = customerService;
        this.tarifaSacarPais = tarifaSacarPais != null ? tarifaSacarPais : BigDecimal.ZERO;
        this.rental = rental;
        this.binder = new BeanValidationBinder<>(UpdateRentalCommand.class);
        this.command = buildCommandFromRental(rental);

        configureDialog();
        loadData();
        buildForm();
        configureButtons();
    }

    // ── Setup ────────────────────────────────────────────────────────────────

    private void configureDialog() {
        setModal(true);
        setDraggable(false);
        setResizable(false);
        setWidth("750px");
        setMaxHeight("92vh");
    }

    private void loadData() {
        activeCustomers = customerService.findAll().stream()
                .filter(CustomerDTO::getIsActiveCustomer)
                .toList();
    }

    private UpdateRentalCommand buildCommandFromRental(RentalDTO r) {
        return UpdateRentalCommand.builder()
                .rentalId(r.getId())
                .customerId(r.getCustomerId())
                .startDate(r.getStartDate())
                .endDate(r.getEndDate())
                .notes(r.getNotes())
                .flightNumber(r.getFlightNumber())
                .travelItinerary(r.getTravelItinerary())
                .accommodation(r.getAccommodation())
                .contactPhone(r.getContactPhone())
                .sacarPais(Boolean.TRUE.equals(r.getSacarPais()))
                .destinosFueraPais(r.getDestinosFueraPais())
                .diasFueraPais(r.getDiasFueraPais())
                .build();
    }

    // ── Form ─────────────────────────────────────────────────────────────────

    private void buildForm() {
        H3 title = new H3("Editar Renta — " + rental.getContractNumber());
        title.getStyle().set("margin", "0 0 0.25rem 0");

        // Vehículo (solo informativo)
        Span vehicleInfo = new Span("🚗 " + rental.getVehicleDescription() + " · " + rental.getVehicleLicensePlate());
        vehicleInfo.getStyle()
                .set("color", "var(--lumo-secondary-text-color)")
                .set("font-size", "0.9rem")
                .set("margin-bottom", "0.75rem")
                .set("display", "block");

        // ── Cliente ───────────────────────────────────────────────────────────

        customerCombo = new ComboBox<>("Cliente");
        customerCombo.setItems(activeCustomers);
        customerCombo.setItemLabelGenerator(c ->
                c.getFullName() + " (" + c.getDocumentNumber() + ")" + (c.getIsVip() ? " [VIP]" : ""));
        customerCombo.setRequired(true);
        customerCombo.setWidthFull();
        customerCombo.addValueChangeListener(e -> calculatePrice());

        // ── Fechas ────────────────────────────────────────────────────────────

        dateRangePicker = new ModernDateRangePicker("Fechas de Renta");
        dateRangePicker.addValueChangeListener(e -> calculatePrice());

        // Bloquear fechas ocupadas de OTROS contratos del mismo vehículo
        loadDisabledDates();

        // ── Notas ─────────────────────────────────────────────────────────────

        notesField = new TextArea("Notas");
        notesField.setPlaceholder("Información adicional...");
        notesField.setMaxLength(500);

        // ── Preview de precio ─────────────────────────────────────────────────

        daysLabel = new Span("Días: —");
        daysLabel.getStyle().set("font-weight", "bold").set("color", "var(--lumo-primary-text-color)");

        priceLabel = new Span("Total estimado: —");
        priceLabel.getStyle()
                .set("font-weight", "bold")
                .set("font-size", "1.15rem")
                .set("color", "var(--lumo-success-text-color)");

        HorizontalLayout infoLayout = new HorizontalLayout(daysLabel, priceLabel);
        infoLayout.setWidthFull();
        infoLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        infoLayout.getStyle()
                .set("padding", "0.75rem 1rem")
                .set("background", "var(--lumo-contrast-5pct)")
                .set("border-radius", "var(--lumo-border-radius-m)");

        FormLayout basicLayout = new FormLayout();
        basicLayout.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("500px", 2));
        basicLayout.add(customerCombo, 2);
        basicLayout.add(dateRangePicker, 2);
        basicLayout.add(notesField, 2);

        // ── Información de viaje (colapsable) ─────────────────────────────────

        flightNumberField = new TextField("Número de Vuelo");
        flightNumberField.setPlaceholder("Ej: AA1234");
        flightNumberField.setMaxLength(20);
        flightNumberField.setPrefixComponent(VaadinIcon.AIRPLANE.create());

        itineraryField = new TextArea("Itinerario de Viaje");
        itineraryField.setPlaceholder("Destinos, fechas, actividades planeadas...");
        itineraryField.setMaxLength(2000);

        accommodationField = new TextField("Hotel / Hospedaje");
        accommodationField.setPlaceholder("Ej: Hotel Sheraton San Salvador");
        accommodationField.setMaxLength(200);
        accommodationField.setPrefixComponent(VaadinIcon.BED.create());

        contactPhoneField = new TextField("Teléfono de Contacto");
        contactPhoneField.setPlaceholder("Ej: +503 7123-4567");
        contactPhoneField.setMaxLength(20);
        contactPhoneField.setPrefixComponent(VaadinIcon.PHONE.create());

        FormLayout travelLayout = new FormLayout();
        travelLayout.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("500px", 2));
        travelLayout.add(flightNumberField, accommodationField);
        travelLayout.add(contactPhoneField, 2);
        travelLayout.add(itineraryField, 2);

        HorizontalLayout travelHeader = new HorizontalLayout();
        travelHeader.setAlignItems(FlexComponent.Alignment.CENTER);
        Icon airIcon = VaadinIcon.AIRPLANE.create();
        airIcon.setSize("16px");
        travelHeader.add(airIcon, new Span("Información de Viaje"));

        Details travelDetails = new Details(travelHeader, travelLayout);
        travelDetails.setOpened(rental.getFlightNumber() != null || rental.getTravelItinerary() != null
                || rental.getAccommodation() != null);
        travelDetails.getStyle()
                .set("border", "1px solid var(--lumo-contrast-20pct)")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("padding", "0.5rem");

        // ── Salida del país (colapsable) ──────────────────────────────────────

        sacarPaisCheckbox = new Checkbox("Autoriza sacar el vehículo fuera del país");

        destinosField = new TextField("Destinos fuera del país");
        destinosField.setPlaceholder("Ej: Guatemala, Honduras");
        destinosField.setMaxLength(300);
        destinosField.setPrefixComponent(VaadinIcon.GLOBE.create());

        diasFueraPaisField = new IntegerField("Días fuera del país");
        diasFueraPaisField.setMin(1);
        diasFueraPaisField.setMax(365);
        diasFueraPaisField.setStepButtonsVisible(true);
        if (tarifaSacarPais.compareTo(BigDecimal.ZERO) > 0) {
            diasFueraPaisField.setHelperText("Tarifa: $" + tarifaSacarPais + "/día");
        }

        boolean sacarPaisActual = Boolean.TRUE.equals(rental.getSacarPais());
        destinosField.setEnabled(sacarPaisActual);
        diasFueraPaisField.setEnabled(sacarPaisActual);

        sacarPaisCheckbox.addValueChangeListener(e -> {
            boolean checked = Boolean.TRUE.equals(e.getValue());
            destinosField.setEnabled(checked);
            diasFueraPaisField.setEnabled(checked);
            if (!checked) {
                destinosField.clear();
                diasFueraPaisField.setValue(null);
            }
            calculatePrice();
        });
        diasFueraPaisField.addValueChangeListener(e -> calculatePrice());

        FormLayout sacarPaisLayout = new FormLayout();
        sacarPaisLayout.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("500px", 2));
        sacarPaisLayout.add(sacarPaisCheckbox, 2);
        sacarPaisLayout.add(destinosField, diasFueraPaisField);

        HorizontalLayout globeHeader = new HorizontalLayout();
        globeHeader.setAlignItems(FlexComponent.Alignment.CENTER);
        Icon globeIcon = VaadinIcon.GLOBE.create();
        globeIcon.setSize("16px");
        globeHeader.add(globeIcon, new Span("Salida del País"));

        Details sacarPaisDetails = new Details(globeHeader, sacarPaisLayout);
        sacarPaisDetails.setOpened(sacarPaisActual);
        sacarPaisDetails.getStyle()
                .set("border", "1px solid var(--lumo-contrast-20pct)")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("padding", "0.5rem");

        // ── Bindings ──────────────────────────────────────────────────────────

        binder.forField(customerCombo)
                .withConverter(
                        c -> c != null ? c.getId() : null,
                        id -> activeCustomers.stream().filter(c -> c.getId().equals(id)).findFirst().orElse(null))
                .bind("customerId");

        binder.forField(dateRangePicker)
                .withValidator(v -> v != null && v.getStartDate() != null && v.getEndDate() != null,
                        "Debe seleccionar un rango de fechas completo")
                .bind(
                        c -> (c.getStartDate() != null && c.getEndDate() != null)
                                ? new DateRange(c.getStartDate(), c.getEndDate()) : null,
                        (c, r) -> {
                            if (r != null) { c.setStartDate(r.getStartDate()); c.setEndDate(r.getEndDate()); }
                        });

        binder.forField(notesField).bind("notes");
        binder.forField(flightNumberField).bind("flightNumber");
        binder.forField(itineraryField).bind("travelItinerary");
        binder.forField(accommodationField).bind("accommodation");
        binder.forField(contactPhoneField).bind("contactPhone");
        binder.forField(sacarPaisCheckbox).bind("sacarPais");
        binder.forField(destinosField).bind("destinosFueraPais");
        binder.forField(diasFueraPaisField)
                .withConverter(val -> val, val -> val)
                .bind("diasFueraPais");

        binder.readBean(command);

        // Forzar cálculo inicial con los valores pre-cargados
        calculatePrice();

        // ── Layout principal ──────────────────────────────────────────────────

        VerticalLayout content = new VerticalLayout(
                title, vehicleInfo,
                basicLayout,
                infoLayout,
                travelDetails,
                sacarPaisDetails);
        content.setPadding(true);
        content.setSpacing(true);

        add(content);
    }

    // ── Fechas bloqueadas ─────────────────────────────────────────────────────

    private void loadDisabledDates() {
        List<RentalDTO> otherRentals = rentalService.findActiveRentalsByVehicleId(
                rental.getVehicleId()).stream()
                .filter(r -> !r.getId().equals(rental.getId()))
                .toList();

        List<java.time.LocalDate> disabled = new ArrayList<>();
        for (RentalDTO r : otherRentals) {
            java.time.LocalDate cur = r.getStartDate();
            while (!cur.isAfter(r.getEndDate())) {
                disabled.add(cur);
                cur = cur.plusDays(1);
            }
        }
        dateRangePicker.setDisabledDates(disabled);
    }

    // ── Precio estimado ───────────────────────────────────────────────────────

    private void calculatePrice() {
        DateRange range = dateRangePicker.getValue();
        if (range == null || range.getStartDate() == null || range.getEndDate() == null) {
            daysLabel.setText("Días: —");
            priceLabel.setText("Total estimado: —");
            return;
        }

        long days = java.time.temporal.ChronoUnit.DAYS.between(range.getStartDate(), range.getEndDate()) + 1;
        if (days <= 0) {
            daysLabel.setText("Días: —");
            priceLabel.setText("Total estimado: —");
            return;
        }

        daysLabel.setText("Días: " + days);

        CustomerDTO customer = customerCombo.getValue();
        if (customer == null) {
            priceLabel.setText("Total estimado: (selecciona cliente)");
            return;
        }

        // Usar la tarifa diaria actual de la renta como base (el vehículo no cambia)
        BigDecimal dailyRate = rental.getDailyRate();
        BigDecimal total = dailyRate.multiply(BigDecimal.valueOf(days));
        String extra = "";

        if (Boolean.TRUE.equals(sacarPaisCheckbox.getValue())
                && diasFueraPaisField.getValue() != null
                && diasFueraPaisField.getValue() > 0
                && tarifaSacarPais.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal cargo = tarifaSacarPais.multiply(BigDecimal.valueOf(diasFueraPaisField.getValue()));
            total = total.add(cargo);
            extra = " + $" + cargo + " (salida país)";
        }

        priceLabel.setText("Total estimado: " + FormatUtils.formatPrice(total) + extra);
    }

    // ── Botones ───────────────────────────────────────────────────────────────

    private void configureButtons() {
        Button saveButton = new Button("Guardar Cambios", VaadinIcon.CHECK.create());
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveButton.addClickListener(e -> save());

        Button cancelButton = new Button("Cancelar", VaadinIcon.CLOSE.create());
        cancelButton.addClickListener(e -> close());

        HorizontalLayout buttons = new HorizontalLayout(saveButton, cancelButton);
        buttons.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        buttons.setWidthFull();
        buttons.getStyle().set("padding", "0.75rem 1rem");

        getFooter().add(buttons);
    }

    private void save() {
        try {
            binder.writeBean(command);
            command.validate();
            rentalService.updateRental(command);
            fireEvent(new SaveEvent(this));
            close();
        } catch (ValidationException e) {
            showError("Por favor corrige los errores en el formulario");
        } catch (Exception e) {
            showError("Error al guardar: " + e.getMessage());
        }
    }

    private void showError(String msg) {
        Notification n = Notification.show(msg, 5000, Notification.Position.TOP_CENTER);
        n.addThemeVariants(NotificationVariant.LUMO_ERROR);
    }

    // ── Evento ───────────────────────────────────────────────────────────────

    public static class SaveEvent extends ComponentEvent<RentalEditDialog> {
        public SaveEvent(RentalEditDialog source) { super(source, false); }
    }

    public Registration addSaveListener(ComponentEventListener<SaveEvent> listener) {
        return addListener(SaveEvent.class, listener);
    }
}
