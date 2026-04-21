package com.rentacaresv.rental.ui;

import com.rentacaresv.rental.application.RentalDTO;
import com.rentacaresv.rental.application.RentalService;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
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

/**
 * Diálogo para entregar vehículo al cliente.
 * - Cambia estado: PENDING → ACTIVE
 * - Guarda notas de entrega
 */
@Slf4j
public class DeliveryDialog extends Dialog {

    private final RentalService rentalService;
    private final RentalDTO rental;

    private TextArea notesField;
    private Button confirmButton;
    private Button cancelButton;

    public DeliveryDialog(RentalService rentalService, RentalDTO rental) {
        this.rentalService = rentalService;
        this.rental = rental;

        configureDialog();
        createContent();
        createFooter();
    }

    private void configureDialog() {
        setModal(true);
        setDraggable(false);
        setResizable(false);
        setWidth("550px");
    }

    private void createContent() {
        // Header
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
        Div separator = createSeparator();

        // Notas
        HorizontalLayout notesTitleLayout = new HorizontalLayout();
        notesTitleLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        Icon notesIcon = VaadinIcon.CLIPBOARD_TEXT.create();
        notesIcon.setSize("18px");
        H4 notesTitle = new H4("Notas de Entrega");
        notesTitle.getStyle().set("margin", "0");
        notesTitleLayout.add(notesIcon, notesTitle);
        notesTitleLayout.getStyle().set("margin", "0 0 0.5rem 0");

        notesField = new TextArea();
        notesField.setPlaceholder("Ej: Vehículo entregado en perfecto estado, tanque lleno, todos los accesorios incluidos...");
        notesField.setWidthFull();
        notesField.setMinHeight("100px");
        notesField.setMaxLength(1000);
        notesField.setHelperText("Opcional - Máximo 1000 caracteres");

        VerticalLayout content = new VerticalLayout(
                titleLayout,
                infoLayout,
                separator,
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

        HorizontalLayout vehicleRow = new HorizontalLayout();
        vehicleRow.setAlignItems(FlexComponent.Alignment.CENTER);
        Span vehicleLabel = new Span("Vehículo:");
        vehicleLabel.getStyle().set("font-weight", "bold").set("width", "130px");
        vehicleRow.add(vehicleLabel, new Span(rental.getVehicleDescription()));

        HorizontalLayout customerRow = new HorizontalLayout();
        customerRow.setAlignItems(FlexComponent.Alignment.CENTER);
        Span customerLabel = new Span("Cliente:");
        customerLabel.getStyle().set("font-weight", "bold").set("width", "130px");
        customerRow.add(customerLabel, new Span(rental.getCustomerName()));

        HorizontalLayout dateRow = new HorizontalLayout();
        dateRow.setAlignItems(FlexComponent.Alignment.CENTER);
        Span dateLabel = new Span("Fecha entrega:");
        dateLabel.getStyle().set("font-weight", "bold").set("width", "130px");
        dateRow.add(dateLabel, new Span(rental.getStartDate().toString()));

        layout.add(vehicleRow, customerRow, dateRow);
        return layout;
    }

    private Div createSeparator() {
        Div separator = new Div();
        separator.getStyle()
                .set("height", "1px")
                .set("background", "var(--lumo-contrast-10pct)")
                .set("margin", "0.5rem 0");
        return separator;
    }

    private void createFooter() {
        confirmButton = new Button("Confirmar Entrega", VaadinIcon.CHECK.create());
        confirmButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);
        confirmButton.addClickListener(e -> confirmDelivery());

        cancelButton = new Button("Cancelar", VaadinIcon.CLOSE.create());
        cancelButton.addClickListener(e -> close());

        HorizontalLayout footer = new HorizontalLayout(confirmButton, cancelButton);
        footer.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        footer.setWidthFull();
        footer.getStyle().set("padding", "1rem");

        getFooter().add(footer);
    }

    private void confirmDelivery() {
        try {
            rentalService.deliverRental(rental.getId(), notesField.getValue());
            fireEvent(new DeliveryConfirmedEvent(this));
            showSuccessNotification("Vehículo entregado exitosamente");
            close();
        } catch (Exception e) {
            log.error("Error al entregar vehículo: {}", e.getMessage(), e);
            showErrorNotification("Error al entregar vehículo: " + e.getMessage());
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
