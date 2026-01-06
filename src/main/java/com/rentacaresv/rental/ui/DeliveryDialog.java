package com.rentacaresv.rental.ui;

import com.rentacaresv.rental.application.RentalContractPdfGenerator;
import com.rentacaresv.rental.application.RentalDTO;
import com.rentacaresv.rental.application.RentalPhotoService;
import com.rentacaresv.rental.application.RentalService;
import com.rentacaresv.rental.domain.Rental;
import com.rentacaresv.rental.domain.photo.RentalPhotoType;
import com.rentacaresv.shared.storage.StorageInitializer;
import com.rentacaresv.shared.ui.PhotoUploadPanel;
import com.rentacaresv.shared.ui.RentalPhotoUploadPanel;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.shared.Registration;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Diálogo para entregar vehículo al cliente
 * - Cambia estado: PENDING → ACTIVE
 * - Sube fotos de entrega
 * - Guarda notas
 */
@Slf4j
public class DeliveryDialog extends Dialog {

    private final RentalService rentalService;
    private final RentalPhotoService rentalPhotoService;
    private final StorageInitializer storageInitializer;
    private final RentalContractPdfGenerator pdfGenerator;
    private final RentalDTO rental;

    private RentalPhotoUploadPanel photoPanel;
    private TextArea notesField;
    private Button generateContractButton;
    private Button confirmButton;
    private Button cancelButton;

    public DeliveryDialog(RentalService rentalService,
            RentalPhotoService rentalPhotoService,
            StorageInitializer storageInitializer,
            RentalContractPdfGenerator pdfGenerator,
            RentalDTO rental) {
        this.rentalService = rentalService;
        this.rentalPhotoService = rentalPhotoService;
        this.storageInitializer = storageInitializer;
        this.pdfGenerator = pdfGenerator;
        this.rental = rental;

        configureDialog();
        createContent();
        createFooter();
    }

    private void configureDialog() {
        setModal(true);
        setDraggable(false);
        setResizable(false);
        setWidth("900px");
        setMaxHeight("90vh");
    }

    private void createContent() {
        // Header con icono
        HorizontalLayout titleLayout = new HorizontalLayout();
        titleLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        titleLayout.setSpacing(true);
        
        Icon carIcon = VaadinIcon.CAR.create();
        carIcon.setSize("24px");
        carIcon.getStyle().set("color", "var(--lumo-primary-color)");
        
        H3 title = new H3("Entregar Vehículo");
        title.getStyle().set("margin", "0");
        
        titleLayout.add(carIcon, title);

        // Info de la renta
        VerticalLayout infoLayout = createInfoSection();

        // Separador
        Div separator1 = createSeparator();

        // Panel de fotos
        photoPanel = new RentalPhotoUploadPanel("Fotos de Entrega", true);

        // Separador
        Div separator2 = createSeparator();

        // Notas
        HorizontalLayout notesTitleLayout = new HorizontalLayout();
        notesTitleLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        Icon notesIcon = VaadinIcon.CLIPBOARD_TEXT.create();
        notesIcon.setSize("18px");
        H4 notesTitle = new H4("Notas de Entrega");
        notesTitle.getStyle().set("margin", "0");
        notesTitleLayout.add(notesIcon, notesTitle);
        notesTitleLayout.getStyle().set("margin", "1rem 0 0.5rem 0");

        notesField = new TextArea();
        notesField.setPlaceholder(
                "Ej: Vehículo entregado en perfecto estado, tanque lleno, todos los accesorios incluidos...");
        notesField.setWidthFull();
        notesField.setMinHeight("100px");
        notesField.setMaxLength(1000);
        notesField.setHelperText("Opcional - Máximo 1000 caracteres");

        // Layout principal
        VerticalLayout content = new VerticalLayout(
                titleLayout,
                infoLayout,
                separator1,
                photoPanel,
                separator2,
                notesTitleLayout,
                notesField);
        content.setPadding(true);
        content.setSpacing(true);

        add(content);
    }

