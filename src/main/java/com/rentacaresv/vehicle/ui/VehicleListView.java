package com.rentacaresv.vehicle.ui;

import com.rentacaresv.catalog.application.CatalogService;
import com.rentacaresv.shared.util.FormatUtils;
import com.rentacaresv.vehicle.application.VehicleDTO;
import com.rentacaresv.vehicle.application.VehiclePhotoService;
import com.rentacaresv.vehicle.application.VehicleService;
import com.rentacaresv.vehicle.domain.VehicleStatus;
import com.rentacaresv.views.MainLayout;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import org.vaadin.lineawesome.LineAwesomeIconUrl;

import java.util.List;

/**
 * Vista de gestión de vehículos
 * Permite listar, crear, editar y eliminar vehículos
 */
@Route(value = "vehicles", layout = MainLayout.class)
@PageTitle("Gestión de Vehículos")
@Menu(order = 1, icon = LineAwesomeIconUrl.CAR_SOLID)
@PermitAll
public class VehicleListView extends VerticalLayout {

    private final VehicleService vehicleService;
    private final VehiclePhotoService vehiclePhotoService;
    private final CatalogService catalogService;
    
    private Grid<VehicleDTO> grid;
    private TextField searchField;
    private ComboBox<VehicleStatus> statusFilter;
    private Button addButton;

    public VehicleListView(VehicleService vehicleService, 
                          VehiclePhotoService vehiclePhotoService,
                          CatalogService catalogService) {
        this.vehicleService = vehicleService;
        this.vehiclePhotoService = vehiclePhotoService;
        this.catalogService = catalogService;
        
        setSizeFull();
        setPadding(true);
        
        add(
            createHeader(),
            createToolbar(),
            createGrid()
        );
        
        updateGrid();
    }

    private Component createHeader() {
        H2 title = new H2("Gestión de Vehículos");
        title.getStyle()
            .set("margin", "0")
            .set("font-size", "1.5rem");
        
        // Contador de vehículos disponibles
        long availableCount = vehicleService.countAvailableVehicles();
        Span counter = new Span(String.format("Disponibles: %d", availableCount));
        counter.getStyle()
            .set("color", "var(--lumo-success-text-color)")
            .set("font-weight", "bold")
            .set("padding", "0.5rem 1rem")
            .set("background", "var(--lumo-success-color-10pct)")
            .set("border-radius", "var(--lumo-border-radius-m)");
        
        HorizontalLayout header = new HorizontalLayout(title, counter);
        header.setWidthFull();
        header.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.getStyle()
            .set("padding-bottom", "1rem")
            .set("border-bottom", "1px solid var(--lumo-contrast-10pct)");
        
        return header;
    }

    private Component createToolbar() {
        // Campo de búsqueda
        searchField = new TextField();
        searchField.setPlaceholder("Buscar por placa, marca o modelo...");
        searchField.setPrefixComponent(VaadinIcon.SEARCH.create());
        searchField.setValueChangeMode(ValueChangeMode.LAZY);
        searchField.addValueChangeListener(e -> updateGrid());
        searchField.setWidth("300px");

        // Filtro por estado
        statusFilter = new ComboBox<>("Estado");
        statusFilter.setItems(VehicleStatus.values());
        statusFilter.setItemLabelGenerator(status -> {
            return switch (status) {
                case AVAILABLE -> "Disponible";
                case RENTED -> "Rentado";
                case MAINTENANCE -> "Mantenimiento";
                case OUT_OF_SERVICE -> "Fuera de servicio";
            };
        });
        statusFilter.setPlaceholder("Todos");
        statusFilter.setClearButtonVisible(true);
        statusFilter.addValueChangeListener(e -> updateGrid());
        statusFilter.setWidth("200px");

        // Botón nuevo vehículo
        addButton = new Button("Nuevo Vehículo", VaadinIcon.PLUS.create());
        addButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        addButton.addClickListener(e -> openVehicleDialog(null));

        HorizontalLayout toolbar = new HorizontalLayout(searchField, statusFilter, addButton);
        toolbar.setWidthFull();
        toolbar.setJustifyContentMode(FlexComponent.JustifyContentMode.START);
        toolbar.setAlignItems(FlexComponent.Alignment.END);
        toolbar.getStyle().set("padding", "1rem 0");

        return toolbar;
    }

