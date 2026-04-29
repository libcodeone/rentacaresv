package com.rentacaresv.rental.ui;

import com.rentacaresv.customer.application.CustomerDTO;
import com.rentacaresv.customer.application.CustomerService;
import com.rentacaresv.rental.application.CreateRentalCommand;
import com.rentacaresv.rental.application.RentalDTO;
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

import com.rentacaresv.shared.ui.ModernDateRangePicker;
import com.rentacaresv.shared.ui.DateRange;
import com.vaadin.flow.component.checkbox.Checkbox;
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
import com.vaadin.flow.component.textfield.IntegerField;
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
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

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

    // Mensaje de ayuda para fechas no disponibles
    private Span unavailableDatesMessage;

    // Lista de vehículos disponibles
    private List<VehicleDTO> availableVehicles;
    private List<CustomerDTO> activeCustomers;

    // Campos básicos del formulario
    private ComboBox<VehicleDTO> vehicleCombo;
    private ComboBox<CustomerDTO> customerCombo;
    private ModernDateRangePicker dateRangePicker;
    private TextArea notes;

    // Panel de preview del vehículo
    private VerticalLayout vehiclePreviewPanel;

    // Campos de información de viaje (opcionales)
    private TextField flightNumberField;
    private TextArea itineraryField;
    private TextField accommodationField;
    private TextField contactPhoneField;

    // Campos de salida del país (opcionales)
    private Checkbox sacarPaisCheckbox;
    private TextField destinosField;
    private IntegerField diasFueraPaisField;

    // Tarifa de salida del país (leída de Settings)
    private final BigDecimal tarifaSacarPais;

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
        this(rentalService, vehicleService, customerService, null, null);
    }

    public RentalFormDialog(
            RentalService rentalService,
            VehicleService vehicleService,
            CustomerService customerService,
            VehiclePhotoService vehiclePhotoService) {
        this(rentalService, vehicleService, customerService, vehiclePhotoService, null);
    }

    public RentalFormDialog(
            RentalService rentalService,
            VehicleService vehicleService,
            CustomerService customerService,
            VehiclePhotoService vehiclePhotoService,
            BigDecimal tarifaSacarPais) {

        this.rentalService = rentalService;
        this.vehicleService = vehicleService;
        this.customerService = customerService;
        this.vehiclePhotoService = vehiclePhotoService;
        this.tarifaSacarPais = tarifaSacarPais != null ? tarifaSacarPais : BigDecimal.ZERO;
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
            updateDisabledDates(e.getValue());
            calculatePrice();
        });
        vehicleCombo.setWidthFull();

        // Helper text indicando que solo se muestran disponibles
        vehicleCombo
                .setHelperText("Solo se muestran vehículos disponibles (" + availableVehicles.size() + " disponibles)");

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

        dateRangePicker = new ModernDateRangePicker("Fechas de Renta");
        dateRangePicker.addValueChangeListener(e -> {
            calculatePrice();
        });

        // Mensaje de ayuda para fechas no disponibles
        // Ahora es un Details (acordeón expandible) en lugar de un Span
        unavailableDatesMessage = new Span();
        unavailableDatesMessage.setVisible(false);

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
                new FormLayout.ResponsiveStep("500px", 2));

        basicFormLayout.add(vehicleCombo, 2);
        basicFormLayout.add(customerCombo, 2);
        basicFormLayout.add(dateRangePicker, 2);
        basicFormLayout.add(unavailableDatesMessage, 2);
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
                new FormLayout.ResponsiveStep("500px", 2));

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
        // Sección colapsable: Salida del País
        // ========================================

        sacarPaisCheckbox = new Checkbox("Autoriza sacar el vehículo fuera del país");
        sacarPaisCheckbox.getStyle().set("margin-bottom", "0.5rem");

        destinosField = new TextField("Destinos fuera del país");
        destinosField.setPlaceholder("Ej: Guatemala, Honduras");
        destinosField.setMaxLength(300);
        destinosField.setPrefixComponent(VaadinIcon.GLOBE.create());
        destinosField.setEnabled(false);

        diasFueraPaisField = new IntegerField("Días fuera del país");
        diasFueraPaisField.setMin(1);
        diasFueraPaisField.setMax(365);
        diasFueraPaisField.setStepButtonsVisible(true);
        diasFueraPaisField.setEnabled(false);

        if (tarifaSacarPais.compareTo(BigDecimal.ZERO) > 0) {
            diasFueraPaisField.setHelperText("Tarifa: $" + tarifaSacarPais + "/día");
        }

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

        FormLayout sacarPaisFormLayout = new FormLayout();
        sacarPaisFormLayout.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("500px", 2));
        sacarPaisFormLayout.add(sacarPaisCheckbox, 2);
        sacarPaisFormLayout.add(destinosField, diasFueraPaisField);

        HorizontalLayout sacarPaisHeader = new HorizontalLayout();
        sacarPaisHeader.setAlignItems(FlexComponent.Alignment.CENTER);
        Icon globeIcon = VaadinIcon.GLOBE.create();
        globeIcon.setSize("16px");
        sacarPaisHeader.add(globeIcon, new Span("Salida del País (Opcional)"));

        Details sacarPaisDetails = new Details(sacarPaisHeader, sacarPaisFormLayout);
        sacarPaisDetails.setOpened(false);
        sacarPaisDetails.getStyle()
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
                                .orElse(null))
                .bind("vehicleId");

        binder.forField(customerCombo)
                .withConverter(
                        customer -> customer != null ? customer.getId() : null,
                        id -> activeCustomers.stream()
                                .filter(c -> c.getId().equals(id))
                                .findFirst()
                                .orElse(null))
                .bind("customerId");

        binder.forField(dateRangePicker)
                .withValidator(v -> v != null && v.getStartDate() != null && v.getEndDate() != null,
                        "Debe seleccionar un rango de fechas completo")
                .bind(
                        c -> (c.getStartDate() != null && c.getEndDate() != null)
                                ? new DateRange(c.getStartDate(), c.getEndDate())
                                : null,
                        (c, range) -> {
                            if (range != null) {
                                c.setStartDate(range.getStartDate());
                                c.setEndDate(range.getEndDate());
                            } else {
                                c.setStartDate(null);
                                c.setEndDate(null);
                            }
                        });
        binder.forField(notes).bind("notes");

        // Binding campos de viaje (opcionales)
        binder.forField(flightNumberField).bind("flightNumber");
        binder.forField(itineraryField).bind("travelItinerary");
        binder.forField(accommodationField).bind("accommodation");
        binder.forField(contactPhoneField).bind("contactPhone");

        // Binding campos de salida del país
        binder.forField(sacarPaisCheckbox).bind("sacarPais");
        binder.forField(destinosField).bind("destinosFueraPais");
        binder.forField(diasFueraPaisField)
                .withConverter(
                        val -> val,          // Integer → Integer (null permitido)
                        val -> val)          // Integer → Integer (null permitido)
                .bind("diasFueraPais");

        binder.readBean(command);

        // Layout principal
        VerticalLayout content = new VerticalLayout(
                title,
                basicFormLayout,
                vehiclePreviewPanel,
                infoLayout,
                travelDetails,
                sacarPaisDetails);
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
        if (type == null)
            return "-";
        return switch (type) {
            case "MANUAL" -> "Manual";
            case "AUTOMATIC" -> "Automática";
            default -> type;
        };
    }

    private String getFuelLabel(String type) {
        if (type == null)
            return "-";
        return switch (type) {
            case "GASOLINE" -> "Gasolina";
            case "DIESEL" -> "Diesel";
            case "HYBRID" -> "Híbrido";
            case "ELECTRIC" -> "Eléctrico";
            default -> type;
        };
    }

    /**
     * Actualiza las fechas deshabilitadas en los DatePickers
     * según las rentas activas del vehículo seleccionado
     */
    private void updateDisabledDates(VehicleDTO vehicle) {
        if (vehicle == null) {
            // Limpiar fechas deshabilitadas
            dateRangePicker.setEnabled(true);
            unavailableDatesMessage.setVisible(false);
            dateRangePicker.setDisabledDates(new ArrayList<>());
            return;
        }

        // Obtener rentas activas/pendientes del vehículo
        List<RentalDTO> activeRentals = rentalService.findActiveRentalsByVehicleId(vehicle.getId());

        // Filtrar solo rentas futuras o que terminen en el futuro (ignorar rentas
        // pasadas completas)
        LocalDate today = LocalDate.now();
        activeRentals = activeRentals.stream()
                .filter(rental -> !rental.getEndDate().isBefore(today)) // Solo si terminan hoy o en el futuro
                .collect(Collectors.toList());

        if (activeRentals.isEmpty()) {
            // No hay rentas, todas las fechas disponibles
            unavailableDatesMessage.setVisible(false);
            dateRangePicker.setDisabledDates(new ArrayList<>());
            return;
        }

        // Construir lista de todas las fechas ocupadas
        List<LocalDate> disabledDates = new ArrayList<>();

        for (RentalDTO rental : activeRentals) {
            LocalDate current = rental.getStartDate();
            LocalDate end = rental.getEndDate();

            // Agregar todas las fechas del rango (inclusive)
            while (!current.isAfter(end)) {
                disabledDates.add(current);
                current = current.plusDays(1);
            }
        }

        // Bloqueo visual de fechas ocupadas inyectando Javascript (Vaadin Flow
        // DatePicker workaround)

        // En lugar de manual javascript form, empujamos al Java Wrapper
        dateRangePicker.setDisabledDates(disabledDates);

        dateRangePicker.addValueChangeListener(event -> {
            DateRange selected = event.getValue();
            if (selected != null) {
                boolean invalid = false;
                if (selected.getStartDate() != null && disabledDates.contains(selected.getStartDate()))
                    invalid = true;
                if (selected.getEndDate() != null && disabledDates.contains(selected.getEndDate()))
                    invalid = true;

                if (!invalid && selected.getStartDate() != null && selected.getEndDate() != null) {
                    LocalDate curr = selected.getStartDate();
                    while (!curr.isAfter(selected.getEndDate())) {
                        if (disabledDates.contains(curr)) {
                            invalid = true;
                            break;
                        }
                        curr = curr.plusDays(1);
                    }
                }

                if (invalid) {
                    dateRangePicker.setInvalid(true);
                    dateRangePicker.setErrorMessage("El rango contiene fechas ocupadas.");
                } else {
                    dateRangePicker.setInvalid(false);
                }
            }
        });

        // Mostrar mensaje informativo con diseño mejorado
        if (!disabledDates.isEmpty()) {
            // Limpiar contenido anterior
            unavailableDatesMessage.removeAll();

            // Crear layout para el mensaje
            VerticalLayout messageLayout = new VerticalLayout();
            messageLayout.setPadding(false);
            messageLayout.setSpacing(false);
            messageLayout.getStyle()
                    .set("background", "#E3F2FD") // Azul claro Material Design
                    .set("border-left", "4px solid #2196F3") // Azul primario
                    .set("border-radius", "var(--lumo-border-radius-m)")
                    .set("padding", "0.75rem")
                    .set("margin-bottom", "1rem");

            // Encabezado con ícono animado
            HorizontalLayout header = new HorizontalLayout();
            header.setAlignItems(FlexComponent.Alignment.CENTER);
            header.setSpacing(true);

            Icon warningIcon = VaadinIcon.WARNING.create();
            warningIcon.setSize("20px");
            warningIcon.getStyle()
                    .set("color", "#1976D2") // Azul oscuro
                    .set("animation", "pulse 2s ease-in-out infinite");

            // Inyectar CSS para la animación
            warningIcon.getElement().executeJs(
                    "const style = document.createElement('style'); " +
                            "style.textContent = '@keyframes pulse { 0%, 100% { opacity: 1; transform: scale(1); } 50% { opacity: 0.7; transform: scale(1.1); } }'; "
                            +
                            "document.head.appendChild(style);");

            Span headerText = new Span(
                    String.format("Vehículo %s tiene %d renta(s) activa(s)",
                            vehicle.getLicensePlate(),
                            activeRentals.size()));
            headerText.getStyle()
                    .set("font-weight", "600")
                    .set("color", "#1565C0") // Azul oscuro
                    .set("font-size", "0.9rem");

            header.add(warningIcon, headerText);
            messageLayout.add(header);

            // Texto explicativo
            Span explanation = new Span("Las fechas ocupadas están bloqueadas en el calendario.");
            explanation.getStyle()
                    .set("font-size", "0.8rem")
                    .set("color", "var(--lumo-secondary-text-color)")
                    .set("margin-top", "0.25rem")
                    .set("display", "block");
            messageLayout.add(explanation);

            // Detalles expandibles con las fechas
            VerticalLayout rangesLayout = new VerticalLayout();
            rangesLayout.setPadding(false);
            rangesLayout.setSpacing(false);
            rangesLayout.getStyle().set("margin-top", "0.5rem");

            // Formatter para las fechas en español
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy", new Locale("es", "SV"));

            for (RentalDTO rental : activeRentals) {
                // Chip para cada renta
                HorizontalLayout chip = new HorizontalLayout();
                chip.setAlignItems(FlexComponent.Alignment.CENTER);
                chip.setSpacing(true);
                chip.getStyle()
                        .set("background", "var(--lumo-contrast-5pct)")
                        .set("border-radius", "16px")
                        .set("padding", "0.25rem 0.75rem")
                        .set("margin", "0.25rem 0")
                        .set("display", "inline-flex")
                        .set("font-size", "0.75rem");

                Icon calendarIcon = VaadinIcon.CALENDAR.create();
                calendarIcon.setSize("14px");
                calendarIcon.getStyle().set("color", "var(--lumo-error-text-color)");

                Span dateRange = new Span(
                        rental.getStartDate().format(formatter) + " - " + rental.getEndDate().format(formatter));
                dateRange.getStyle()
                        .set("font-weight", "500")
                        .set("color", "var(--lumo-body-text-color)");

                Span contractNumber = new Span(rental.getContractNumber());
                contractNumber.getStyle()
                        .set("color", "var(--lumo-secondary-text-color)")
                        .set("font-size", "0.7rem")
                        .set("margin-left", "0.5rem");

                chip.add(calendarIcon, dateRange, contractNumber);
                rangesLayout.add(chip);
            }

            // Details (acordeón) para mostrar/ocultar fechas
            Details details = new Details("Ver fechas ocupadas: " + activeRentals.size(), rangesLayout);
            details.setOpened(false);
            details.getStyle()
                    .set("margin-top", "0.5rem")
                    .set("cursor", "pointer");

            messageLayout.add(details);

            // Añadir al Span contenedor
            unavailableDatesMessage.removeAll();
            unavailableDatesMessage.add(messageLayout);
            unavailableDatesMessage.setVisible(true);
        }
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
        DateRange range = dateRangePicker.getValue();
        if (range == null || range.getStartDate() == null || range.getEndDate() == null) {
            daysLabel.setText("Días: -");
            priceLabel.setText("Total estimado: -");
            return;
        }

        long days = ChronoUnit.DAYS.between(range.getStartDate(), range.getEndDate()) + 1;

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

            // Incluir cargo por salida del país en la previsualización
            BigDecimal cargoPreview = BigDecimal.ZERO;
            if (sacarPaisCheckbox != null && Boolean.TRUE.equals(sacarPaisCheckbox.getValue())
                    && diasFueraPaisField != null
                    && diasFueraPaisField.getValue() != null
                    && diasFueraPaisField.getValue() > 0
                    && tarifaSacarPais.compareTo(BigDecimal.ZERO) > 0) {
                cargoPreview = tarifaSacarPais.multiply(BigDecimal.valueOf(diasFueraPaisField.getValue()));
                total = total.add(cargoPreview);
                rateType += " + $" + cargoPreview + " (salida país)";
            }

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
