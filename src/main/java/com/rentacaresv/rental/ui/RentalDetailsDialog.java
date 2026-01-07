package com.rentacaresv.rental.ui;

import com.rentacaresv.rental.application.RentalDTO;
import com.rentacaresv.rental.application.RentalPhotoService;
import com.rentacaresv.rental.application.RentalService;
import com.rentacaresv.rental.domain.photo.RentalPhoto;
import com.rentacaresv.rental.domain.photo.RentalPhotoType;
import com.rentacaresv.shared.util.FormatUtils;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabSheet;

import java.util.List;

/**
 * Diálogo para mostrar detalles completos de una renta
 * Incluye fotos de entrega y devolución (solo lectura)
 */
public class RentalDetailsDialog extends Dialog {

    private final RentalService rentalService;
    private final RentalPhotoService rentalPhotoService;
    private final RentalDTO rental;

    public RentalDetailsDialog(RentalService rentalService, RentalPhotoService rentalPhotoService, RentalDTO rental) {
        this.rentalService = rentalService;
        this.rentalPhotoService = rentalPhotoService;
        this.rental = rental;
        
        configureDialog();
        createContent();
    }
    
    public RentalDetailsDialog(RentalService rentalService, RentalDTO rental) {
        this(rentalService, null, rental);
    }

    private void configureDialog() {
        setModal(true);
        setDraggable(false);
        setWidth("1000px");
        setMaxHeight("90vh");
    }

    private void createContent() {
        // Header con icono
        HorizontalLayout titleLayout = new HorizontalLayout();
        titleLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        Icon titleIcon = VaadinIcon.FILE_TEXT.create();
        titleIcon.setSize("24px");
        titleIcon.getStyle().set("color", "var(--lumo-primary-color)");
        H3 title = new H3("Detalles de Renta");
        title.getStyle().set("margin", "0");
        titleLayout.add(titleIcon, title);
        titleLayout.getStyle().set("margin-bottom", "1rem");
        
        // TabSheet
        TabSheet tabSheet = new TabSheet();
        tabSheet.setSizeFull();
        
        // Tab 1: Información General
        VerticalLayout generalTab = createGeneralTab();
        tabSheet.add(createTabWithIcon(VaadinIcon.INFO_CIRCLE, "General"), generalTab);
        
        // Tab 2: Fotos de Entrega
        if (rentalPhotoService != null && hasDeliveryPhotos()) {
            VerticalLayout deliveryTab = createDeliveryPhotosTab();
            tabSheet.add(createTabWithIcon(VaadinIcon.CAMERA, "Fotos Entrega"), deliveryTab);
        }
        
        // Tab 3: Fotos de Devolución
        if (rentalPhotoService != null && hasReturnPhotos()) {
            VerticalLayout returnTab = createReturnPhotosTab();
            tabSheet.add(createTabWithIcon(VaadinIcon.CAMERA, "Fotos Devolución"), returnTab);
        }
        
        VerticalLayout content = new VerticalLayout(titleLayout, tabSheet);
        content.setPadding(true);
        content.setSpacing(true);
        content.setSizeFull();
        
        add(content);
    }

    private Tab createTabWithIcon(VaadinIcon iconType, String label) {
        HorizontalLayout tabContent = new HorizontalLayout();
        tabContent.setAlignItems(FlexComponent.Alignment.CENTER);
        tabContent.setSpacing(true);
        
        Icon icon = iconType.create();
        icon.setSize("16px");
        
        tabContent.add(icon);
        tabContent.add(new Span(label));
        
        return new Tab(tabContent);
    }

