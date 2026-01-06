package com.rentacaresv.rental.ui;

import com.rentacaresv.rental.application.RentalDTO;
import com.rentacaresv.rental.application.RentalPhotoService;
import com.rentacaresv.rental.application.RentalService;
import com.rentacaresv.rental.domain.photo.RentalPhotoType;
import com.rentacaresv.shared.storage.StorageInitializer;
import com.rentacaresv.shared.ui.PhotoUploadPanel;
import com.rentacaresv.shared.ui.RentalPhotoUploadPanel;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dialog.Dialog;
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
import com.vaadin.flow.shared.Registration;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

/**
 * Diálogo para recibir devolución de vehículo
 * - Cambia estado: ACTIVE → COMPLETED
 * - Sube fotos de devolución
 * - Guarda notas
 * - Opción de marcar vehículo para mantenimiento
 */
@Slf4j
public class ReturnDialog extends Dialog {

    private final RentalService rentalService;
    private final RentalPhotoService rentalPhotoService;
    private final StorageInitializer storageInitializer;
    private final RentalDTO rental;

    private RentalPhotoUploadPanel photoPanel;
    private TextArea notesField;
    private Checkbox maintenanceCheckbox;
    private Button confirmButton;
    private Button cancelButton;

    public ReturnDialog(RentalService rentalService,
                        RentalPhotoService rentalPhotoService,
                        StorageInitializer storageInitializer,
                        RentalDTO rental) {
        this.rentalService = rentalService;
        this.rentalPhotoService = rentalPhotoService;
        this.storageInitializer = storageInitializer;
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
        
        Icon returnIcon = VaadinIcon.SIGN_IN.create();
        returnIcon.setSize("24px");
        returnIcon.getStyle().set("color", "var(--lumo-primary-color)");
        
        H3 title = new H3("Recibir Devolución");
        title.getStyle().set("margin", "0");
        
        titleLayout.add(returnIcon, title);

        // Info de la renta
        VerticalLayout infoLayout = createInfoSection();

        // Separador
        Div separator1 = createSeparator();

        // Panel de fotos
        photoPanel = new RentalPhotoUploadPanel("Fotos de Devolución", false);

        // Separador
        Div separator2 = createSeparator();

        // Notas con icono
        HorizontalLayout notesTitleLayout = new HorizontalLayout();
        notesTitleLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        Icon notesIcon = VaadinIcon.CLIPBOARD_TEXT.create();
        notesIcon.setSize("18px");
        H4 notesTitle = new H4("Notas de Devolución");
        notesTitle.getStyle().set("margin", "0");
        notesTitleLayout.add(notesIcon, notesTitle);
        notesTitleLayout.getStyle().set("margin", "1rem 0 0.5rem 0");

        notesField = new TextArea();
        notesField.setPlaceholder("Ej: Vehículo devuelto en buen estado, rayón menor en puerta trasera, kilometraje: 15,250...");
        notesField.setWidthFull();
        notesField.setMinHeight("100px");
        notesField.setMaxLength(1000);
        notesField.setHelperText("Opcional - Máximo 1000 caracteres");

        // Checkbox de mantenimiento con icono
        HorizontalLayout maintenanceLayout = new HorizontalLayout();
        maintenanceLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        maintenanceLayout.setSpacing(true);
        
        Icon warningIcon = VaadinIcon.WARNING.create();
        warningIcon.setSize("16px");
        warningIcon.getStyle().set("color", "var(--lumo-error-color)");
        
        maintenanceCheckbox = new Checkbox("Requiere mantenimiento o reparación");
        maintenanceCheckbox.getStyle().set("color", "var(--lumo-error-text-color)");
        
        maintenanceLayout.add(warningIcon, maintenanceCheckbox);
        maintenanceLayout.getStyle().set("margin-top", "0.5rem");

        // Layout principal
        VerticalLayout content = new VerticalLayout(
            titleLayout,
            infoLayout,
            separator1,
            photoPanel,
            separator2,
            notesTitleLayout,
            notesField,
            maintenanceLayout
        );
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

        // Fecha devolución
        HorizontalLayout dateRow = new HorizontalLayout();
        dateRow.setAlignItems(FlexComponent.Alignment.CENTER);
        Span dateLabel = new Span("Fecha devolución:");
        dateLabel.getStyle().set("font-weight", "bold").set("width", "120px");
        Span dateValue = new Span(rental.getEndDate().toString());
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
        confirmButton = new Button("Confirmar Devolución", VaadinIcon.CHECK.create());
        confirmButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);
        confirmButton.addClickListener(e -> confirmReturn());

        cancelButton = new Button("Cancelar", VaadinIcon.CLOSE.create());
        cancelButton.addClickListener(e -> close());

        HorizontalLayout footer = new HorizontalLayout(confirmButton, cancelButton);
        footer.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        footer.setWidthFull();
        footer.getStyle().set("padding", "1rem");

        getFooter().add(footer);
    }

    private void confirmReturn() {
        try {
            // Inicializar storage si es necesario
            storageInitializer.initializeIfNeeded();

            // 1. Cambiar estado de renta a COMPLETED
            boolean needsMaintenance = maintenanceCheckbox.getValue();
            rentalService.returnRental(rental.getId(), notesField.getValue(), needsMaintenance);

            // 2. Subir fotos pendientes
            uploadPendingPhotos();

            // 3. Mostrar éxito y cerrar
            fireEvent(new ReturnConfirmedEvent(this));
            showSuccessNotification("Devolución registrada exitosamente");
            close();

        } catch (Exception e) {
            log.error("Error al registrar devolución: {}", e.getMessage(), e);
            showErrorNotification("Error al registrar devolución: " + e.getMessage());
        }
    }

    private void uploadPendingPhotos() {
        Map<RentalPhotoType, List<PhotoUploadPanel.PendingUpload>> allUploads =
            photoPanel.getAllPendingUploads();

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
                    log.info("Foto de devolución subida: {} - {}", photoType, upload.getFileName());
                } catch (Exception e) {
                    log.error("Error subiendo foto de devolución: {}", e.getMessage(), e);
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
    public static class ReturnConfirmedEvent extends ComponentEvent<ReturnDialog> {
        public ReturnConfirmedEvent(ReturnDialog source) {
            super(source, false);
        }
    }

    public Registration addReturnConfirmedListener(ComponentEventListener<ReturnConfirmedEvent> listener) {
        return addListener(ReturnConfirmedEvent.class, listener);
    }
}
