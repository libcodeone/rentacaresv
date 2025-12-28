package com.rentacaresv.rental.ui;

import com.rentacaresv.rental.application.RentalDTO;
import com.rentacaresv.rental.application.RentalService;
import com.rentacaresv.shared.util.FormatUtils;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;


/**
 * Diálogo para mostrar detalles completos de una renta
 */
public class RentalDetailsDialog extends Dialog {

    private final RentalService rentalService;
    private final RentalDTO rental;

    public RentalDetailsDialog(RentalService rentalService, RentalDTO rental) {
        this.rentalService = rentalService;
        this.rental = rental;
        
        configureDialog();
        createContent();
    }

    private void configureDialog() {
        setModal(true);
        setDraggable(false);
        setWidth("600px");
        setMaxHeight("90vh");
    }

    private void createContent() {
        H3 title = new H3("Detalles de Renta");
        title.getStyle().set("margin", "0 0 1rem 0");
        
        // Información del contrato
        VerticalLayout contractInfo = createSection("📄 Información del Contrato",
            createInfoRow("Número de Contrato", rental.getContractNumber()),
            createInfoRow("Estado", rental.getStatusLabel()),
            createInfoRow("Fecha de Inicio", FormatUtils.formatDate(rental.getStartDate())),
            createInfoRow("Fecha de Fin", FormatUtils.formatDate(rental.getEndDate())),
            createInfoRow("Días Totales", rental.getTotalDays().toString())
        );
        
        // Información del vehículo
        VerticalLayout vehicleInfo = createSection("🚗 Vehículo",
            createInfoRow("Placa", rental.getVehicleLicensePlate()),
            createInfoRow("Descripción", rental.getVehicleDescription())
        );
        
        // Información del cliente
        VerticalLayout customerInfo = createSection("👤 Cliente",
            createInfoRow("Nombre", rental.getCustomerName()),
            createInfoRow("Documento", rental.getCustomerDocument()),
            createInfoRow("Categoría", rental.getCustomerIsVip() ? "VIP ⭐" : "Normal")
        );
        
        // Información financiera
        VerticalLayout financialInfo = createSection("💰 Información Financiera",
            createInfoRow("Tarifa Diaria", FormatUtils.formatPrice(rental.getDailyRate())),
            createInfoRow("Total", FormatUtils.formatPrice(rental.getTotalAmount())),
            createInfoRow("Pagado", FormatUtils.formatPrice(rental.getAmountPaid())),
            createInfoRow("Saldo", FormatUtils.formatPrice(rental.getBalance()))
        );
        
        // Información de viaje (solo si existe)
        VerticalLayout travelInfo = null;
        if (rental.getIsTouristRental()) {
            travelInfo = createTravelSection();
        }
        
        // Notas (si existen)
        VerticalLayout notesSection = null;
        if (rental.getNotes() != null && !rental.getNotes().isBlank()) {
            notesSection = createSection("📝 Notas",
                createNoteText(rental.getNotes())
            );
        }
        
        // Layout principal
        VerticalLayout content = new VerticalLayout(
            title,
            contractInfo,
            vehicleInfo,
            customerInfo,
            financialInfo
        );
        
        if (travelInfo != null) {
            content.add(travelInfo);
        }
        
        if (notesSection != null) {
            content.add(notesSection);
        }
        
        content.setPadding(true);
        content.setSpacing(true);
        
        add(content);
    }

    private VerticalLayout createSection(String title, Div... rows) {
        H3 sectionTitle = new H3(title);
        sectionTitle.getStyle()
            .set("font-size", "1.1rem")
            .set("margin", "0.5rem 0")
            .set("color", "var(--lumo-primary-color)");
        
        VerticalLayout section = new VerticalLayout();
        section.setPadding(false);
        section.setSpacing(false);
        section.add(sectionTitle);
        
        for (Div row : rows) {
            section.add(row);
        }
        
        section.getStyle()
            .set("padding", "1rem")
            .set("background", "var(--lumo-contrast-5pct)")
            .set("border-radius", "var(--lumo-border-radius-m)")
            .set("margin-bottom", "0.5rem");
        
        return section;
    }