    private VerticalLayout createGeneralTab() {
        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(false);
        layout.setSpacing(true);
        
        // Información del contrato
        layout.add(createSection(VaadinIcon.FILE_TEXT, "Información del Contrato",
            createInfoRow("Número de Contrato", rental.getContractNumber()),
            createInfoRow("Estado", rental.getStatusLabel()),
            createInfoRow("Fecha de Inicio", FormatUtils.formatDate(rental.getStartDate())),
            createInfoRow("Fecha de Fin", FormatUtils.formatDate(rental.getEndDate())),
            createInfoRow("Días Totales", rental.getTotalDays().toString())
        ));
        
        // Información del vehículo
        layout.add(createSection(VaadinIcon.CAR, "Vehículo",
            createInfoRow("Placa", rental.getVehicleLicensePlate()),
            createInfoRow("Descripción", rental.getVehicleDescription())
        ));
        
        // Información del cliente
        layout.add(createSection(VaadinIcon.USER, "Cliente",
            createInfoRow("Nombre", rental.getCustomerName()),
            createInfoRow("Documento", rental.getCustomerDocument()),
            createInfoRow("Categoría", rental.getCustomerIsVip() ? "VIP" : "Normal")
        ));
        
        // Información financiera
        layout.add(createSection(VaadinIcon.MONEY, "Información Financiera",
            createInfoRow("Tarifa Diaria", FormatUtils.formatPrice(rental.getDailyRate())),
            createInfoRow("Total", FormatUtils.formatPrice(rental.getTotalAmount())),
            createInfoRow("Pagado", FormatUtils.formatPrice(rental.getAmountPaid())),
            createInfoRow("Saldo", FormatUtils.formatPrice(rental.getBalance()))
        ));
        
        // Información de viaje
        if (rental.getIsTouristRental()) {
            layout.add(createTravelSection());
        }
        
        // Notas
        if (rental.getNotes() != null && !rental.getNotes().isBlank()) {
            String[] noteSections = parseNotes(rental.getNotes());
            
            if (noteSections[0] != null) {
                layout.add(createNotesSection(VaadinIcon.CLIPBOARD_TEXT, "Notas Generales", noteSections[0]));
            }
            if (noteSections[1] != null) {
                layout.add(createNotesSection(VaadinIcon.CAR, "Notas de Entrega", noteSections[1]));
            }
            if (noteSections[2] != null) {
                layout.add(createNotesSection(VaadinIcon.SIGN_IN, "Notas de Devolución", noteSections[2]));
            }
        }
        
        return layout;
    }

    private VerticalLayout createDeliveryPhotosTab() {
        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(false);
        layout.setSpacing(true);
        
        HorizontalLayout titleLayout = new HorizontalLayout();
        titleLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        Icon icon = VaadinIcon.CAMERA.create();
        icon.setSize("18px");
        H4 title = new H4("Fotos de Entrega");
        title.getStyle().set("margin", "0");
        titleLayout.add(icon, title);
        titleLayout.getStyle().set("margin-bottom", "1rem");
        layout.add(titleLayout);
        
        List<RentalPhoto> deliveryPhotos = rentalPhotoService.getDeliveryPhotos(rental.getId());
        
        if (deliveryPhotos.isEmpty()) {
            Span noPhotos = new Span("No hay fotos de entrega");
            noPhotos.getStyle()
                .set("color", "var(--lumo-secondary-text-color)")
                .set("font-style", "italic");
            layout.add(noPhotos);
        } else {
            List<RentalPhoto> exterior = deliveryPhotos.stream()
                .filter(p -> p.getPhotoType() == RentalPhotoType.DELIVERY_EXTERIOR)
                .toList();
            List<RentalPhoto> interior = deliveryPhotos.stream()
                .filter(p -> p.getPhotoType() == RentalPhotoType.DELIVERY_INTERIOR)
                .toList();
            List<RentalPhoto> accessories = deliveryPhotos.stream()
                .filter(p -> p.getPhotoType() == RentalPhotoType.DELIVERY_ACCESSORIES)
                .toList();
            
            if (!exterior.isEmpty()) {
                layout.add(createPhotoGallery(VaadinIcon.CAR, "Exteriores", exterior.size(), exterior));
            }
            if (!interior.isEmpty()) {
                layout.add(createPhotoGallery(VaadinIcon.WORKPLACE, "Interiores", interior.size(), interior));
            }
            if (!accessories.isEmpty()) {
                layout.add(createPhotoGallery(VaadinIcon.TOOLS, "Accesorios", accessories.size(), accessories));
            }
        }
        
        return layout;
    }

