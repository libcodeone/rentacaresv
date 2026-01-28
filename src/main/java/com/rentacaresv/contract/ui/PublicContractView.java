package com.rentacaresv.contract.ui;

import com.rentacaresv.contract.application.ContractService;
import com.rentacaresv.contract.domain.*;
import com.rentacaresv.settings.application.SettingsCache;
import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.*;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Vista del contrato en formato similar al papel.
 * Usada por Admin/Empleado para llenar los datos del contrato.
 * El cliente solo firmará al final.
 * 
 * Accesible mediante: /contract/edit/{contractId} (requiere autenticación)
 * O para firma pública: /public/contract/{token}
 */
@Route("public/contract/:token")
@PageTitle("Contrato de Alquiler")
@AnonymousAllowed
@CssImport("./css/public-contract.css")
@Slf4j
public class PublicContractView extends VerticalLayout implements BeforeEnterObserver {

    private final ContractService contractService;
    private final SettingsCache settingsCache;

    private Contract contract;
    private String token;
    private boolean isReadOnly = false; // Si el contrato ya está firmado

    // Componentes del formulario
    private VerticalLayout mainContent;

    // Sección Cliente
    private TextField clientNameField;
    private ComboBox<DocumentType> documentTypeCombo;
    private TextField documentNumberField;
    private TextField passportField;
    private TextField licenseField;
    private TextField addressElSalvadorField;
    private TextField addressForeignField;
    private TextField phoneUsaField;
    private TextField phoneFamilyField;

    // Sección Vehículo
    private TextField vehiclePlateField;
    private TextField deliveryLocationField;
    private IntegerField totalDaysField;
    private TextField totalAmountField;
    private ComboBox<PaymentMethod> paymentMethodCombo;
    private TextField depositField;

    // Conductor adicional
    private TextField additionalDriverField;
    private TextField additionalDriverLicenseField;
    private TextField additionalDriverDuiField;

    // Deducibles
    private TextField accidentDeductibleField;
    private TextField theftDeductibleField;

    // Revisión física
    private IntegerField mileageOutField;
    private ComboBox<Integer> fuelLevelOutCombo;

    // Accesorios
    private Map<Long, Checkbox> accessoryCheckboxes = new HashMap<>();

    // Daños
    private List<ContractService.ContractDamageMarkDTO> damageMarks = new ArrayList<>();

    // Firma
    private String signatureBase64;

    // Observaciones
    private TextArea observationsField;

    public PublicContractView(ContractService contractService, SettingsCache settingsCache) {
        this.contractService = contractService;
        this.settingsCache = settingsCache;

        addClassName("public-contract-view");
        setSizeFull();
        setPadding(false);
        setSpacing(false);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        token = event.getRouteParameters().get("token").orElse(null);

        if (token == null || token.isEmpty()) {
            showError("Token inválido");
            return;
        }

        Optional<Contract> contractOpt = contractService.findByToken(token);

        if (contractOpt.isEmpty()) {
            showError("Contrato no encontrado");
            return;
        }

        contract = contractOpt.get();

        if (contract.getStatus() == ContractStatus.SIGNED) {
            isReadOnly = true;
            buildContractView();
            showAlreadySignedBanner();
            return;
        }

        if (contract.getStatus() == ContractStatus.EXPIRED || contract.isExpired()) {
            showExpired();
            return;
        }

        if (contract.getStatus() == ContractStatus.CANCELLED) {
            showCancelled();
            return;
        }

        buildContractView();
    }

    private void buildContractView() {
        removeAll();

        // Header con logo y título
        Div header = createHeader();

        // Contenido principal scrolleable
        mainContent = new VerticalLayout();
        mainContent.addClassName("contract-main-content");
        mainContent.setPadding(true);
        mainContent.setSpacing(true);
        mainContent.setWidthFull();
        mainContent.setMaxWidth("900px");
        mainContent.getStyle().set("margin", "0 auto");

        // Construir secciones del contrato
        mainContent.add(createClientSection());
        mainContent.add(createVehicleSection());
        mainContent.add(createPaymentSection());
        mainContent.add(createAccessoriesSection());
        mainContent.add(createPhysicalReviewSection());
        mainContent.add(createDamageSection());
        mainContent.add(createTermsSection());
        mainContent.add(createSignatureSection());

        // Pre-llenar datos existentes
        populateFormWithContractData();

        Scroller scroller = new Scroller(mainContent);
        scroller.setScrollDirection(Scroller.ScrollDirection.VERTICAL);
        scroller.setSizeFull();

        add(header, scroller);
        setFlexGrow(1, scroller);
    }