    private VerticalLayout createTravelSection() {
        Span sectionTitle = new Span("✈️ Información de Viaje");
        sectionTitle.getStyle()
            .set("font-size", "1.1rem")
            .set("font-weight", "bold")
            .set("color", "var(--lumo-primary-color)");
        
        VerticalLayout content = new VerticalLayout();
        content.setPadding(false);
        content.setSpacing(true);
        
        if (rental.getFlightNumber() != null && !rental.getFlightNumber().isBlank()) {
            content.add(createInfoRowWithIcon(VaadinIcon.AIRPLANE, "Número de Vuelo", rental.getFlightNumber()));
        }
        
        if (rental.getAccommodation() != null && !rental.getAccommodation().isBlank()) {
            content.add(createInfoRowWithIcon(VaadinIcon.BED, "Hospedaje", rental.getAccommodation()));
        }
        
        if (rental.getContactPhone() != null && !rental.getContactPhone().isBlank()) {
            content.add(createInfoRowWithIcon(VaadinIcon.PHONE, "Teléfono de Contacto", rental.getContactPhone()));
        }
        
        if (rental.getTravelItinerary() != null && !rental.getTravelItinerary().isBlank()) {
            Span itineraryLabel = new Span("📍 Itinerario:");
            itineraryLabel.getStyle()
                .set("font-weight", "bold")
                .set("display", "block")
                .set("margin-bottom", "0.5rem");
            
            Span itineraryText = new Span(rental.getTravelItinerary());
            itineraryText.getStyle()
                .set("white-space", "pre-wrap")
                .set("padding", "0.5rem")
                .set("background", "var(--lumo-contrast-5pct)")
                .set("border-radius", "var(--lumo-border-radius-s)")
                .set("display", "block");
            
            content.add(itineraryLabel, itineraryText);
        }
        
        Details travelDetails = new Details("", content);
        travelDetails.setSummaryText("✈️ Información de Viaje (Cliente Turista)");
        travelDetails.setOpened(true);
        travelDetails.getStyle()
            .set("padding", "1rem")
            .set("background", "var(--lumo-primary-color-10pct)")
            .set("border", "1px solid var(--lumo-primary-color-50pct)")
            .set("border-radius", "var(--lumo-border-radius-m)")
            .set("margin-bottom", "0.5rem");
        
        VerticalLayout wrapper = new VerticalLayout(travelDetails);
        wrapper.setPadding(false);
        
        return wrapper;
    }

    private Div createInfoRow(String label, String value) {
        Span labelSpan = new Span(label + ":");
        labelSpan.getStyle()
            .set("font-weight", "bold")
            .set("min-width", "150px");
        
        Span valueSpan = new Span(value != null ? value : "-");
        
        HorizontalLayout row = new HorizontalLayout(labelSpan, valueSpan);
        row.setAlignItems(HorizontalLayout.Alignment.BASELINE);
        row.getStyle().set("padding", "0.25rem 0");
        
        Div wrapper = new Div(row);
        return wrapper;
    }

    private Div createInfoRowWithIcon(VaadinIcon icon, String label, String value) {
        Span iconSpan = new Span(icon.create());
        iconSpan.getStyle().set("margin-right", "0.5rem");
        
        Span labelSpan = new Span(label + ":");
        labelSpan.getStyle()
            .set("font-weight", "bold")
            .set("min-width", "150px");
        
        Span valueSpan = new Span(value != null ? value : "-");
        
        HorizontalLayout row = new HorizontalLayout(iconSpan, labelSpan, valueSpan);
        row.setAlignItems(HorizontalLayout.Alignment.BASELINE);
        row.getStyle().set("padding", "0.25rem 0");
        
        Div wrapper = new Div(row);
        return wrapper;
    }

    private Div createNoteText(String text) {
        Span noteSpan = new Span(text);
        noteSpan.getStyle()
            .set("white-space", "pre-wrap")
            .set("display", "block");
        
        Div wrapper = new Div(noteSpan);
        return wrapper;
    }

    // Evento de actualización
    public static class RefreshEvent extends ComponentEvent<RentalDetailsDialog> {
        public RefreshEvent(RentalDetailsDialog source) {
            super(source, false);
        }
    }
}