    private VerticalLayout createInfoSection() {
        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(false);
        layout.setSpacing(false);
        layout.getStyle()
                .set("background", "var(--lumo-contrast-5pct)")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("padding", "1rem");

        // Vehículo
        HorizontalLayout vehicleRow = new HorizontalLayout();
        vehicleRow.setAlignItems(FlexComponent.Alignment.CENTER);
        Span vehicleLabel = new Span("Vehículo:");
        vehicleLabel.getStyle().set("font-weight", "bold").set("width", "120px");
        Span vehicleValue = new Span(rental.getVehicleDescription());
        vehicleRow.add(vehicleLabel, vehicleValue);

        // Cliente
        HorizontalLayout customerRow = new HorizontalLayout();
        customerRow.setAlignItems(FlexComponent.Alignment.CENTER);
        Span customerLabel = new Span("Cliente:");
        customerLabel.getStyle().set("font-weight", "bold").set("width", "120px");
        Span customerValue = new Span(rental.getCustomerName());
        customerRow.add(customerLabel, customerValue);

        // Fecha entrega
        HorizontalLayout dateRow = new HorizontalLayout();
        dateRow.setAlignItems(FlexComponent.Alignment.CENTER);
        Span dateLabel = new Span("Fecha entrega:");
        dateLabel.getStyle().set("font-weight", "bold").set("width", "120px");
        Span dateValue = new Span(rental.getStartDate().toString());
        dateRow.add(dateLabel, dateValue);

        layout.add(vehicleRow, customerRow, dateRow);
        return layout;
    }

    private Div createSeparator() {
        Div separator = new Div();
        separator.getStyle()
                .set("height", "1px")
                .set("background", "var(--lumo-contrast-10pct)")
                .set("margin", "1rem 0");
        return separator;
    }

    private void createFooter() {
        generateContractButton = new Button("Generar Contrato", VaadinIcon.FILE_TEXT_O.create());
        generateContractButton.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
        generateContractButton.addClickListener(e -> generateContract());

        confirmButton = new Button("Confirmar Entrega", VaadinIcon.CHECK.create());
        confirmButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);
        confirmButton.addClickListener(e -> confirmDelivery());

        cancelButton = new Button("Cancelar", VaadinIcon.CLOSE.create());
        cancelButton.addClickListener(e -> close());

        HorizontalLayout footer = new HorizontalLayout(generateContractButton, confirmButton, cancelButton);
        footer.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        footer.setWidthFull();
        footer.getStyle().set("padding", "1rem");