    private Div createHeader() {
        Div header = new Div();
        header.addClassName("contract-header-compact");
        header.setWidthFull();
        header.getStyle()
                .set("padding", "var(--lumo-space-m)")
                .set("background", "var(--lumo-contrast-5pct)")
                .set("border-bottom", "2px solid var(--lumo-primary-color)")
                .set("box-sizing", "border-box");

        // Contenedor interno con max-width para centrar el contenido
        Div headerInner = new Div();
        headerInner.getStyle()
                .set("max-width", "1200px")
                .set("margin", "0 auto")
                .set("width", "100%");

        HorizontalLayout headerContent = new HorizontalLayout();
        headerContent.setWidthFull();
        headerContent.setAlignItems(FlexComponent.Alignment.CENTER);
        headerContent.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        headerContent.getStyle()
                .set("flex-wrap", "wrap")
                .set("gap", "var(--lumo-space-m)");

        // Logo y nombre empresa
        HorizontalLayout leftSide = new HorizontalLayout();
        leftSide.setAlignItems(FlexComponent.Alignment.CENTER);
        leftSide.setSpacing(true);
        leftSide.getStyle()
                .set("flex-shrink", "0")
                .set("gap", "var(--lumo-space-s)");

        String logoUrl = settingsCache.getLogoUrl();
        if (logoUrl != null && !logoUrl.isEmpty()) {
            Image logo = new Image(logoUrl, "Logo");
            logo.setHeight("50px");
            logo.setWidth("50px");
            logo.getStyle()
                    .set("object-fit", "contain")
                    .set("flex-shrink", "0");
            leftSide.add(logo);
        }

        String companyName = settingsCache.getCompanyName();
        if (companyName == null || companyName.isEmpty()) {
            companyName = "RENT A CAR";
        }
        H2 title = new H2(companyName);
        title.getStyle()
                .set("margin", "0")
                .set("font-size", "1.5rem")
                .set("white-space", "nowrap");
        leftSide.add(title);

        // Info del contrato
        VerticalLayout rightSide = new VerticalLayout();
        rightSide.setPadding(false);
        rightSide.setSpacing(false);
        rightSide.setAlignItems(FlexComponent.Alignment.END);
        rightSide.getStyle()
                .set("min-width", "200px");

        Span contractNum = new Span("Contrato #: " + contract.getRental().getContractNumber());
        contractNum.getStyle()
                .set("font-weight", "bold")
                .set("font-size", "0.9rem");

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        Span dates = new Span("Período: " + contract.getRental().getStartDate().format(fmt) +
                " - " + contract.getRental().getEndDate().format(fmt));
        dates.getStyle()
                .set("font-size", "0.85rem")
                .set("color", "var(--lumo-secondary-text-color)");

        rightSide.add(contractNum, dates);

        headerContent.add(leftSide, rightSide);
        headerInner.add(headerContent);
        header.add(headerInner);

        return header;
    }

    // ========================================
    // SECCIÓN: Datos del Cliente
    // ========================================

    private Div createClientSection() {
        Div section = createSection("DATOS DEL ARRENDATARIO");

        var customer = contract.getRental().getCustomer();

        FormLayout form = new FormLayout();
        form.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("500px", 2),
                new FormLayout.ResponsiveStep("800px", 3));

        // Nombre completo
        clientNameField = new TextField("Nombre Completo");
        clientNameField.setValue(customer.getFullName());
        clientNameField.setReadOnly(isReadOnly);
        clientNameField.setWidthFull();
        form.add(clientNameField, 2);

        // Tipo de documento
        documentTypeCombo = new ComboBox<>("Tipo Documento");
        documentTypeCombo.setItems(DocumentType.values());
        documentTypeCombo.setItemLabelGenerator(DocumentType::getLabel);
        documentTypeCombo.setValue(mapCustomerDocType(customer.getDocumentType()));
        documentTypeCombo.setReadOnly(isReadOnly);
        form.add(documentTypeCombo);

        // Número DUI
        documentNumberField = new TextField("DUI");
        if (customer.getDocumentType() == com.rentacaresv.customer.domain.DocumentType.DUI) {
            documentNumberField.setValue(customer.getDocumentNumber() != null ? customer.getDocumentNumber() : "");
        }
        documentNumberField.setReadOnly(isReadOnly);
        form.add(documentNumberField);

        // Pasaporte
        passportField = new TextField("Pasaporte");
        if (customer.getDocumentType() == com.rentacaresv.customer.domain.DocumentType.PASSPORT) {
            passportField.setValue(customer.getDocumentNumber() != null ? customer.getDocumentNumber() : "");
        }
        passportField.setReadOnly(isReadOnly);
        form.add(passportField);

        // Licencia
        licenseField = new TextField("Licencia");
        licenseField.setValue(customer.getDriverLicenseNumber() != null ? customer.getDriverLicenseNumber() : "");
        licenseField.setReadOnly(isReadOnly);
        form.add(licenseField);

        // Dirección El Salvador
        addressElSalvadorField = new TextField("Dirección en El Salvador");
        addressElSalvadorField.setValue(customer.getAddress() != null ? customer.getAddress() : "");
        addressElSalvadorField.setReadOnly(isReadOnly);
        form.add(addressElSalvadorField, 2);

        // Dirección extranjero
        addressForeignField = new TextField("Dirección en el Extranjero");
        addressForeignField.setReadOnly(isReadOnly);
        form.add(addressForeignField, 2);

        // Teléfono USA
        phoneUsaField = new TextField("Tel. (USA)");
        phoneUsaField.setReadOnly(isReadOnly);
        form.add(phoneUsaField);

        // Teléfono familiar
        phoneFamilyField = new TextField("Tel. (Familiar)");
        phoneFamilyField.setValue(customer.getPhone() != null ? customer.getPhone() : "");
        phoneFamilyField.setReadOnly(isReadOnly);
        form.add(phoneFamilyField);

