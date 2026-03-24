package com.rentacaresv.contract.ui;

import com.rentacaresv.contract.application.ContractService;
import com.rentacaresv.contract.domain.*;
import com.rentacaresv.settings.application.SettingsCache;

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
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.router.*;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.auth.AnonymousAllowed;

import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Vista del contrato en formato similar al papel.
 * Incluye:
 * - Datos del cliente
 * - Fotos de documentos (licencia frente/reverso, DUI/pasaporte frente/reverso)
 * - Datos del vehículo
 * - Revisión de accesorios con selección rápida
 * - Video del estado del vehículo
 * - Términos y condiciones
 * - Dos firmas (cliente y empleado) con validación de canvas
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
    private boolean isReadOnly = false;

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

    // Videos del vehículo (3 videos)
    private String vehicleExteriorVideoUrl;
    private String vehicleInteriorVideoUrl;
    private String vehicleDetailsVideoUrl;

    // Firmas (cliente y empleado)
    private String clientSignatureBase64;
    private String employeeSignatureBase64;
    private TextField employeeNameField;

    // Documentos subidos (base64)
    private String licenseFrontBase64;
    private String licenseBackBase64;
    private String documentFrontBase64;
    private String documentBackBase64;

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

        Div header = createHeader();

        mainContent = new VerticalLayout();
        mainContent.addClassName("contract-main-content");
        mainContent.setPadding(true);
        mainContent.setSpacing(true);
        mainContent.setWidthFull();
        mainContent.setMaxWidth("900px");
        mainContent.getStyle().set("margin", "0 auto");

        mainContent.add(createClientSection());
        mainContent.add(createDocumentsSection());
        mainContent.add(createVehicleSection());
        mainContent.add(createPaymentSection());
        mainContent.add(createAccessoriesSection());
        mainContent.add(createPhysicalReviewSection());
        mainContent.add(createVideoSection());
        mainContent.add(createTermsSection());
        mainContent.add(createSignatureSection());

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

        // Lado izquierdo: Logo + Nombre
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

        // Lado derecho: # Contrato + Período (usando Div para mejor control)
        Div rightSide = new Div();
        rightSide.getStyle()
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("align-items", "flex-end")
                .set("justify-content", "center")
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

        clientNameField = new TextField("Nombre Completo");
        clientNameField.setValue(customer.getFullName());
        clientNameField.setReadOnly(isReadOnly);
        clientNameField.setWidthFull();
        form.add(clientNameField, 2);

        documentTypeCombo = new ComboBox<>("Tipo Documento");
        documentTypeCombo.setItems(DocumentType.values());
        documentTypeCombo.setItemLabelGenerator(DocumentType::getLabel);
        documentTypeCombo.setValue(mapCustomerDocType(customer.getDocumentType()));
        documentTypeCombo.setReadOnly(isReadOnly);
        form.add(documentTypeCombo);

        documentNumberField = new TextField("DUI");
        if (customer.getDocumentType() == com.rentacaresv.customer.domain.DocumentType.DUI) {
            documentNumberField.setValue(customer.getDocumentNumber() != null ? customer.getDocumentNumber() : "");
        }
        documentNumberField.setReadOnly(isReadOnly);
        form.add(documentNumberField);

        passportField = new TextField("Pasaporte");
        if (customer.getDocumentType() == com.rentacaresv.customer.domain.DocumentType.PASSPORT) {
            passportField.setValue(customer.getDocumentNumber() != null ? customer.getDocumentNumber() : "");
        }
        passportField.setReadOnly(isReadOnly);
        form.add(passportField);

        licenseField = new TextField("Licencia");
        licenseField.setValue(customer.getDriverLicenseNumber() != null ? customer.getDriverLicenseNumber() : "");
        licenseField.setReadOnly(isReadOnly);
        form.add(licenseField);

        addressElSalvadorField = new TextField("Dirección en El Salvador");
        addressElSalvadorField.setValue(customer.getAddress() != null ? customer.getAddress() : "");
        addressElSalvadorField.setReadOnly(isReadOnly);
        form.add(addressElSalvadorField, 2);

        addressForeignField = new TextField("Dirección en el Extranjero");
        addressForeignField.setValue(customer.getAddressForeign() != null ? customer.getAddressForeign() : "");
        addressForeignField.setReadOnly(isReadOnly);
        form.add(addressForeignField, 2);

        phoneUsaField = new TextField("Tel. (USA)");
        phoneUsaField.setReadOnly(isReadOnly);
        form.add(phoneUsaField);

        phoneFamilyField = new TextField("Teléfono");
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
    // SECCIÓN: Documentos del Cliente
    // ========================================

    private Div createDocumentsSection() {
        Div section = createSection("DOCUMENTOS DEL CLIENTE");

        Paragraph instruction = new Paragraph(
                "Suba fotos de los documentos del cliente (licencia de conducir y documento de identidad/pasaporte).");
        instruction.getStyle().set("color", "var(--lumo-secondary-text-color)");
        section.add(instruction);

        if (isReadOnly) {
            HorizontalLayout imagesRow = new HorizontalLayout();
            imagesRow.setWidthFull();
            imagesRow.setSpacing(true);
            imagesRow.getStyle().set("flex-wrap", "wrap");

            if (contract.getLicenseFrontUrl() != null) {
                imagesRow.add(createDocumentPreview("Licencia (Frente)", contract.getLicenseFrontUrl()));
            }
            if (contract.getLicenseBackUrl() != null) {
                imagesRow.add(createDocumentPreview("Licencia (Reverso)", contract.getLicenseBackUrl()));
            }
            if (contract.getDocumentFrontUrl() != null) {
                imagesRow.add(createDocumentPreview("Documento ID (Frente)", contract.getDocumentFrontUrl()));
            }
            if (contract.getDocumentBackUrl() != null) {
                imagesRow.add(createDocumentPreview("Documento ID (Reverso)", contract.getDocumentBackUrl()));
            }

            if (imagesRow.getComponentCount() == 0) {
                section.add(new Paragraph("No se subieron documentos."));
            } else {
                section.add(imagesRow);
            }
        } else {
            Div uploadsGrid = new Div();
            uploadsGrid.getStyle()
                    .set("display", "grid")
                    .set("grid-template-columns", "repeat(auto-fill, minmax(200px, 1fr))")
                    .set("gap", "var(--lumo-space-m)");

            uploadsGrid.add(createDocumentUpload("Licencia (Frente)", "license-front"));
            uploadsGrid.add(createDocumentUpload("Licencia (Reverso)", "license-back"));
            uploadsGrid.add(createDocumentUpload("DUI/Pasaporte (Frente)", "doc-front"));
            uploadsGrid.add(createDocumentUpload("DUI/Pasaporte (Reverso)", "doc-back"));

            section.add(uploadsGrid);

            Div noteBox = new Div();
            noteBox.getStyle()
                    .set("background", "var(--lumo-contrast-5pct)")
                    .set("padding", "var(--lumo-space-s)")
                    .set("border-radius", "var(--lumo-border-radius-m)")
                    .set("margin-top", "var(--lumo-space-m)")
                    .set("font-size", "var(--lumo-font-size-s)");
            noteBox.add(new Html(
                    "<div><strong>Nota:</strong> Si el cliente no es salvadoreño, debe subir foto de su pasaporte.</div>"));
            section.add(noteBox);
        }

        return section;
    }

    private VerticalLayout createDocumentPreview(String label, String imageUrl) {
        VerticalLayout container = new VerticalLayout();
        container.setPadding(false);
        container.setSpacing(false);
        container.setAlignItems(FlexComponent.Alignment.CENTER);
        container.setWidth("200px");

        Span labelSpan = new Span(label);
        labelSpan.getStyle().set("font-weight", "bold").set("font-size", "var(--lumo-font-size-s)");

        Image img = new Image(imageUrl, label);
        img.setMaxWidth("180px");
        img.setMaxHeight("120px");
        img.getStyle()
                .set("object-fit", "contain")
                .set("border", "1px solid var(--lumo-contrast-20pct)")
                .set("border-radius", "var(--lumo-border-radius-m)");

        container.add(labelSpan, img);
        return container;
    }

    private VerticalLayout createDocumentUpload(String label, String fieldId) {
        VerticalLayout container = new VerticalLayout();
        container.setPadding(false);
        container.setSpacing(true);
        container.setAlignItems(FlexComponent.Alignment.CENTER);

        Span labelSpan = new Span(label);
        labelSpan.getStyle().set("font-weight", "bold").set("font-size", "var(--lumo-font-size-s)");

        Div previewContainer = new Div();
        previewContainer.setId("preview-" + fieldId);
        previewContainer.getStyle()
                .set("width", "180px")
                .set("height", "120px")
                .set("border", "2px dashed var(--lumo-contrast-30pct)")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("display", "flex")
                .set("align-items", "center")
                .set("justify-content", "center")
                .set("background", "var(--lumo-contrast-5pct)")
                .set("overflow", "hidden");

        Span placeholder = new Span("Sin imagen");
        placeholder.getStyle().set("color", "var(--lumo-tertiary-text-color)");
        previewContainer.add(placeholder);

        MemoryBuffer buffer = new MemoryBuffer();
        Upload upload = new Upload(buffer);
        upload.setAcceptedFileTypes("image/png", "image/jpeg", "image/jpg");
        upload.setMaxFiles(1);
        upload.setMaxFileSize(5 * 1024 * 1024);

        Button uploadButton = new Button("Subir foto", VaadinIcon.UPLOAD.create());
        uploadButton.addThemeVariants(ButtonVariant.LUMO_SMALL);
        upload.setUploadButton(uploadButton);
        upload.setDropAllowed(false);

        upload.addSucceededListener(event -> {
            try {
                InputStream inputStream = buffer.getInputStream();
                byte[] bytes = inputStream.readAllBytes();
                String base64 = "data:" + event.getMIMEType() + ";base64," + Base64.getEncoder().encodeToString(bytes);

                switch (fieldId) {
                    case "license-front" -> licenseFrontBase64 = base64;
                    case "license-back" -> licenseBackBase64 = base64;
                    case "doc-front" -> documentFrontBase64 = base64;
                    case "doc-back" -> documentBackBase64 = base64;
                }

                previewContainer.removeAll();
                Image preview = new Image(base64, label);
                preview.setMaxWidth("176px");
                preview.setMaxHeight("116px");
                preview.getStyle().set("object-fit", "contain");
                previewContainer.add(preview);

                Notification.show("Imagen cargada: " + label, 2000, Notification.Position.BOTTOM_CENTER);

            } catch (Exception e) {
                log.error("Error cargando imagen: {}", e.getMessage());
                Notification.show("Error al cargar imagen", 3000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });

        container.add(labelSpan, previewContainer, upload);
        return container;
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

        TextField vehicleField = new TextField("Vehículo");
        vehicleField.setValue(vehicle.getBrand() + " " + vehicle.getModel() + " " + vehicle.getYear());
        vehicleField.setReadOnly(true);
        form.add(vehicleField, 2);

        vehiclePlateField = new TextField("Placa");
        vehiclePlateField.setValue(vehicle.getLicensePlate());
        vehiclePlateField.setReadOnly(true);
        form.add(vehiclePlateField);

        TextField colorField = new TextField("Color");
        colorField.setValue(vehicle.getColor() != null ? vehicle.getColor() : "");
        colorField.setReadOnly(true);
        form.add(colorField);

        deliveryLocationField = new TextField("Lugar de Entrega");
        deliveryLocationField.setPlaceholder("Ej: Aeropuerto");
        deliveryLocationField.setReadOnly(isReadOnly);
        form.add(deliveryLocationField, 2);

        totalDaysField = new IntegerField("Cantidad de Días");
        totalDaysField.setValue(rental.getTotalDays());
        totalDaysField.setReadOnly(true);
        form.add(totalDaysField);

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

        var vehicle = contract.getRental().getVehicle();

        FormLayout form = new FormLayout();
        form.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("500px", 2),
                new FormLayout.ResponsiveStep("800px", 4));

        paymentMethodCombo = new ComboBox<>("Forma de Pago");
        paymentMethodCombo.setItems(PaymentMethod.values());
        paymentMethodCombo.setItemLabelGenerator(PaymentMethod::getLabel);
        paymentMethodCombo.setReadOnly(isReadOnly);
        form.add(paymentMethodCombo);

        depositField = new TextField("Depósito $");
        depositField.setReadOnly(isReadOnly);
        form.add(depositField);

        accidentDeductibleField = new TextField("Deducible Accidente $");
        accidentDeductibleField.setReadOnly(isReadOnly);
        form.add(accidentDeductibleField);

        Integer theftPercentage = vehicle.getTheftDeductiblePercentage();
        theftDeductibleField = new TextField("Deducible Robo");
        theftDeductibleField.setValue(theftPercentage + "% del valor del vehículo");
        theftDeductibleField.setReadOnly(true);
        theftDeductibleField.setHelperText("Según configuración del vehículo");
        form.add(theftDeductibleField);

        Hr separator = new Hr();
        form.add(separator, 4);

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

        if (!isReadOnly) {
            HorizontalLayout quickActions = new HorizontalLayout();
            quickActions.setSpacing(true);
            quickActions.getStyle().set("margin-bottom", "var(--lumo-space-m)");

            Button selectAllBtn = new Button("Seleccionar Todos", VaadinIcon.CHECK_SQUARE.create());
            selectAllBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_PRIMARY);
            selectAllBtn.addClickListener(e -> {
                accessoryCheckboxes.values().forEach(cb -> cb.setValue(true));
                Notification.show("Todos los accesorios seleccionados", 2000, Notification.Position.BOTTOM_CENTER);
            });

            Button selectNoneBtn = new Button("Deseleccionar Todos", VaadinIcon.THIN_SQUARE.create());
            selectNoneBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
            selectNoneBtn.addClickListener(e -> {
                accessoryCheckboxes.values().forEach(cb -> cb.setValue(false));
                Notification.show("Todos los accesorios deseleccionados", 2000, Notification.Position.BOTTOM_CENTER);
            });

            quickActions.add(selectAllBtn, selectNoneBtn);
            section.add(quickActions);
        }

        Div accessoriesGrid = new Div();
        accessoriesGrid.addClassName("accessories-grid");
        accessoriesGrid.getStyle()
                .set("display", "grid")
                .set("grid-template-columns", "repeat(auto-fill, minmax(200px, 1fr))")
                .set("gap", "var(--lumo-space-s)");

        // MODO EDICIÓN: Cargar TODOS los accesorios del catálogo
        if (!isReadOnly) {
            // Obtener todos los accesorios del catálogo activos
            List<AccessoryCatalog> catalogAccessories = contractService.getActiveAccessories();
            
            // Crear un mapa de los accesorios ya asociados al contrato para saber cuáles están presentes
            Map<Long, Boolean> contractAccessoriesMap = contract.getAccessories().stream()
                    .collect(Collectors.toMap(
                            ContractAccessory::getAccessoryCatalogId,
                            acc -> acc.getIsPresent() != null ? acc.getIsPresent() : true
                    ));
            
            // Mostrar todos los accesorios del catálogo
            for (AccessoryCatalog catalogItem : catalogAccessories) {
                HorizontalLayout row = new HorizontalLayout();
                row.setAlignItems(FlexComponent.Alignment.CENTER);
                row.setPadding(false);
                row.setSpacing(true);

                Checkbox checkbox = new Checkbox(catalogItem.getName());
                // Si el accesorio ya está en el contrato, usar su valor, sino por defecto true
                checkbox.setValue(contractAccessoriesMap.getOrDefault(catalogItem.getId(), true));
                checkbox.setReadOnly(false);
                
                // Usar el ID del catálogo como clave (no el ID del ContractAccessory)
                accessoryCheckboxes.put(catalogItem.getId(), checkbox);

                row.add(checkbox);
                accessoriesGrid.add(row);
            }
        } 
        // MODO LECTURA: Mostrar solo los accesorios asociados al contrato
        else {
            List<ContractAccessory> accessories = contract.getAccessories().stream()
                    .sorted(Comparator.comparingInt(a -> a.getDisplayOrder() != null ? a.getDisplayOrder() : 0))
                    .collect(Collectors.toList());

            for (ContractAccessory acc : accessories) {
                HorizontalLayout row = new HorizontalLayout();
                row.setAlignItems(FlexComponent.Alignment.CENTER);
                row.setPadding(false);
                row.setSpacing(true);

                Checkbox checkbox = new Checkbox(acc.getAccessoryName());
                checkbox.setValue(acc.getIsPresent() != null ? acc.getIsPresent() : true);
                checkbox.setReadOnly(true);
                // No necesitamos guardar en el mapa en modo lectura

                row.add(checkbox);
                accessoriesGrid.add(row);
            }
        }

        section.add(accessoriesGrid);

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
                        "<li>Entregar lavado el vehículo, de lo contrario se cobrará entre <strong>$5 a $15</strong> dependiendo de cómo se entregue de sucio</li>"
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

        mileageOutField = new IntegerField("Kilometraje de Salida");
        mileageOutField.setValue(vehicle.getMileage() != null ? vehicle.getMileage() : 0);
        mileageOutField.setMin(0);
        mileageOutField.setReadOnly(isReadOnly);
        form.add(mileageOutField);

        fuelLevelOutCombo = new ComboBox<>("Combustible Salida (%)");
        fuelLevelOutCombo.setItems(0, 25, 50, 75, 100);
        fuelLevelOutCombo.setValue(100);
        fuelLevelOutCombo.setReadOnly(isReadOnly);
        form.add(fuelLevelOutCombo);

        TextField fuelTypeField = new TextField("Tipo Combustible");
        fuelTypeField.setValue(vehicle.getFuelType() != null ? vehicle.getFuelType().name() : "GASOLINA");
        fuelTypeField.setReadOnly(true);
        form.add(fuelTypeField);

        TextField transmissionField = new TextField("Transmisión");
        transmissionField.setValue(vehicle.getTransmissionType() != null ? vehicle.getTransmissionType().name() : "");
        transmissionField.setReadOnly(true);
        form.add(transmissionField);

        section.add(form);
        return section;
    }

    // ========================================
    // SECCIÓN: Video del Estado del Vehículo
    // ========================================

   private Div createVideoSection() {
    Div section = createSection("ESTADO DEL VEHÍCULO - VIDEOS (3)");

    Paragraph instruction = new Paragraph(
            "Grabe 3 videos mostrando el estado del vehículo. " +
            "Los videos sirven como evidencia del estado al momento de la entrega.");
    instruction.getStyle().set("color", "var(--lumo-secondary-text-color)");
    section.add(instruction);

    // Info box con instrucciones
    Div infoBox = new Div();
    infoBox.getStyle()
            .set("background", "linear-gradient(135deg, #E3F2FD 0%, #BBDEFB 100%)")
            .set("border", "1px solid #64B5F6")
            .set("border-radius", "var(--lumo-border-radius-m)")
            .set("padding", "var(--lumo-space-m)")
            .set("margin-bottom", "var(--lumo-space-m)");
    
    infoBox.add(new Html(
            "<div style='font-size: var(--lumo-font-size-s);'>" +
            "<strong>🎥 Instrucciones para grabar los videos:</strong>" +
            "<ul style='margin: var(--lumo-space-xs) 0; padding-left: var(--lumo-space-l);'>" +
            "<li><strong>Video Exterior:</strong> Todos los lados del vehículo (frente, lados, trasera, techo)</li>" +
            "<li><strong>Video Interior:</strong> Asientos, tablero, piso, consola, techo interior</li>" +
            "<li><strong>Otros Detalles:</strong> Motor, cajuela, daños específicos, accesorios</li>" +
            "<li>Formatos aceptados: MP4, MOV, WebM</li>" +
            "<li>💡 Grabe desde su celular directamente para mejor calidad - SIN límite de tamaño</li>" +
            "</ul></div>"));
    section.add(infoBox);

    if (isReadOnly) {
        // Modo lectura: mostrar los 3 videos si existen
        section.add(createVideoReadOnlySection("🚗 Video Exterior", 
                contract.getVehicleExteriorVideoUrl()));
        section.add(createVideoReadOnlySection("🪑 Video Interior", 
                contract.getVehicleInteriorVideoUrl()));
        section.add(createVideoReadOnlySection("🔧 Otros Detalles", 
                contract.getVehicleDetailsVideoUrl()));
    } else {
        // Modo edición: permitir subir 3 videos
        Div videosGrid = new Div();
        videosGrid.getStyle()
                .set("display", "grid")
                .set("grid-template-columns", "repeat(auto-fit, minmax(280px, 1fr))")
                .set("gap", "var(--lumo-space-m)")
                .set("margin-top", "var(--lumo-space-m)");

        videosGrid.add(
                createVideoUploadSection("🚗 Video Exterior", "exterior", 
                        "Grabe todos los lados del vehículo"),
                createVideoUploadSection("🪑 Video Interior", "interior", 
                        "Grabe el interior completo"),
                createVideoUploadSection("🔧 Otros Detalles", "details", 
                        "Grabe motor, cajuela, daños específicos")
        );

        section.add(videosGrid);

        // Nota importante
        Div noteBox = new Div();
        noteBox.getStyle()
                .set("background", "var(--lumo-contrast-5pct)")
                .set("padding", "var(--lumo-space-s)")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("margin-top", "var(--lumo-space-m)")
                .set("font-size", "var(--lumo-font-size-s)");
        noteBox.add(new Html(
                "<div><strong>⚠️ Importante:</strong> Los videos son evidencia oficial del estado del vehículo. " +
                "Asegúrese de grabar claramente cualquier daño preexistente en cada área.</div>"));
        section.add(noteBox);
    }

    return section;
}