    private Component createGrid() {
        grid = new Grid<>(VehicleDTO.class, false);
        grid.setSizeFull();
        
        // Columna de Foto
        grid.addComponentColumn(this::createThumbnail)
            .setHeader("Foto")
            .setWidth("80px")
            .setFlexGrow(0);
        
        // Columnas
        grid.addColumn(VehicleDTO::getLicensePlate)
            .setHeader("Placa")
            .setWidth("120px")
            .setFlexGrow(0);
        
        grid.addColumn(vehicle -> vehicle.getBrand() + " " + vehicle.getModel())
            .setHeader("Vehículo")
            .setAutoWidth(true);
        
        grid.addColumn(VehicleDTO::getYear)
            .setHeader("Año")
            .setWidth("80px")
            .setFlexGrow(0);
        
        grid.addColumn(VehicleDTO::getColor)
            .setHeader("Color")
            .setWidth("100px")
            .setFlexGrow(0);
        
        grid.addColumn(VehicleDTO::getPassengerCapacity)
            .setHeader("Pasajeros")
            .setWidth("100px")
            .setFlexGrow(0);
        
        grid.addColumn(vehicle -> FormatUtils.formatPrice(vehicle.getPriceNormal()))
            .setHeader("Precio/día")
            .setWidth("120px")
            .setFlexGrow(0);
        
        grid.addComponentColumn(vehicle -> createStatusBadge(vehicle.getStatus()))
            .setHeader("Estado")
            .setWidth("150px")
            .setFlexGrow(0);
        
        grid.addComponentColumn(this::createActionButtons)
            .setHeader("Acciones")
            .setWidth("150px")
            .setFlexGrow(0);
        
        // Estilo
        grid.addThemeVariants();
        grid.getStyle()
            .set("border", "1px solid var(--lumo-contrast-10pct)")
            .set("border-radius", "var(--lumo-border-radius-m)");

        return grid;
    }

    private Component createThumbnail(VehicleDTO vehicle) {
        String photoUrl = vehiclePhotoService.getPrimaryPhotoUrl(vehicle.getId());
        
        if (photoUrl != null) {
            Image thumbnail = new Image(photoUrl, "Vehículo");
            thumbnail.setWidth("60px");
            thumbnail.setHeight("45px");
            thumbnail.getStyle()
                .set("object-fit", "cover")
                .set("border-radius", "4px")
                .set("border", "1px solid var(--lumo-contrast-20pct)");
            return thumbnail;
        } else {
            // Placeholder bonito con ícono vectorial
            var icon = VaadinIcon.CAR.create();
            icon.setSize("40px");
            icon.getStyle()
                .set("color", "var(--lumo-contrast-50pct)");
            
            VerticalLayout placeholder = new VerticalLayout(icon);
            placeholder.setWidth("60px");
            placeholder.setHeight("45px");
            placeholder.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
            placeholder.setAlignItems(FlexComponent.Alignment.CENTER);
            placeholder.setPadding(false);
            placeholder.setSpacing(false);
            placeholder.getStyle()
                .set("background", "var(--lumo-contrast-5pct)")
                .set("border-radius", "4px")
                .set("border", "1px solid var(--lumo-contrast-20pct)");
            
            return placeholder;
        }
    }

    private Component createStatusBadge(String status) {
        Span badge = new Span(getStatusLabel(status));
        badge.getElement().getThemeList().add("badge");
        
        String color = switch (status) {
            case "AVAILABLE" -> "success";
            case "RENTED" -> "contrast";
            case "MAINTENANCE" -> "error";
            case "OUT_OF_SERVICE" -> "error";
            default -> "contrast";
        };
        
        badge.getElement().getThemeList().add(color);
        return badge;
    }