    private VerticalLayout createReturnPhotosTab() {
        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(false);
        layout.setSpacing(true);
        
        HorizontalLayout titleLayout = new HorizontalLayout();
        titleLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        Icon icon = VaadinIcon.CAMERA.create();
        icon.setSize("18px");
        H4 title = new H4("Fotos de Devolución");
        title.getStyle().set("margin", "0");
        titleLayout.add(icon, title);
        titleLayout.getStyle().set("margin-bottom", "1rem");
        layout.add(titleLayout);
        
        List<RentalPhoto> returnPhotos = rentalPhotoService.getReturnPhotos(rental.getId());
        
        if (returnPhotos.isEmpty()) {
            Span noPhotos = new Span("No hay fotos de devolución");
            noPhotos.getStyle()
                .set("color", "var(--lumo-secondary-text-color)")
                .set("font-style", "italic");
            layout.add(noPhotos);
        } else {
            List<RentalPhoto> exterior = returnPhotos.stream()
                .filter(p -> p.getPhotoType() == RentalPhotoType.RETURN_EXTERIOR)
                .toList();
            List<RentalPhoto> interior = returnPhotos.stream()
                .filter(p -> p.getPhotoType() == RentalPhotoType.RETURN_INTERIOR)
                .toList();
            List<RentalPhoto> accessories = returnPhotos.stream()
                .filter(p -> p.getPhotoType() == RentalPhotoType.RETURN_ACCESSORIES)
                .toList();
            
            if (!exterior.isEmpty()) {
                layout.add(createPhotoGallery(VaadinIcon.CAR, "Exteriores", exterior.size(), exterior));
            }
            if (!interior.isEmpty()) {
                layout.add(createPhotoGallery(VaadinIcon.WORKPLACE, "Interiores", interior.size(), interior));
            }
            if (!accessories.isEmpty()) {
                layout.add(createPhotoGallery(VaadinIcon.TOOLS, "Accesorios", accessories.size(), accessories));
            }
        }
        
        return layout;
    }

    private VerticalLayout createPhotoGallery(VaadinIcon iconType, String title, int count, List<RentalPhoto> photos) {
        VerticalLayout section = new VerticalLayout();
        section.setPadding(false);
        section.setSpacing(true);
        
        HorizontalLayout titleLayout = new HorizontalLayout();
        titleLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        Icon icon = iconType.create();
        icon.setSize("16px");
        H4 sectionTitle = new H4(title + " (" + count + ")");
        sectionTitle.getStyle().set("margin", "0");
        titleLayout.add(icon, sectionTitle);
        section.add(titleLayout);
        
        FlexLayout gallery = new FlexLayout();
        gallery.setFlexWrap(FlexLayout.FlexWrap.WRAP);
        gallery.getStyle().set("gap", "1rem");
        
        for (RentalPhoto photo : photos) {
            Image img = new Image(photo.getPhotoUrl(), "Foto");
            img.setWidth("200px");
            img.setHeight("150px");
            img.getStyle()
                .set("object-fit", "cover")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("border", "2px solid var(--lumo-contrast-20pct)")
                .set("cursor", "pointer");
            
            img.addClickListener(e -> {
                Dialog imageDialog = new Dialog();
                Image fullImage = new Image(photo.getPhotoUrl(), "Foto completa");
                fullImage.setMaxWidth("90vw");
                fullImage.setMaxHeight("90vh");
                fullImage.getStyle().set("object-fit", "contain");
                imageDialog.add(fullImage);
                imageDialog.setModal(true);
                imageDialog.open();
            });
            
            gallery.add(img);
        }
        
        section.add(gallery);
        
        section.getStyle()
            .set("padding", "1rem")
            .set("background", "var(--lumo-contrast-5pct)")
            .set("border-radius", "var(--lumo-border-radius-m)");
        
        return section;
    }

    private boolean hasDeliveryPhotos() {
        try {
            return rentalPhotoService.hasDeliveryPhotos(rental.getId());
        } catch (Exception e) {
            return false;
        }
    }

    private boolean hasReturnPhotos() {
        try {
            return rentalPhotoService.hasReturnPhotos(rental.getId());
        } catch (Exception e) {
            return false;
        }
    }

    private String[] parseNotes(String notes) {
        String[] sections = new String[3];
        
        String[] parts = notes.split("=== ENTREGA ===|=== DEVOLUCIÓN ===");
        
        if (notes.contains("=== ENTREGA ===") && notes.contains("=== DEVOLUCIÓN ===")) {
            sections[0] = parts.length > 0 ? parts[0].trim() : null;
            sections[1] = parts.length > 1 ? parts[1].trim() : null;
            sections[2] = parts.length > 2 ? parts[2].trim() : null;
        } else if (notes.contains("=== ENTREGA ===")) {
            sections[0] = parts.length > 0 ? parts[0].trim() : null;
            sections[1] = parts.length > 1 ? parts[1].trim() : null;
        } else if (notes.contains("=== DEVOLUCIÓN ===")) {
            sections[0] = parts.length > 0 ? parts[0].trim() : null;
            sections[2] = parts.length > 1 ? parts[1].trim() : null;
        } else {
            sections[0] = notes.trim();
        }
        
        for (int i = 0; i < sections.length; i++) {
            if (sections[i] != null && sections[i].isBlank()) {
                sections[i] = null;
            }
        }
        
        return sections;
    }