private VerticalLayout createVideoReadOnlySection(String title, String videoUrl) {
    VerticalLayout layout = new VerticalLayout();
    layout.setPadding(false);
    layout.setSpacing(true);

    H4 videoTitle = new H4(title);
    videoTitle.getStyle().set("margin", "var(--lumo-space-s) 0");
    layout.add(videoTitle);

    if (videoUrl != null && !videoUrl.isEmpty()) {
        Div videoContainer = new Div();
        videoContainer.getStyle()
                .set("text-align", "center")
                .set("margin", "var(--lumo-space-m) 0");

        Html video = new Html(
                "<video controls style='max-width: 100%; max-height: 300px; border-radius: 8px;'>" +
                "<source src='" + videoUrl + "' type='video/mp4'>" +
                "Su navegador no soporta el elemento de video." +
                "</video>");
        videoContainer.add(video);

        Anchor downloadLink = new Anchor(videoUrl, "⬇️ Descargar");
        downloadLink.getElement().setAttribute("download", "");
        downloadLink.getElement().setAttribute("target", "_blank");
        downloadLink.getStyle()
                .set("display", "inline-block")
                .set("margin-top", "var(--lumo-space-s)");

        layout.add(videoContainer, downloadLink);
    } else {
        Paragraph noVideo = new Paragraph("No se subió este video");
        noVideo.getStyle().set("color", "var(--lumo-secondary-text-color)");
        layout.add(noVideo);
    }

    return layout;
}