        section.add(form);
        return section;
    }

    private DocumentType mapCustomerDocType(com.rentacaresv.customer.domain.DocumentType type) {
        if (type == null)
            return DocumentType.DUI;
        return switch (type) {
            case DUI -> DocumentType.DUI;
            case PASSPORT -> DocumentType.PASSPORT;
            case LICENSE -> DocumentType.LICENSE;
            default -> DocumentType.DUI;
        };
    }

    // ========================================
    // SECCIÓN: Datos del Vehículo
    // ========================================

    private Div createVehicleSection() {
        Div section = createSection("DATOS DEL VEHÍCULO");

        var vehicle = contract.getRental().getVehicle();
        var rental = contract.getRental();

        FormLayout form = new FormLayout();
        form.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("500px", 2),
                new FormLayout.ResponsiveStep("800px", 4));

        // Vehículo (solo lectura)
        TextField vehicleField = new TextField("Vehículo");
        vehicleField.setValue(vehicle.getBrand() + " " + vehicle.getModel() + " " + vehicle.getYear());
        vehicleField.setReadOnly(true);
        form.add(vehicleField, 2);

        // Placa
        vehiclePlateField = new TextField("Placa");
        vehiclePlateField.setValue(vehicle.getLicensePlate());
        vehiclePlateField.setReadOnly(true);
        form.add(vehiclePlateField);

        // Color
        TextField colorField = new TextField("Color");
        colorField.setValue(vehicle.getColor() != null ? vehicle.getColor() : "");
        colorField.setReadOnly(true);
        form.add(colorField);

        // Lugar de entrega
        deliveryLocationField = new TextField("Lugar de Entrega");
        deliveryLocationField.setPlaceholder("Ej: Aeropuerto");
        deliveryLocationField.setReadOnly(isReadOnly);
        form.add(deliveryLocationField, 2);

        // Días totales
        totalDaysField = new IntegerField("Cantidad de Días");
        totalDaysField.setValue(rental.getTotalDays());
        totalDaysField.setReadOnly(true);
        form.add(totalDaysField);

        // Total
        totalAmountField = new TextField("Total $");
        totalAmountField.setValue(rental.getTotalAmount().toString());
        totalAmountField.setReadOnly(true);
        form.add(totalAmountField);

        section.add(form);
        return section;
    }

    // ========================================
    // SECCIÓN: Forma de Pago y Deducibles
    // ========================================

    private Div createPaymentSection() {
        Div section = createSection("FORMA DE PAGO Y GARANTÍAS");

        FormLayout form = new FormLayout();
        form.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("500px", 2),
                new FormLayout.ResponsiveStep("800px", 4));

        // Forma de pago
        paymentMethodCombo = new ComboBox<>("Forma de Pago");
        paymentMethodCombo.setItems(PaymentMethod.values());
        paymentMethodCombo.setItemLabelGenerator(PaymentMethod::getLabel);
        paymentMethodCombo.setReadOnly(isReadOnly);
        form.add(paymentMethodCombo);

        // Depósito
        depositField = new TextField("Depósito $");
        depositField.setReadOnly(isReadOnly);
        form.add(depositField);

        // Deducible por accidente
        accidentDeductibleField = new TextField("Deducible Accidente $");
        accidentDeductibleField.setReadOnly(isReadOnly);
        form.add(accidentDeductibleField);

        // Deducible por robo
        theftDeductibleField = new TextField("Deducible Robo $");
        theftDeductibleField.setReadOnly(isReadOnly);
        form.add(theftDeductibleField);

        // Separador
        Hr separator = new Hr();
        form.add(separator, 4);

        // Conductor adicional
        Span additionalTitle = new Span("Conductor Adicional (opcional)");
        additionalTitle.getStyle().set("font-weight", "bold");
        form.add(additionalTitle, 4);

        additionalDriverField = new TextField("Nombre");
        additionalDriverField.setReadOnly(isReadOnly);
        form.add(additionalDriverField, 2);

        additionalDriverLicenseField = new TextField("Licencia");
        additionalDriverLicenseField.setReadOnly(isReadOnly);
        form.add(additionalDriverLicenseField);

        additionalDriverDuiField = new TextField("DUI");
        additionalDriverDuiField.setReadOnly(isReadOnly);
        form.add(additionalDriverDuiField);

        section.add(form);
        return section;
    }

    // ========================================
    // SECCIÓN: Revisión de Accesorios
    // ========================================

    private Div createAccessoriesSection() {
        Div section = createSection("REVISIÓN DE ACCESORIOS");

        // Crear grid de checkboxes similar al papel
        Div accessoriesGrid = new Div();
        accessoriesGrid.addClassName("accessories-grid");
        accessoriesGrid.getStyle()
                .set("display", "grid")
                .set("grid-template-columns", "repeat(auto-fill, minmax(200px, 1fr))")
                .set("gap", "var(--lumo-space-s)");

        // Obtener accesorios del contrato y ordenarlos
        List<ContractAccessory> accessories = contract.getAccessories().stream()
                .sorted(Comparator.comparingInt(a -> a.getDisplayOrder() != null ? a.getDisplayOrder() : 0))
                .collect(Collectors.toList());

        for (ContractAccessory acc : accessories) {
            HorizontalLayout row = new HorizontalLayout();
            row.setAlignItems(FlexComponent.Alignment.CENTER);
            row.setPadding(false);
            row.setSpacing(true);

            Checkbox checkbox = new Checkbox(acc.getAccessoryName());
            checkbox.setValue(acc.getIsPresent());
            checkbox.setReadOnly(isReadOnly);
            accessoryCheckboxes.put(acc.getId(), checkbox);

            row.add(checkbox);
            accessoriesGrid.add(row);
        }

        section.add(accessoriesGrid);

        // Nota importante
        Div noteBox = new Div();
        noteBox.addClassName("note-box");
        noteBox.getStyle()
                .set("background", "var(--lumo-contrast-5pct)")
                .set("padding", "var(--lumo-space-m)")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("margin-top", "var(--lumo-space-m)")
                .set("font-size", "var(--lumo-font-size-s)");

        noteBox.add(new Html(
                "<div><strong>NOTA:</strong>" +
                        "<ul style='margin: var(--lumo-space-xs) 0; padding-left: var(--lumo-space-l);'>" +
                        "<li>Entregar lavado el vehículo, de lo contrario se cobrará extra <strong>$5.00</strong></li>"
                        +
                        "<li>Manchas o derrame de líquido en tapicería: <strong>$20.00</strong></li>" +
                        "<li>Cargo extra por salir del país sin autorización</li>" +
                        "</ul></div>"));

        section.add(noteBox);

        return section;
    }

    // ========================================
    // SECCIÓN: Revisión Física del Vehículo
    // ========================================

    private Div createPhysicalReviewSection() {
        Div section = createSection("REVISIÓN FÍSICA DEL VEHÍCULO");

        var vehicle = contract.getRental().getVehicle();

        FormLayout form = new FormLayout();
        form.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("500px", 2),
                new FormLayout.ResponsiveStep("800px", 4));

        // Kilometraje de salida
        mileageOutField = new IntegerField("Kilometraje de Salida");
        mileageOutField.setValue(vehicle.getMileage() != null ? vehicle.getMileage() : 0);
        mileageOutField.setMin(0);
        mileageOutField.setReadOnly(isReadOnly);
        form.add(mileageOutField);

        // Nivel de combustible de salida
        fuelLevelOutCombo = new ComboBox<>("Combustible Salida (%)");
        fuelLevelOutCombo.setItems(0, 25, 50, 75, 100);
        fuelLevelOutCombo.setValue(100);
        fuelLevelOutCombo.setReadOnly(isReadOnly);
        form.add(fuelLevelOutCombo);

        // Tipo de combustible
        TextField fuelTypeField = new TextField("Tipo Combustible");
        fuelTypeField.setValue(vehicle.getFuelType() != null ? vehicle.getFuelType().name() : "GASOLINA");
        fuelTypeField.setReadOnly(true);
        form.add(fuelTypeField);

        // Transmisión
        TextField transmissionField = new TextField("Transmisión");
        transmissionField.setValue(vehicle.getTransmissionType() != null ? vehicle.getTransmissionType().name() : "");
        transmissionField.setReadOnly(true);
        form.add(transmissionField);

        section.add(form);
        return section;
    }

    // ========================================
    // SECCIÓN: Diagrama de Daños
    // ========================================

    private Div createDamageSection() {
        Div section = createSection("ESTADO DEL VEHÍCULO - DIAGRAMA DE DAÑOS");

        Paragraph instruction = new Paragraph(
                "Haga clic en el diagrama para marcar los daños existentes del vehículo.");
        instruction.getStyle().set("color", "var(--lumo-secondary-text-color)");
        section.add(instruction);

        // Leyenda de tipos de daño
        Div legend = createDamageLegend();
        section.add(legend);

        // Diagrama
        var vehicle = contract.getRental().getVehicle();
        var vehicleType = vehicle.getVehicleType();

        if (vehicleType == null) {
            vehicleType = com.rentacaresv.vehicle.domain.VehicleType.SEDAN;
        }

        Optional<VehicleDiagram> diagramOpt = contractService.getDiagramForVehicleType(vehicleType);

        Div diagramContainer = new Div();
        diagramContainer.addClassName("damage-diagram-container");
        diagramContainer.setId("damage-diagram");
        diagramContainer.getStyle()
                .set("position", "relative")
                .set("width", "100%")
                .set("max-width", "800px")
                .set("margin", "0 auto")
                .set("background", "var(--lumo-contrast-5pct)")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("min-height", "300px");

        if (diagramOpt.isPresent()) {
            VehicleDiagram diagram = diagramOpt.get();
            String diagramUrl = diagram.getDiagramUrl();

            if (diagramUrl != null && !diagramUrl.isEmpty()) {
                // Usar imagen
                Image diagramImg = new Image(diagramUrl, "Diagrama del vehículo");
                diagramImg.setWidthFull();
                diagramImg.getStyle()
                        .set("display", "block")
                        .set("max-width", "100%")
                        .set("height", "auto");
                diagramContainer.add(diagramImg);
            } else if (diagram.getSvgContent() != null && !diagram.getSvgContent().isEmpty()) {
                // Fallback a SVG inline
                Html svg = new Html("<div class='svg-wrapper'>" + diagram.getSvgContent() + "</div>");
                diagramContainer.add(svg);
            } else {
                diagramContainer.add(new Paragraph("Imagen de diagrama no configurada para este tipo de vehículo"));
            }

            // Agregar listener para marcar daños (solo si no es readonly)
            if (!isReadOnly) {
                diagramContainer.getElement().executeJs(
                        "this.addEventListener('click', (e) => {" +
                                "  const rect = this.getBoundingClientRect();" +
                                "  const x = ((e.clientX - rect.left) / rect.width * 100).toFixed(2);" +
                                "  const y = ((e.clientY - rect.top) / rect.height * 100).toFixed(2);" +
                                "  $0.$server.addDamageMark(x, y);" +
                                "});",
                        getElement());
            }
        } else {
            diagramContainer.add(new Paragraph("Diagrama no disponible para este tipo de vehículo. " +
                    "Configure uno en Catálogo de Contratos."));
        }

        section.add(diagramContainer);

        // Lista de daños marcados
        Div damageList = new Div();
        damageList.setId("damage-list");
        damageList.addClassName("damage-list");
        updateDamageList(damageList);
        section.add(damageList);

        return section;
    }

    @ClientCallable
    public void addDamageMark(String x, String y) {
        if (isReadOnly)
            return;

        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Agregar Daño");

        ComboBox<DamageType> typeCombo = new ComboBox<>("Tipo de Daño");
        typeCombo.setItems(DamageType.values());
        typeCombo.setItemLabelGenerator(DamageType::getLabel);
        typeCombo.setWidthFull();

        TextArea descArea = new TextArea("Descripción");
        descArea.setWidthFull();

        Button saveBtn = new Button("Guardar", e -> {
            if (typeCombo.getValue() == null) {
                Notification.show("Seleccione un tipo de daño", 2000, Notification.Position.MIDDLE);
                return;
            }

            ContractService.ContractDamageMarkDTO mark = new ContractService.ContractDamageMarkDTO();
            mark.setPositionX(new BigDecimal(x));
            mark.setPositionY(new BigDecimal(y));
            mark.setDamageType(typeCombo.getValue());
            mark.setDescription(descArea.getValue());
            mark.setSeverity(1);

            damageMarks.add(mark);

            Div damageList = (Div) mainContent.getChildren()
                    .flatMap(c -> {
                        if (c instanceof Div div) {
                            return div.getChildren();
                        }
                        return java.util.stream.Stream.empty();
                    })
                    .filter(c -> c.getId().orElse("").equals("damage-list"))
                    .findFirst().orElse(null);

            if (damageList != null) {
                updateDamageList(damageList);
            }

            addVisualMarker(x, y, typeCombo.getValue());
            dialog.close();
        });
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancelBtn = new Button("Cancelar", e -> dialog.close());

        HorizontalLayout btns = new HorizontalLayout(saveBtn, cancelBtn);
        dialog.add(typeCombo, descArea, btns);
        dialog.open();
    }

    private void addVisualMarker(String x, String y, DamageType type) {
        UI.getCurrent().getPage().executeJs(
                "const container = document.getElementById('damage-diagram');" +
                        "const marker = document.createElement('div');" +
                        "marker.className = 'damage-marker';" +
                        "marker.style.left = $0 + '%';" +
                        "marker.style.top = $1 + '%';" +
                        "marker.style.backgroundColor = $2;" +
                        "marker.textContent = $3;" +
                        "marker.title = $4;" +
                        "container.appendChild(marker);",
                x, y, type.getColor(), type.getSymbol(), type.getLabel());
    }

    private void updateDamageList(Div container) {
        container.removeAll();

        if (damageMarks.isEmpty()) {
            container.add(new Paragraph("No se han marcado daños."));
            return;
        }

        H4 listTitle = new H4("Daños marcados (" + damageMarks.size() + "):");
        container.add(listTitle);

        for (int i = 0; i < damageMarks.size(); i++) {
            var mark = damageMarks.get(i);
            int index = i;

            HorizontalLayout row = new HorizontalLayout();
            row.setAlignItems(FlexComponent.Alignment.CENTER);

            Span typeSpan = new Span(mark.getDamageType().getLabel());
            typeSpan.getStyle().set("color", mark.getDamageType().getColor()).set("font-weight", "bold");

            Span descSpan = new Span(mark.getDescription() != null ? mark.getDescription() : "Sin descripción");

            if (!isReadOnly) {
                Button removeBtn = new Button(VaadinIcon.TRASH.create());
                removeBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR,
                        ButtonVariant.LUMO_TERTIARY);
                removeBtn.addClickListener(e -> {
                    damageMarks.remove(index);
                    updateDamageList(container);
                });
                row.add(typeSpan, descSpan, removeBtn);
            } else {
                row.add(typeSpan, descSpan);
            }

            container.add(row);
        }
    }

    private Div createDamageLegend() {
        Div legend = new Div();
        legend.addClassName("damage-legend");

        for (DamageType type : DamageType.values()) {
            Span item = new Span();
            item.addClassName("legend-item");

            Span color = new Span(type.getSymbol());
            color.addClassName("legend-color");
            color.getStyle().set("background-color", type.getColor());

            Span label = new Span(type.getLabel());
            item.add(color, label);
            legend.add(item);
        }

        return legend;
    }

    // ========================================
    // SECCIÓN: Términos y Condiciones
    // ========================================

    private Div createTermsSection() {
        Div section = createSection("TÉRMINOS Y CONDICIONES");

        Div terms = new Div();
        terms.addClassName("contract-terms");
        terms.add(new Html(
                "<div style='font-size: var(--lumo-font-size-s);'>" +
                        "<ul>" +
                        "<li>El arrendatario se compromete a devolver el vehículo en las mismas condiciones en que lo recibió.</li>"
                        +
                        "<li>Entregar lavado el vehículo, de lo contrario se cobrará extra $5.00</li>" +
                        "<li>Manchas o derrame de líquido en tapicería: $20.00</li>" +
                        "<li>El arrendatario es responsable de cualquier daño o pérdida del vehículo durante el período de renta.</li>"
                        +
                        "<li>En caso de accidente, el arrendatario participará con el deducible establecido en este contrato.</li>"
                        +
                        "<li>En caso de robo, el arrendatario participará con el deducible por robo establecido.</li>" +
                        "<li>El vehículo no puede salir del país sin autorización previa por escrito.</li>" +
                        "<li>Está prohibido fumar dentro del vehículo.</li>" +
                        "</ul>" +
                        "</div>"));

        section.add(terms);

        // Observaciones
        observationsField = new TextArea("Observaciones");
        observationsField.setWidthFull();
        observationsField.setMinHeight("100px");
        observationsField.setReadOnly(isReadOnly);
        section.add(observationsField);

        return section;
    }

    // ========================================
    // SECCIÓN: Firma
    // ========================================

    private Div createSignatureSection() {
        Div section = createSection("FIRMAS");

        HorizontalLayout signaturesRow = new HorizontalLayout();
        signaturesRow.setWidthFull();
        signaturesRow.setSpacing(true);

        // Firma del cliente
        VerticalLayout clientSignature = new VerticalLayout();
        clientSignature.setAlignItems(FlexComponent.Alignment.CENTER);
        clientSignature.setWidth("50%");
        clientSignature.setPadding(false);

        Paragraph clientLabel = new Paragraph("FIRMA DEL CLIENTE");
        clientLabel.getStyle().set("font-weight", "bold");

        if (isReadOnly && contract.getSignatureUrl() != null) {
            Image signatureImg = new Image(contract.getSignatureUrl(), "Firma");
            signatureImg.setMaxWidth("300px");
            signatureImg.setMaxHeight("150px");
            clientSignature.add(clientLabel, signatureImg);
        } else if (!isReadOnly) {
            Div signatureContainer = new Div();
            signatureContainer.setId("signature-container");
            signatureContainer.addClassName("signature-box");

            Html canvas = new Html(
                    "<canvas id='signature-canvas' width='400' height='150' " +
                            "style='border: 2px solid #ccc; border-radius: 8px; background: white; touch-action: none;'></canvas>");
            signatureContainer.add(canvas);

            Button clearBtn = new Button("Limpiar", VaadinIcon.ERASER.create());
            clearBtn.addThemeVariants(ButtonVariant.LUMO_SMALL);
            clearBtn.addClickListener(e -> {
                UI.getCurrent().getPage().executeJs(
                        "const canvas = document.getElementById('signature-canvas');" +
                                "const ctx = canvas.getContext('2d');" +
                                "ctx.clearRect(0, 0, canvas.width, canvas.height);");
                signatureBase64 = null;
            });

            // Inicializar canvas
            UI.getCurrent().getPage().executeJs(
                    "setTimeout(() => {" +
                            "  const canvas = document.getElementById('signature-canvas');" +
                            "  if (!canvas) return;" +
                            "  const ctx = canvas.getContext('2d');" +
                            "  let drawing = false;" +
                            "  ctx.strokeStyle = '#000';" +
                            "  ctx.lineWidth = 2;" +
                            "  ctx.lineCap = 'round';" +
                            "  function getPos(e) {" +
                            "    const rect = canvas.getBoundingClientRect();" +
                            "    const clientX = e.touches ? e.touches[0].clientX : e.clientX;" +
                            "    const clientY = e.touches ? e.touches[0].clientY : e.clientY;" +
                            "    return { x: clientX - rect.left, y: clientY - rect.top };" +
                            "  }" +
                            "  canvas.addEventListener('mousedown', (e) => { drawing = true; ctx.beginPath(); ctx.moveTo(getPos(e).x, getPos(e).y); });"
                            +
                            "  canvas.addEventListener('mousemove', (e) => { if (drawing) { ctx.lineTo(getPos(e).x, getPos(e).y); ctx.stroke(); } });"
                            +
                            "  canvas.addEventListener('mouseup', () => { drawing = false; });" +
                            "  canvas.addEventListener('mouseout', () => { drawing = false; });" +
                            "  canvas.addEventListener('touchstart', (e) => { e.preventDefault(); drawing = true; ctx.beginPath(); ctx.moveTo(getPos(e).x, getPos(e).y); });"
                            +
                            "  canvas.addEventListener('touchmove', (e) => { e.preventDefault(); if (drawing) { ctx.lineTo(getPos(e).x, getPos(e).y); ctx.stroke(); } });"
                            +
                            "  canvas.addEventListener('touchend', () => { drawing = false; });" +
                            "}, 500);");

            clientSignature.add(clientLabel, signatureContainer, clearBtn);
        } else {
            Div placeholder = new Div();
            placeholder.getStyle()
                    .set("width", "300px")
                    .set("height", "100px")
                    .set("border-bottom", "2px solid black");
            clientSignature.add(clientLabel, placeholder);
        }

        // Firma del empleado (placeholder)
        VerticalLayout employeeSignature = new VerticalLayout();
        employeeSignature.setAlignItems(FlexComponent.Alignment.CENTER);
        employeeSignature.setWidth("50%");
        employeeSignature.setPadding(false);

        Paragraph employeeLabel = new Paragraph("FIRMA DEL EMPLEADO");
        employeeLabel.getStyle().set("font-weight", "bold");

        Div employeePlaceholder = new Div();
        employeePlaceholder.getStyle()
                .set("width", "300px")
                .set("height", "100px")
                .set("border-bottom", "2px solid black");

        employeeSignature.add(employeeLabel, employeePlaceholder);

        signaturesRow.add(clientSignature, employeeSignature);
        section.add(signaturesRow);

        // Checkbox de aceptación y botón de firma (solo si no está firmado)
        if (!isReadOnly) {
            Hr separator = new Hr();
            section.add(separator);

            Checkbox acceptTerms = new Checkbox("He leído y acepto los términos y condiciones del contrato");

            Button signBtn = new Button("Firmar Contrato", VaadinIcon.CHECK.create());
            signBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_LARGE);
            signBtn.addClickListener(e -> {
                if (!acceptTerms.getValue()) {
                    Notification.show("Debe aceptar los términos y condiciones", 3000, Notification.Position.MIDDLE)
                            .addThemeVariants(NotificationVariant.LUMO_ERROR);
                    return;
                }

                UI.getCurrent().getPage().executeJs(
                        "return document.getElementById('signature-canvas').toDataURL('image/png');")
                        .then(String.class, dataUrl -> {
                            if (dataUrl == null || dataUrl.length() < 1000) {
                                Notification.show("Por favor, dibuje su firma", 3000, Notification.Position.MIDDLE)
                                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                                return;
                            }

                            signatureBase64 = dataUrl;
                            submitContract();
                        });
            });

            HorizontalLayout signRow = new HorizontalLayout(acceptTerms, signBtn);
            signRow.setWidthFull();
            signRow.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
            signRow.setAlignItems(FlexComponent.Alignment.CENTER);

            section.add(signRow);
        }

        return section;
    }

    // ========================================
    // Métodos auxiliares
    // ========================================

    private Div createSection(String title) {
        Div section = new Div();
        section.addClassName("contract-section");
        section.getStyle()
                .set("background", "white")
                .set("border", "1px solid var(--lumo-contrast-20pct)")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("padding", "var(--lumo-space-m)")
                .set("margin-bottom", "var(--lumo-space-m)");

        H3 sectionTitle = new H3(title);
        sectionTitle.getStyle()
                .set("margin", "0 0 var(--lumo-space-m) 0")
                .set("padding-bottom", "var(--lumo-space-s)")
                .set("border-bottom", "2px solid var(--lumo-primary-color)")
                .set("color", "var(--lumo-primary-color)")
                .set("font-size", "var(--lumo-font-size-l)");

        section.add(sectionTitle);
        return section;
    }

    private void populateFormWithContractData() {
        // Pre-llenar con datos guardados en el contrato (si existen)
        if (contract.getAddressElSalvador() != null) {
            addressElSalvadorField.setValue(contract.getAddressElSalvador());
        }
        if (contract.getAddressForeign() != null) {
            addressForeignField.setValue(contract.getAddressForeign());
        }
        if (contract.getPhoneUsa() != null) {
            phoneUsaField.setValue(contract.getPhoneUsa());
        }
        if (contract.getPhoneFamily() != null) {
            phoneFamilyField.setValue(contract.getPhoneFamily());
        }
        if (contract.getDeliveryLocation() != null) {
            deliveryLocationField.setValue(contract.getDeliveryLocation());
        }
        if (contract.getPaymentMethod() != null) {
            paymentMethodCombo.setValue(contract.getPaymentMethod());
        }
        if (contract.getDepositAmount() != null) {
            depositField.setValue(contract.getDepositAmount().toString());
        }
        if (contract.getAccidentDeductible() != null) {
            accidentDeductibleField.setValue(contract.getAccidentDeductible().toString());
        }
        if (contract.getTheftDeductible() != null) {
            theftDeductibleField.setValue(contract.getTheftDeductible().toString());
        }
        if (contract.getAdditionalDriverName() != null) {
            additionalDriverField.setValue(contract.getAdditionalDriverName());
        }
        if (contract.getAdditionalDriverLicense() != null) {
            additionalDriverLicenseField.setValue(contract.getAdditionalDriverLicense());
        }
        if (contract.getAdditionalDriverDui() != null) {
            additionalDriverDuiField.setValue(contract.getAdditionalDriverDui());
        }
        if (contract.getMileageOut() != null) {
            mileageOutField.setValue(contract.getMileageOut());
        }
        if (contract.getFuelLevelOut() != null) {
            fuelLevelOutCombo.setValue(contract.getFuelLevelOut());
        }
        if (contract.getObservations() != null) {
            observationsField.setValue(contract.getObservations());
        }
        if (contract.getDocumentType() != null) {
            documentTypeCombo.setValue(contract.getDocumentType());
        }
        if (contract.getDocumentNumber() != null) {
            documentNumberField.setValue(contract.getDocumentNumber());
        }

        // Cargar daños existentes
        if (contract.getDamageMarks() != null && !contract.getDamageMarks().isEmpty()) {
            for (ContractDamageMark mark : contract.getDamageMarks()) {
                ContractService.ContractDamageMarkDTO dto = new ContractService.ContractDamageMarkDTO();
                dto.setPositionX(mark.getPositionX());
                dto.setPositionY(mark.getPositionY());
                dto.setDamageType(mark.getDamageType());
                dto.setDescription(mark.getDescription());
                dto.setSeverity(mark.getSeverity());
                damageMarks.add(dto);
            }
        }
    }

    private void submitContract() {
        try {
            // Mostrar loading
            Dialog loadingDialog = new Dialog();
            loadingDialog.setCloseOnEsc(false);
            loadingDialog.setCloseOnOutsideClick(false);
            loadingDialog.add(new Paragraph("Procesando contrato..."));
            loadingDialog.open();

            // Guardar todos los datos del formulario
            saveFormData();

            // Firmar
            String ipAddress = VaadinRequest.getCurrent().getRemoteAddr();
            String userAgent = VaadinRequest.getCurrent().getHeader("User-Agent");

            contractService.signContract(token, signatureBase64, ipAddress, userAgent);

            loadingDialog.close();

            // Mostrar éxito
            showSuccess();

        } catch (Exception e) {
            log.error("Error al firmar contrato: {}", e.getMessage(), e);
            Notification.show("Error: " + e.getMessage(), 5000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void saveFormData() {
        // Actualizar documento
        contractService.updateDocumentInfo(token,
                documentTypeCombo.getValue(),
                documentNumberField.getValue(),
                null, null); // No subimos fotos en esta versión simplificada

        // Actualizar info del vehículo
        contractService.updateVehicleInfo(token,
                mileageOutField.getValue(),
                fuelLevelOutCombo.getValue());

        // Actualizar accesorios
        List<ContractService.ContractAccessoryDTO> accessoryDTOs = new ArrayList<>();
        for (var entry : accessoryCheckboxes.entrySet()) {
            ContractService.ContractAccessoryDTO dto = new ContractService.ContractAccessoryDTO();
            dto.setId(entry.getKey());
            dto.setIsPresent(entry.getValue().getValue());
            accessoryDTOs.add(dto);
        }
        contractService.updateAccessories(token, accessoryDTOs);

        // Actualizar daños
        contractService.updateDamageMarks(token, damageMarks);

        // Actualizar campos adicionales
        contractService.updateAdditionalInfo(token,
                deliveryLocationField.getValue(),
                addressElSalvadorField.getValue(),
                addressForeignField.getValue(),
                phoneUsaField.getValue(),
                phoneFamilyField.getValue(),
                paymentMethodCombo.getValue(),
                parseDecimal(depositField.getValue()),
                parseDecimal(accidentDeductibleField.getValue()),
                parseDecimal(theftDeductibleField.getValue()),
                additionalDriverField.getValue(),
                additionalDriverLicenseField.getValue(),
                additionalDriverDuiField.getValue(),
                observationsField.getValue());
    }

    private BigDecimal parseDecimal(String value) {
        if (value == null || value.isEmpty())
            return null;
        try {
            return new BigDecimal(value.replace(",", ""));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // ========================================
    // Estados finales
    // ========================================

    private void showError(String message) {
        removeAll();
        Div container = new Div();
        container.addClassName("error-container");
        container.add(VaadinIcon.EXCLAMATION_CIRCLE.create(), new H2("Error"), new Paragraph(message));
        add(container);
    }

    private void showAlreadySignedBanner() {
        Notification.show("Este contrato ya fue firmado el " +
                contract.getSignedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),
                5000, Notification.Position.TOP_CENTER)
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }

    private void showExpired() {
        removeAll();
        Div container = new Div();
        container.addClassName("error-container");
        container.add(VaadinIcon.CLOCK.create(), new H2("Contrato expirado"),
                new Paragraph("El enlace de este contrato ha expirado."));
        add(container);
    }

    private void showCancelled() {
        removeAll();
        Div container = new Div();
        container.addClassName("error-container");
        container.add(VaadinIcon.CLOSE_CIRCLE.create(), new H2("Contrato cancelado"),
                new Paragraph("Este contrato ha sido cancelado."));
        add(container);
    }

    private void showSuccess() {
        removeAll();
        Div container = new Div();
        container.addClassName("success-container");
        container.add(
                VaadinIcon.CHECK_CIRCLE.create(),
                new H2("¡Contrato firmado exitosamente!"),
                new Paragraph("Gracias por completar el contrato."),
                new Paragraph("Recibirá una copia por correo electrónico."));
        add(container);
    }
}