        getFooter().add(footer);
    }

    private void generateContract() {
        try {
            // Obtener la renta completa desde el servicio
            Rental rental = rentalService.findRentalEntityById(this.rental.getId());

            // Obtener notas de entrega
            String deliveryNotes = notesField.getValue();

            // Obtener URLs de fotos pendientes (simulado)
            List<String> photoUrls = new ArrayList<>();

            // Generar PDF
            byte[] pdfBytes = pdfGenerator.generateContract(rental, deliveryNotes, photoUrls);

            // Mostrar PDF en modal con previsualizador
            showPdfPreview(pdfBytes, rental.getContractNumber());

            showSuccessNotification("Contrato generado exitosamente");

        } catch (Exception e) {
            log.error("Error generando contrato PDF: {}", e.getMessage(), e);
            showErrorNotification("Error al generar contrato: " + e.getMessage());
        }
    }

    /**
     * Muestra el PDF en un modal con previsualizador
     */
    private void showPdfPreview(byte[] pdfBytes, String contractNumber) {
        // Crear dialog para el previsualizador
        Dialog pdfDialog = new Dialog();
        pdfDialog.setWidth("900px");
        pdfDialog.setHeight("700px");
        pdfDialog.setModal(true);
        pdfDialog.setDraggable(false);
        pdfDialog.setCloseOnOutsideClick(false);
        
        // Título del modal con icono
        HorizontalLayout dialogTitleLayout = new HorizontalLayout();
        dialogTitleLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        Icon pdfIcon = VaadinIcon.FILE_TEXT.create();
        pdfIcon.setSize("20px");
        H3 dialogTitle = new H3("Contrato - " + contractNumber);
        dialogTitle.getStyle().set("margin", "0");
        dialogTitleLayout.add(pdfIcon, dialogTitle);
        
        // Botón cerrar con icono
        Button closeIcon = new Button(new Icon("lumo", "cross"));
        closeIcon.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON);
        closeIcon.addClickListener(e -> pdfDialog.close());
        
        // Header
        HorizontalLayout header = new HorizontalLayout(dialogTitleLayout, closeIcon);
        header.setWidthFull();
        header.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.setPadding(false);
        header.setSpacing(false);
        
        // Convertir PDF a Base64
        String base64Pdf = java.util.Base64.getEncoder().encodeToString(pdfBytes);
        String pdfDataUri = "data:application/pdf;base64," + base64Pdf;
        
        // Crear HTML object para mostrar el PDF
        com.vaadin.flow.component.Html pdfViewer = new com.vaadin.flow.component.Html(
            "<object data='" + pdfDataUri + "' " +
            "type='application/pdf' " +
            "style='width: 100%; height: 550px; border: 1px solid #e0e0e0;'>" +
            "<p>No se puede visualizar el PDF</p>" +
            "</object>"
        );
        
        // Crear recurso para descarga
        StreamResource resource = new StreamResource(
            "Contrato_" + contractNumber + ".pdf",
            () -> new ByteArrayInputStream(pdfBytes)
        );
        resource.setContentType("application/pdf");
        
        // Botón de descarga
        Anchor downloadLink = new Anchor(resource, "");
        downloadLink.getElement().setAttribute("download", true);
        downloadLink.getStyle().set("text-decoration", "none");
        
        Button downloadButton = new Button("Descargar", VaadinIcon.DOWNLOAD.create());
        downloadButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        downloadButton.addClickListener(e -> downloadLink.getElement().callJsFunction("click"));
        downloadLink.add(downloadButton);
        
        Button closeButton = new Button("Cerrar");
        closeButton.addClickListener(e -> pdfDialog.close());
        
        // Footer
        HorizontalLayout footer = new HorizontalLayout(downloadLink, closeButton);
        footer.setWidthFull();
        footer.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        footer.setPadding(false);
        footer.setSpacing(true);
        
        // Layout principal
        VerticalLayout content = new VerticalLayout(header, pdfViewer, footer);
        content.setSizeFull();
        content.setPadding(true);
        content.setSpacing(true);
        
        pdfDialog.add(content);
        pdfDialog.open();
    }

    private void confirmDelivery() {
        try {
            // Inicializar storage si es necesario
            storageInitializer.initializeIfNeeded();

            // 1. Cambiar estado de renta a ACTIVE
            rentalService.deliverRental(rental.getId(), notesField.getValue());

            // 2. Subir fotos pendientes
            uploadPendingPhotos();

            // 3. Mostrar éxito y cerrar
            fireEvent(new DeliveryConfirmedEvent(this));
            showSuccessNotification("Vehículo entregado exitosamente");
            close();

        } catch (Exception e) {
            log.error("Error al entregar vehículo: {}", e.getMessage(), e);
            showErrorNotification("Error al entregar vehículo: " + e.getMessage());
        }
    }

    private void uploadPendingPhotos() {
        Map<RentalPhotoType, List<PhotoUploadPanel.PendingUpload>> allUploads = photoPanel.getAllPendingUploads();

        for (Map.Entry<RentalPhotoType, List<PhotoUploadPanel.PendingUpload>> entry : allUploads.entrySet()) {
            RentalPhotoType photoType = entry.getKey();
            List<PhotoUploadPanel.PendingUpload> uploads = entry.getValue();

            for (PhotoUploadPanel.PendingUpload upload : uploads) {
                try {
                    rentalPhotoService.uploadPhoto(
                            rental.getId(),
                            upload.getInputStream(),
                            upload.getFileName(),
                            upload.getMimeType(),
                            photoType,
                            null
                    );
                    log.info("Foto de entrega subida: {} - {}", photoType, upload.getFileName());
                } catch (Exception e) {
                    log.error("Error subiendo foto de entrega: {}", e.getMessage(), e);
                }
            }
        }
    }

    private void showSuccessNotification(String message) {
        Notification notification = Notification.show(message, 3000, Notification.Position.TOP_CENTER);
        notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }

    private void showErrorNotification(String message) {
        Notification notification = Notification.show(message, 5000, Notification.Position.TOP_CENTER);
        notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
    }

    // Eventos
    public static class DeliveryConfirmedEvent extends ComponentEvent<DeliveryDialog> {
        public DeliveryConfirmedEvent(DeliveryDialog source) {
            super(source, false);
        }
    }

    public Registration addDeliveryConfirmedListener(ComponentEventListener<DeliveryConfirmedEvent> listener) {
        return addListener(DeliveryConfirmedEvent.class, listener);
    }
}