private VerticalLayout createVideoUploadSection(String title, String videoType, String hint) {
    VerticalLayout layout = new VerticalLayout();
    layout.setPadding(true);
    layout.setSpacing(true);
    layout.getStyle()
            .set("border", "2px dashed var(--lumo-contrast-30pct)")
            .set("border-radius", "var(--lumo-border-radius-m)")
            .set("background", "var(--lumo-contrast-5pct)");

    H4 videoTitle = new H4(title);
    videoTitle.getStyle().set("margin", "0 0 var(--lumo-space-s) 0");

    Paragraph hintText = new Paragraph(hint);
    hintText.getStyle()
            .set("font-size", "var(--lumo-font-size-s)")
            .set("color", "var(--lumo-secondary-text-color)")
            .set("margin", "0 0 var(--lumo-space-m) 0");

    // Preview container
    Div previewContainer = new Div();
    previewContainer.setId("video-preview-" + videoType);
    previewContainer.getStyle()
            .set("text-align", "center")
            .set("margin-bottom", "var(--lumo-space-m)");

    Span placeholderIcon = new Span("🎥");
    placeholderIcon.getStyle().set("font-size", "2.5em");
    
    Paragraph placeholderText = new Paragraph("Selecciona el video");
    placeholderText.getStyle()
            .set("font-size", "var(--lumo-font-size-s)")
            .set("color", "var(--lumo-secondary-text-color)");

    previewContainer.add(placeholderIcon, placeholderText);

    // Upload component
    MemoryBuffer buffer = new MemoryBuffer();
    Upload upload = new Upload(buffer);
    upload.setAcceptedFileTypes("video/mp4", "video/quicktime", "video/webm", "video/x-msvideo");
    upload.setMaxFiles(1);
    // SIN LÍMITE - 500MB es suficiente para la mayoría de videos desde celular
    upload.setMaxFileSize(500 * 1024 * 1024); // 500MB

    Button uploadButton = new Button("Seleccionar", VaadinIcon.UPLOAD.create());
    uploadButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_PRIMARY);
    upload.setUploadButton(uploadButton);
    upload.setDropAllowed(false);

    upload.addSucceededListener(event -> {
        try {
            String fileName = event.getFileName();
            String mimeType = event.getMIMEType();
            InputStream inputStream = buffer.getInputStream();

            // Subir el video según el tipo
            contractService.uploadVehicleVideo(token, inputStream, fileName, mimeType, videoType);

            // Actualizar UI
            previewContainer.removeAll();
            Span successIcon = new Span("✅");
            successIcon.getStyle().set("font-size", "2em");
            Paragraph successText = new Paragraph("Video subido: " + fileName);
            successText.getStyle()
                    .set("font-size", "var(--lumo-font-size-s)")
                    .set("color", "var(--lumo-success-color)");
            previewContainer.add(successIcon, successText);

            // Marcar como subido
            switch (videoType) {
                case "exterior" -> vehicleExteriorVideoUrl = "uploaded";
                case "interior" -> vehicleInteriorVideoUrl = "uploaded";
                case "details" -> vehicleDetailsVideoUrl = "uploaded";
            }

            Notification.show("✅ " + title + " subido exitosamente", 
                    3000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);

        } catch (Exception e) {
            log.error("Error subiendo video {}: {}", videoType, e.getMessage(), e);
            Notification.show("Error al subir " + title + ": " + e.getMessage(), 
                    5000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    });

    upload.addFileRejectedListener(event -> {
        Notification.show(title + " rechazado: " + event.getErrorMessage(), 
                4000, Notification.Position.MIDDLE)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
    });

    layout.add(videoTitle, hintText, previewContainer, upload);
    return layout;
}

    // ========================================
    // SECCIÓN: Términos y Condiciones
    // ========================================

    private Div createTermsSection() {
        Div section = createSection("TÉRMINOS Y CONDICIONES");

        var vehicle = contract.getRental().getVehicle();
        Integer theftPercentage = vehicle.getTheftDeductiblePercentage();

        Div terms = new Div();
        terms.addClassName("contract-terms");
        terms.add(new Html(
                "<div style='font-size: var(--lumo-font-size-s);'>" +
                        "<ol>" +
                        "<li>El arrendatario se compromete a devolver el vehículo en las mismas condiciones en que lo recibió.</li>"
                        +
                        "<li>Entregar lavado el vehículo, de lo contrario se cobrará entre <strong>$5 a $15</strong> dependiendo de cómo se entregue de sucio.</li>"
                        +
                        "<li>Manchas o derrame de líquido en tapicería: <strong>$20.00</strong></li>" +
                        "<li>El arrendatario es responsable de cualquier daño o pérdida del vehículo durante el período de renta.</li>"
                        +
                        "<li>En caso de accidente, el arrendatario participará con el deducible establecido en este contrato.</li>"
                        +
                        "<li>En caso de robo, el arrendatario participará con el <strong>" + theftPercentage
                        + "%</strong> del valor del vehículo como deducible.</li>" +
                        "<li>El vehículo no puede salir del país sin autorización previa por escrito.</li>" +
                        "<li>Está prohibido fumar dentro del vehículo.</li>" +
                        "</ol>" +
                        "</div>"));

        section.add(terms);

        observationsField = new TextArea("Observaciones");
        observationsField.setWidthFull();
        observationsField.setMinHeight("100px");
        observationsField.setReadOnly(isReadOnly);
        section.add(observationsField);

        return section;
    }

    // ========================================
    // SECCIÓN: Firmas (Cliente y Empleado)
    // ========================================

    private Div createSignatureSection() {
        Div section = createSection("FIRMAS");

        HorizontalLayout signaturesRow = new HorizontalLayout();
        signaturesRow.setWidthFull();
        signaturesRow.setSpacing(true);
        signaturesRow.getStyle()
                .set("flex-wrap", "wrap")
                .set("gap", "var(--lumo-space-m)")
                .set("justify-content", "space-around");

        // ===== FIRMA DEL CLIENTE =====
        VerticalLayout clientSignature = new VerticalLayout();
        clientSignature.setAlignItems(FlexComponent.Alignment.CENTER);
        clientSignature.getStyle().set("flex", "1").set("min-width", "280px");
        clientSignature.setPadding(false);

        Paragraph clientLabel = new Paragraph("FIRMA DEL CLIENTE");
        clientLabel.getStyle().set("font-weight", "bold");

        if (isReadOnly && contract.getSignatureUrl() != null) {
            Image signatureImg = new Image(contract.getSignatureUrl(), "Firma Cliente");
            signatureImg.setMaxWidth("300px");
            signatureImg.setMaxHeight("150px");
            clientSignature.add(clientLabel, signatureImg);

            Span clientName = new Span(contract.getRental().getCustomer().getFullName());
            clientName.getStyle().set("font-size", "var(--lumo-font-size-s)");
            clientSignature.add(clientName);
        } else if (!isReadOnly) {
            // Campo de nombre del cliente (solo lectura para alinear con el del empleado)
            TextField clientNameField = new TextField("Nombre del Cliente");
            clientNameField.setValue(contract.getRental().getCustomer().getFullName());
            clientNameField.setReadOnly(true);
            clientNameField.setWidthFull();
            clientNameField.setMaxWidth("350px");

            Div signatureContainer = new Div();
            signatureContainer.setId("client-signature-container");
            signatureContainer.addClassName("signature-box");

            Html canvas = new Html(
                    "<canvas id='client-signature-canvas' width='350' height='150' " +
                            "style='border: 2px solid #ccc; border-radius: 8px; background: white; touch-action: none;'></canvas>");
            signatureContainer.add(canvas);

            Button clearBtn = new Button("Limpiar", VaadinIcon.ERASER.create());
            clearBtn.addThemeVariants(ButtonVariant.LUMO_SMALL);
            clearBtn.addClickListener(e -> {
                UI.getCurrent().getPage().executeJs(
                        "const canvas = document.getElementById('client-signature-canvas');" +
                                "const ctx = canvas.getContext('2d');" +
                                "ctx.clearRect(0, 0, canvas.width, canvas.height);");
                clientSignatureBase64 = null;
            });

            clientSignature.add(clientLabel, clientNameField, signatureContainer, clearBtn);
        } else {
            Div placeholder = new Div();
            placeholder.getStyle()
                    .set("width", "300px")
                    .set("height", "100px")
                    .set("border-bottom", "2px solid black");
            clientSignature.add(clientLabel, placeholder);
        }

        // ===== FIRMA DEL EMPLEADO =====
        VerticalLayout employeeSignature = new VerticalLayout();
        employeeSignature.setAlignItems(FlexComponent.Alignment.CENTER);
        employeeSignature.getStyle().set("flex", "1").set("min-width", "280px");
        employeeSignature.setPadding(false);

        Paragraph employeeLabel = new Paragraph("FIRMA DEL EMPLEADO");
        employeeLabel.getStyle().set("font-weight", "bold");

        if (isReadOnly && contract.getEmployeeSignatureUrl() != null) {
            Image employeeSigImg = new Image(contract.getEmployeeSignatureUrl(), "Firma Empleado");
            employeeSigImg.setMaxWidth("300px");
            employeeSigImg.setMaxHeight("150px");
            employeeSignature.add(employeeLabel, employeeSigImg);

            if (contract.getEmployeeName() != null) {
                Span empName = new Span(contract.getEmployeeName());
                empName.getStyle().set("font-size", "var(--lumo-font-size-s)");
                employeeSignature.add(empName);
            }
        } else if (!isReadOnly) {
            employeeNameField = new TextField("Nombre del Empleado");
            employeeNameField.setWidthFull();
            employeeNameField.setMaxWidth("350px");
            employeeNameField.setPlaceholder("Nombre completo del empleado que entrega");
            employeeNameField.setRequired(true);

            Div signatureContainer = new Div();
            signatureContainer.setId("employee-signature-container");
            signatureContainer.addClassName("signature-box");

            Html canvas = new Html(
                    "<canvas id='employee-signature-canvas' width='350' height='150' " +
                            "style='border: 2px solid #ccc; border-radius: 8px; background: white; touch-action: none;'></canvas>");
            signatureContainer.add(canvas);

            Button clearBtn = new Button("Limpiar", VaadinIcon.ERASER.create());
            clearBtn.addThemeVariants(ButtonVariant.LUMO_SMALL);
            clearBtn.addClickListener(e -> {
                UI.getCurrent().getPage().executeJs(
                        "const canvas = document.getElementById('employee-signature-canvas');" +
                                "const ctx = canvas.getContext('2d');" +
                                "ctx.clearRect(0, 0, canvas.width, canvas.height);");
                employeeSignatureBase64 = null;
            });

            employeeSignature.add(employeeLabel, employeeNameField, signatureContainer, clearBtn);
        } else {
            Div placeholder = new Div();
            placeholder.getStyle()
                    .set("width", "300px")
                    .set("height", "100px")
                    .set("border-bottom", "2px solid black");
            employeeSignature.add(employeeLabel, placeholder);
        }

        signaturesRow.add(clientSignature, employeeSignature);
        section.add(signaturesRow);

        // Inicializar ambos canvas
        if (!isReadOnly) {
            UI.getCurrent().getPage().executeJs(
                    "setTimeout(() => {" +
                            "  ['client-signature-canvas', 'employee-signature-canvas'].forEach(canvasId => {" +
                            "    const canvas = document.getElementById(canvasId);" +
                            "    if (!canvas) return;" +
                            "    const ctx = canvas.getContext('2d');" +
                            "    let drawing = false;" +
                            "    ctx.strokeStyle = '#000';" +
                            "    ctx.lineWidth = 2;" +
                            "    ctx.lineCap = 'round';" +
                            "    function getPos(e) {" +
                            "      const rect = canvas.getBoundingClientRect();" +
                            "      const clientX = e.touches ? e.touches[0].clientX : e.clientX;" +
                            "      const clientY = e.touches ? e.touches[0].clientY : e.clientY;" +
                            "      return { x: clientX - rect.left, y: clientY - rect.top };" +
                            "    }" +
                            "    canvas.addEventListener('mousedown', (e) => { drawing = true; ctx.beginPath(); ctx.moveTo(getPos(e).x, getPos(e).y); });"
                            +
                            "    canvas.addEventListener('mousemove', (e) => { if (drawing) { ctx.lineTo(getPos(e).x, getPos(e).y); ctx.stroke(); } });"
                            +
                            "    canvas.addEventListener('mouseup', () => { drawing = false; });" +
                            "    canvas.addEventListener('mouseout', () => { drawing = false; });" +
                            "    canvas.addEventListener('touchstart', (e) => { e.preventDefault(); drawing = true; ctx.beginPath(); ctx.moveTo(getPos(e).x, getPos(e).y); });"
                            +
                            "    canvas.addEventListener('touchmove', (e) => { e.preventDefault(); if (drawing) { ctx.lineTo(getPos(e).x, getPos(e).y); ctx.stroke(); } });"
                            +
                            "    canvas.addEventListener('touchend', () => { drawing = false; });" +
                            "  });" +
                            "}, 500);");

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

                if (employeeNameField.getValue() == null || employeeNameField.getValue().trim().isEmpty()) {
                    Notification.show("Ingrese el nombre del empleado que entrega", 3000, Notification.Position.MIDDLE)
                            .addThemeVariants(NotificationVariant.LUMO_ERROR);
                    employeeNameField.focus();
                    return;
                }

                // Validar y obtener firmas usando llamadas separadas
                validateAndSubmitSignatures();
            });

            HorizontalLayout signRow = new HorizontalLayout(acceptTerms, signBtn);
            signRow.setWidthFull();
            signRow.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
            signRow.setAlignItems(FlexComponent.Alignment.CENTER);
            signRow.getStyle().set("flex-wrap", "wrap").set("gap", "var(--lumo-space-m)");

            section.add(signRow);
        }

        return section;
    }

    // ========================================
    // Validación de firmas
    // ========================================

    private void validateAndSubmitSignatures() {
        // Primero verificar si el canvas del cliente está vacío
        UI.getCurrent().getPage().executeJs(
                "const canvas = document.getElementById('client-signature-canvas');" +
                "if (!canvas) return 'NO_CANVAS';" +
                "const ctx = canvas.getContext('2d');" +
                "const pixelData = ctx.getImageData(0, 0, canvas.width, canvas.height).data;" +
                "for (let i = 3; i < pixelData.length; i += 4) {" +
                "  if (pixelData[i] !== 0) return 'HAS_CONTENT';" +
                "}" +
                "return 'EMPTY';")
        .then(String.class, clientStatus -> {
            if ("EMPTY".equals(clientStatus) || "NO_CANVAS".equals(clientStatus)) {
                Notification.show("El cliente debe firmar en el área correspondiente", 3000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }
            
            // Verificar si el canvas del empleado está vacío
            UI.getCurrent().getPage().executeJs(
                    "const canvas = document.getElementById('employee-signature-canvas');" +
                    "if (!canvas) return 'NO_CANVAS';" +
                    "const ctx = canvas.getContext('2d');" +
                    "const pixelData = ctx.getImageData(0, 0, canvas.width, canvas.height).data;" +
                    "for (let i = 3; i < pixelData.length; i += 4) {" +
                    "  if (pixelData[i] !== 0) return 'HAS_CONTENT';" +
                    "}" +
                    "return 'EMPTY';")
            .then(String.class, employeeStatus -> {
                if ("EMPTY".equals(employeeStatus) || "NO_CANVAS".equals(employeeStatus)) {
                    Notification.show("El empleado debe firmar en el área correspondiente", 3000, Notification.Position.MIDDLE)
                            .addThemeVariants(NotificationVariant.LUMO_ERROR);
                    return;
                }
                
                // Obtener la firma del cliente
                UI.getCurrent().getPage().executeJs(
                        "return document.getElementById('client-signature-canvas')?.toDataURL('image/png') || '';")
                .then(String.class, clientData -> {
                    if (clientData == null || clientData.length() < 1000) {
                        Notification.show("Por favor, el cliente debe dibujar su firma", 3000, Notification.Position.MIDDLE)
                                .addThemeVariants(NotificationVariant.LUMO_ERROR);
                        return;
                    }
                    clientSignatureBase64 = clientData;
                    
                    // Obtener la firma del empleado
                    UI.getCurrent().getPage().executeJs(
                            "return document.getElementById('employee-signature-canvas')?.toDataURL('image/png') || '';")
                    .then(String.class, employeeData -> {
                        if (employeeData == null || employeeData.length() < 1000) {
                            Notification.show("Por favor, el empleado debe dibujar su firma", 3000, Notification.Position.MIDDLE)
                                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
                            return;
                        }
                        employeeSignatureBase64 = employeeData;
                        
                        // Ambas firmas válidas, proceder a firmar
                        submitContract();
                    });
                });
            });
        });
    }

    // ========================================
    // Métodos auxiliares
    // ========================================

    private Div createSection(String title) {
        Div section = new Div();
        section.addClassName("contract-section");
        section.setWidthFull();
        section.getStyle()
                .set("background", "white")
                .set("border", "1px solid var(--lumo-contrast-20pct)")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("padding", "var(--lumo-space-m)")
                .set("margin-bottom", "var(--lumo-space-m)")
                .set("width", "100%")
                .set("box-sizing", "border-box");

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

        // Cargar URL del video si existe
        if (contract.getVehicleExteriorVideoUrl() != null) {
            vehicleExteriorVideoUrl = contract.getVehicleExteriorVideoUrl();
        }
        if (contract.getVehicleInteriorVideoUrl() != null) {
            vehicleInteriorVideoUrl = contract.getVehicleInteriorVideoUrl();
        }
        if (contract.getVehicleDetailsVideoUrl() != null) {
            vehicleDetailsVideoUrl = contract.getVehicleDetailsVideoUrl();
        }
    }

    private void submitContract() {
        try {
            Dialog loadingDialog = new Dialog();
            loadingDialog.setCloseOnEsc(false);
            loadingDialog.setCloseOnOutsideClick(false);
            loadingDialog.add(new Paragraph("Procesando contrato..."));
            loadingDialog.open();

            saveFormData();

            String ipAddress = VaadinRequest.getCurrent().getRemoteAddr();
            String userAgent = VaadinRequest.getCurrent().getHeader("User-Agent");

            contractService.signContractWithEmployeeSignature(
                    token,
                    clientSignatureBase64,
                    employeeSignatureBase64,
                    employeeNameField.getValue().trim(),
                    ipAddress,
                    userAgent);

            loadingDialog.close();

            showSuccess();

        } catch (Exception e) {
            log.error("Error al firmar contrato: {}", e.getMessage(), e);
            Notification.show("Error: " + e.getMessage(), 5000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void saveFormData() {
        contractService.updateDocumentInfoWithPhotos(token,
                documentTypeCombo.getValue(),
                documentNumberField.getValue(),
                documentFrontBase64,
                documentBackBase64,
                licenseFrontBase64,
                licenseBackBase64);

        contractService.updateVehicleInfo(token,
                mileageOutField.getValue(),
                fuelLevelOutCombo.getValue());

        List<ContractService.ContractAccessoryDTO> accessoryDTOs = new ArrayList<>();
        for (var entry : accessoryCheckboxes.entrySet()) {
            ContractService.ContractAccessoryDTO dto = new ContractService.ContractAccessoryDTO();
            dto.setCatalogId(entry.getKey());
            dto.setIsPresent(entry.getValue().getValue());
            accessoryDTOs.add(dto);
        }
        contractService.updateAccessories(token, accessoryDTOs);

        // El video ya se sube directamente en el upload, no necesita guardarse aquí

        contractService.updateAdditionalInfo(token,
                deliveryLocationField.getValue(),
                addressElSalvadorField.getValue(),
                addressForeignField.getValue(),
                phoneUsaField.getValue(),
                phoneFamilyField.getValue(),
                paymentMethodCombo.getValue(),
                parseDecimal(depositField.getValue()),
                parseDecimal(accidentDeductibleField.getValue()),
                null,
                additionalDriverField.getValue(),
                additionalDriverLicenseField.getValue(),
                additionalDriverDuiField.getValue(),
                observationsField.getValue());
    }

    private BigDecimal parseDecimal(String value) {
        if (value == null || value.isEmpty())
            return null;
        try {
            return new BigDecimal(value.replace(",", "").replace("$", "").trim());
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