    private VerticalLayout createSection(VaadinIcon iconType, String title, Div... rows) {
        HorizontalLayout titleLayout = new HorizontalLayout();
        titleLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        titleLayout.setSpacing(true);
        
        Icon icon = iconType.create();
        icon.setSize("18px");
        icon.getStyle().set("color", "var(--lumo-primary-color)");
        
        H4 sectionTitle = new H4(title);
        sectionTitle.getStyle()
            .set("font-size", "1.1rem")
            .set("margin", "0")
            .set("color", "var(--lumo-primary-color)");
        
        titleLayout.add(icon, sectionTitle);
        
        VerticalLayout section = new VerticalLayout();
        section.setPadding(false);
        section.setSpacing(false);
        section.add(titleLayout);
        
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

    private VerticalLayout createNotesSection(VaadinIcon iconType, String title, String text) {
        VerticalLayout section = new VerticalLayout();
        section.setPadding(false);
        section.setSpacing(true);
        
        HorizontalLayout titleLayout = new HorizontalLayout();
        titleLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        titleLayout.setSpacing(true);
        
        Icon icon = iconType.create();
        icon.setSize("18px");
        icon.getStyle().set("color", "var(--lumo-primary-color)");
        
        H4 sectionTitle = new H4(title);
        sectionTitle.getStyle()
            .set("font-size", "1.1rem")
            .set("margin", "0")
            .set("color", "var(--lumo-primary-color)");
        
        titleLayout.add(icon, sectionTitle);
        
        Span noteSpan = new Span(text);
        noteSpan.getStyle()
            .set("white-space", "pre-wrap")
            .set("display", "block");
        
        section.add(titleLayout, noteSpan);
        
        section.getStyle()
            .set("padding", "1rem")
            .set("background", "var(--lumo-contrast-5pct)")
            .set("border-radius", "var(--lumo-border-radius-m)")
            .set("margin-bottom", "0.5rem");
        
        return section;
    }

    private VerticalLayout createTravelSection() {
        HorizontalLayout titleLayout = new HorizontalLayout();
        titleLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        titleLayout.setSpacing(true);
        
        Icon icon = VaadinIcon.AIRPLANE.create();
        icon.setSize("18px");
        icon.getStyle().set("color", "var(--lumo-primary-color)");
        
        Span sectionTitle = new Span("Información de Viaje (Cliente Turista)");
        sectionTitle.getStyle()
            .set("font-size", "1.1rem")
            .set("font-weight", "bold")
            .set("color", "var(--lumo-primary-color)");
        
        titleLayout.add(icon, sectionTitle);
        
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
            HorizontalLayout itineraryLabel = new HorizontalLayout();
            itineraryLabel.setAlignItems(FlexComponent.Alignment.CENTER);
            Icon mapIcon = VaadinIcon.MAP_MARKER.create();
            mapIcon.setSize("14px");
            Span labelSpan = new Span("Itinerario:");
            labelSpan.getStyle().set("font-weight", "bold");
            itineraryLabel.add(mapIcon, labelSpan);
            itineraryLabel.getStyle().set("margin-bottom", "0.5rem");
            
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
        travelDetails.setSummary(titleLayout);
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
        row.setAlignItems(FlexComponent.Alignment.BASELINE);
        row.getStyle().set("padding", "0.25rem 0");
        
        Div wrapper = new Div(row);
        return wrapper;
    }

    private Div createInfoRowWithIcon(VaadinIcon iconType, String label, String value) {
        Icon icon = iconType.create();
        icon.setSize("14px");
        icon.getStyle().set("margin-right", "0.5rem");
        
        Span labelSpan = new Span(label + ":");
        labelSpan.getStyle()
            .set("font-weight", "bold")
            .set("min-width", "150px");
        
        Span valueSpan = new Span(value != null ? value : "-");
        
        HorizontalLayout row = new HorizontalLayout(icon, labelSpan, valueSpan);
        row.setAlignItems(FlexComponent.Alignment.CENTER);
        row.getStyle().set("padding", "0.25rem 0");
        
        Div wrapper = new Div(row);
        return wrapper;
    }

    public static class RefreshEvent extends ComponentEvent<RentalDetailsDialog> {
        public RefreshEvent(RentalDetailsDialog source) {
            super(source, false);
        }
    }
}