    private String getStatusLabel(String status) {
        return switch (status) {
            case "AVAILABLE" -> "Disponible";
            case "RENTED" -> "Rentado";
            case "MAINTENANCE" -> "Mantenimiento";
            case "OUT_OF_SERVICE" -> "Fuera de servicio";
            default -> status;
        };
    }

    private Component createActionButtons(VehicleDTO vehicle) {
        Button editButton = new Button(VaadinIcon.EDIT.create());
        editButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        editButton.getElement().setAttribute("title", "Editar");
        editButton.addClickListener(e -> openVehicleDialog(vehicle));

        Button deleteButton = new Button(VaadinIcon.TRASH.create());
        deleteButton.addThemeVariants(
            ButtonVariant.LUMO_SMALL, 
            ButtonVariant.LUMO_TERTIARY,
            ButtonVariant.LUMO_ERROR
        );
        deleteButton.getElement().setAttribute("title", "Eliminar");
        deleteButton.addClickListener(e -> deleteVehicle(vehicle));
        deleteButton.setEnabled(!vehicle.getStatus().equals("RENTED"));

        HorizontalLayout actions = new HorizontalLayout(editButton, deleteButton);
        actions.setSpacing(false);
        return actions;
    }

    private void openVehicleDialog(VehicleDTO vehicle) {
        VehicleFormDialog dialog = new VehicleFormDialog(
            vehicleService, vehiclePhotoService, catalogService, vehicle);
        dialog.addSaveListener(e -> {
            updateGrid();
            showSuccessNotification(
                vehicle == null ? "Vehículo creado exitosamente" : "Vehículo actualizado exitosamente"
            );
        });
        dialog.open();
    }

    private void deleteVehicle(VehicleDTO vehicle) {
        // Diálogo de confirmación
        com.vaadin.flow.component.confirmdialog.ConfirmDialog confirmDialog = 
            new com.vaadin.flow.component.confirmdialog.ConfirmDialog();
        
        confirmDialog.setHeader("Eliminar Vehículo");
        confirmDialog.setText(
            String.format("¿Está seguro de eliminar el vehículo %s %s (Placa: %s)?\n\n" +
                         "Esta acción no se puede deshacer.",
                vehicle.getBrand(), vehicle.getModel(), vehicle.getLicensePlate())
        );
        
        confirmDialog.setCancelable(true);
        confirmDialog.setCancelText("Cancelar");
        
        confirmDialog.setConfirmText("Eliminar");
        confirmDialog.setConfirmButtonTheme("error primary");
        
        confirmDialog.addConfirmListener(event -> {
            try {
                vehicleService.deleteVehicle(vehicle.getId());
                updateGrid();
                showSuccessNotification("Vehículo eliminado exitosamente");
            } catch (Exception e) {
                showErrorNotification("Error al eliminar: " + e.getMessage());
            }
        });
        
        confirmDialog.open();
    }

    private void updateGrid() {
        List<VehicleDTO> vehicles;
        
        VehicleStatus status = statusFilter.getValue();
        String searchTerm = searchField.getValue();
        
        if (status != null) {
            vehicles = vehicleService.findByStatus(status);
        } else {
            vehicles = vehicleService.findAll();
        }
        
        // Filtro de búsqueda local
        if (searchTerm != null && !searchTerm.isBlank()) {
            String lowerSearch = searchTerm.toLowerCase();
            vehicles = vehicles.stream()
                .filter(v -> 
                    v.getLicensePlate().toLowerCase().contains(lowerSearch) ||
                    v.getBrand().toLowerCase().contains(lowerSearch) ||
                    v.getModel().toLowerCase().contains(lowerSearch)
                )
                .toList();
        }
        
        grid.setItems(vehicles);
    }

    private void showSuccessNotification(String message) {
        Notification notification = Notification.show(message, 3000, Notification.Position.TOP_CENTER);
        notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }

    private void showErrorNotification(String message) {
        Notification notification = Notification.show(message, 5000, Notification.Position.TOP_CENTER);
        notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
    }
}
