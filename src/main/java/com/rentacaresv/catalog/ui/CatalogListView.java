package com.rentacaresv.catalog.ui;

import com.rentacaresv.catalog.application.CatalogService;
import com.rentacaresv.catalog.application.VehicleBrandDTO;
import com.rentacaresv.catalog.application.VehicleModelDTO;
import com.rentacaresv.vehicle.domain.VehicleType;
import com.rentacaresv.views.MainLayout;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import org.vaadin.lineawesome.LineAwesomeIconUrl;

import java.util.List;

/**
 * Vista de gestión de catálogos (Marcas y Modelos de vehículos)
 */
@Route(value = "catalogs", layout = MainLayout.class)
@PageTitle("Catálogos")
@Menu(order = 5, icon = LineAwesomeIconUrl.LIST_SOLID)
@RolesAllowed("ADMIN")
public class CatalogListView extends VerticalLayout {

    private final CatalogService catalogService;

    // Grids
    private Grid<VehicleBrandDTO> brandGrid;
    private Grid<VehicleModelDTO> modelGrid;

    // Filtros
    private ComboBox<VehicleBrandDTO> brandFilter;

    public CatalogListView(CatalogService catalogService) {
        this.catalogService = catalogService;

        setSizeFull();
        setPadding(true);

        add(createHeader(), createContent());
    }

    private Component createHeader() {
        H2 title = new H2("Catálogos de Vehículos");
        title.getStyle()
                .set("margin", "0")
                .set("font-size", "1.5rem");

        long brandCount = catalogService.countActiveBrands();
        long modelCount = catalogService.countActiveModels();

        Span brandCounter = new Span(String.format("Marcas: %d", brandCount));
        brandCounter.getStyle()
                .set("color", "var(--lumo-primary-text-color)")
                .set("font-weight", "bold")
                .set("padding", "0.5rem 1rem")
                .set("background", "var(--lumo-primary-color-10pct)")
                .set("border-radius", "var(--lumo-border-radius-m)");

        Span modelCounter = new Span(String.format("Modelos: %d", modelCount));
        modelCounter.getStyle()
                .set("color", "var(--lumo-success-text-color)")
                .set("font-weight", "bold")
                .set("padding", "0.5rem 1rem")
                .set("background", "var(--lumo-success-color-10pct)")
                .set("border-radius", "var(--lumo-border-radius-m)");

        HorizontalLayout counters = new HorizontalLayout(brandCounter, modelCounter);
        counters.setSpacing(true);

        HorizontalLayout header = new HorizontalLayout(title, counters);
        header.setWidthFull();
        header.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.getStyle()
                .set("padding-bottom", "1rem")
                .set("border-bottom", "1px solid var(--lumo-contrast-10pct)");

        return header;
    }

    private Component createContent() {
        TabSheet tabSheet = new TabSheet();
        tabSheet.setSizeFull();

        // Tab de Marcas
        VerticalLayout brandsTab = createBrandsTab();
        tabSheet.add(createTabWithIcon(VaadinIcon.COPYRIGHT, "Marcas"), brandsTab);

        // Tab de Modelos
        VerticalLayout modelsTab = createModelsTab();
        tabSheet.add(createTabWithIcon(VaadinIcon.CAR, "Modelos"), modelsTab);

        return tabSheet;
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

    // ========================================
    // TAB DE MARCAS
    // ========================================

    private VerticalLayout createBrandsTab() {
        VerticalLayout layout = new VerticalLayout();
        layout.setSizeFull();
        layout.setPadding(false);

        // Toolbar
        Button addBrandButton = new Button("Nueva Marca", VaadinIcon.PLUS.create());
        addBrandButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        addBrandButton.addClickListener(e -> openBrandDialog(null));

        HorizontalLayout toolbar = new HorizontalLayout(addBrandButton);
        toolbar.setWidthFull();
        toolbar.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        toolbar.getStyle().set("padding", "1rem 0");

        // Grid
        brandGrid = new Grid<>(VehicleBrandDTO.class, false);
        brandGrid.setSizeFull();

        brandGrid.addColumn(VehicleBrandDTO::getName)
                .setHeader("Nombre")
                .setAutoWidth(true)
                .setFlexGrow(1);

        brandGrid.addColumn(VehicleBrandDTO::getModelCount)
                .setHeader("Modelos")
                .setAutoWidth(true);

        brandGrid.addComponentColumn(this::createBrandStatusBadge)
                .setHeader("Estado")
                .setAutoWidth(true);

        brandGrid.addComponentColumn(this::createBrandActions)
                .setHeader("Acciones")
                .setAutoWidth(true);

        layout.add(toolbar, brandGrid);
        updateBrandGrid();

        return layout;
    }

    private Component createBrandStatusBadge(VehicleBrandDTO brand) {
        Span badge = new Span(brand.getActive() ? "Activo" : "Inactivo");
        badge.getElement().getThemeList().add("badge");
        badge.getElement().getThemeList().add(brand.getActive() ? "success" : "error");
        return badge;
    }

    private Component createBrandActions(VehicleBrandDTO brand) {
        Button editButton = new Button(VaadinIcon.EDIT.create());
        editButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        editButton.getElement().setAttribute("title", "Editar");
        editButton.addClickListener(e -> openBrandDialog(brand));

        Button toggleButton = new Button(brand.getActive() ? VaadinIcon.BAN.create() : VaadinIcon.CHECK.create());
        toggleButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        toggleButton.getElement().setAttribute("title", brand.getActive() ? "Desactivar" : "Activar");
        toggleButton.addClickListener(e -> toggleBrand(brand));

        Button deleteButton = new Button(VaadinIcon.TRASH.create());
        deleteButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);
        deleteButton.getElement().setAttribute("title", "Eliminar");
        deleteButton.addClickListener(e -> deleteBrand(brand));
        deleteButton.setEnabled(brand.getModelCount() == 0);

        HorizontalLayout actions = new HorizontalLayout(editButton, toggleButton, deleteButton);
        actions.setSpacing(false);
        return actions;
    }

    private void openBrandDialog(VehicleBrandDTO brand) {
        Dialog dialog = new Dialog();
        dialog.setModal(true);
        dialog.setWidth("450px");

        H3 title = new H3(brand == null ? "Nueva Marca" : "Editar Marca");
        title.getStyle().set("margin", "0 0 1rem 0");

        TextField nameField = new TextField("Nombre");
        nameField.setRequired(true);
        nameField.setWidthFull();

        TextField logoUrlField = new TextField("URL del Logo (opcional)");
        logoUrlField.setWidthFull();
        logoUrlField.setPlaceholder("https://...");

        if (brand != null) {
            nameField.setValue(brand.getName());
            if (brand.getLogoUrl() != null) logoUrlField.setValue(brand.getLogoUrl());
        }

        FormLayout form = new FormLayout(nameField, logoUrlField);

        Button saveButton = new Button("Guardar", VaadinIcon.CHECK.create());
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveButton.addClickListener(e -> {
            if (nameField.getValue().isBlank()) {
                showErrorNotification("El nombre es requerido");
                return;
            }

            try {
                if (brand == null) {
                    catalogService.createBrand(
                            nameField.getValue(),
                            logoUrlField.getValue()
                    );
                    showSuccessNotification("Marca creada exitosamente");
                } else {
                    catalogService.updateBrand(
                            brand.getId(),
                            nameField.getValue(),
                            logoUrlField.getValue()
                    );
                    showSuccessNotification("Marca actualizada exitosamente");
                }
                updateBrandGrid();
                updateModelBrandFilter();
                dialog.close();
            } catch (Exception ex) {
                showErrorNotification("Error: " + ex.getMessage());
            }
        });

        Button cancelButton = new Button("Cancelar", VaadinIcon.CLOSE.create());
        cancelButton.addClickListener(e -> dialog.close());

        HorizontalLayout buttons = new HorizontalLayout(saveButton, cancelButton);
        buttons.setJustifyContentMode(FlexComponent.JustifyContentMode.END);

        VerticalLayout content = new VerticalLayout(title, form, buttons);
        content.setPadding(true);

        dialog.add(content);
        dialog.open();
    }

    private void toggleBrand(VehicleBrandDTO brand) {
        try {
            catalogService.toggleBrandActive(brand.getId());
            updateBrandGrid();
            showSuccessNotification("Marca " + (brand.getActive() ? "desactivada" : "activada"));
        } catch (Exception e) {
            showErrorNotification("Error: " + e.getMessage());
        }
    }

    private void deleteBrand(VehicleBrandDTO brand) {
        ConfirmDialog confirmDialog = new ConfirmDialog();
        confirmDialog.setHeader("Eliminar Marca");
        confirmDialog.setText("¿Está seguro de eliminar la marca " + brand.getName() + "?");
        confirmDialog.setCancelable(true);
        confirmDialog.setCancelText("Cancelar");
        confirmDialog.setConfirmText("Eliminar");
        confirmDialog.setConfirmButtonTheme("error primary");

        confirmDialog.addConfirmListener(event -> {
            try {
                catalogService.deleteBrand(brand.getId());
                updateBrandGrid();
                updateModelBrandFilter();
                showSuccessNotification("Marca eliminada exitosamente");
            } catch (Exception e) {
                showErrorNotification("Error: " + e.getMessage());
            }
        });

        confirmDialog.open();
    }

    private void updateBrandGrid() {
        List<VehicleBrandDTO> brands = catalogService.findAllBrands();
        brandGrid.setItems(brands);
    }

    // ========================================
    // TAB DE MODELOS
    // ========================================

    private VerticalLayout createModelsTab() {
        VerticalLayout layout = new VerticalLayout();
        layout.setSizeFull();
        layout.setPadding(false);

        // Toolbar
        brandFilter = new ComboBox<>("Filtrar por Marca");
        brandFilter.setItemLabelGenerator(VehicleBrandDTO::getName);
        brandFilter.setClearButtonVisible(true);
        brandFilter.setPlaceholder("Todas");
        brandFilter.addValueChangeListener(e -> updateModelGrid());
        brandFilter.setWidth("250px");

        Button addModelButton = new Button("Nuevo Modelo", VaadinIcon.PLUS.create());
        addModelButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        addModelButton.addClickListener(e -> openModelDialog(null));

        HorizontalLayout toolbar = new HorizontalLayout(brandFilter, addModelButton);
        toolbar.setWidthFull();
        toolbar.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        toolbar.setAlignItems(FlexComponent.Alignment.END);
        toolbar.getStyle().set("padding", "1rem 0");

        // Grid
        modelGrid = new Grid<>(VehicleModelDTO.class, false);
        modelGrid.setSizeFull();

        modelGrid.addColumn(VehicleModelDTO::getBrandName)
                .setHeader("Marca")
                .setAutoWidth(true);

        modelGrid.addColumn(VehicleModelDTO::getName)
                .setHeader("Modelo")
                .setAutoWidth(true)
                .setFlexGrow(1);

        modelGrid.addColumn(VehicleModelDTO::getVehicleTypeLabel)
                .setHeader("Tipo")
                .setAutoWidth(true);

        modelGrid.addColumn(model -> {
            if (model.getYearStart() != null && model.getYearEnd() != null) {
                return model.getYearStart() + " - " + model.getYearEnd();
            } else if (model.getYearStart() != null) {
                return model.getYearStart() + " - Actual";
            }
            return "-";
        }).setHeader("Años").setAutoWidth(true);

        modelGrid.addComponentColumn(this::createModelStatusBadge)
                .setHeader("Estado")
                .setAutoWidth(true);

        modelGrid.addComponentColumn(this::createModelActions)
                .setHeader("Acciones")
                .setAutoWidth(true);

        layout.add(toolbar, modelGrid);
        updateModelBrandFilter();
        updateModelGrid();

        return layout;
    }

    private Component createModelStatusBadge(VehicleModelDTO model) {
        Span badge = new Span(model.getActive() ? "Activo" : "Inactivo");
        badge.getElement().getThemeList().add("badge");
        badge.getElement().getThemeList().add(model.getActive() ? "success" : "error");
        return badge;
    }

    private Component createModelActions(VehicleModelDTO model) {
        Button editButton = new Button(VaadinIcon.EDIT.create());
        editButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        editButton.getElement().setAttribute("title", "Editar");
        editButton.addClickListener(e -> openModelDialog(model));

        Button toggleButton = new Button(model.getActive() ? VaadinIcon.BAN.create() : VaadinIcon.CHECK.create());
        toggleButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        toggleButton.getElement().setAttribute("title", model.getActive() ? "Desactivar" : "Activar");
        toggleButton.addClickListener(e -> toggleModel(model));

        Button deleteButton = new Button(VaadinIcon.TRASH.create());
        deleteButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);
        deleteButton.getElement().setAttribute("title", "Eliminar");
        deleteButton.addClickListener(e -> deleteModel(model));

        HorizontalLayout actions = new HorizontalLayout(editButton, toggleButton, deleteButton);
        actions.setSpacing(false);
        return actions;
    }

    private void openModelDialog(VehicleModelDTO model) {
        Dialog dialog = new Dialog();
        dialog.setModal(true);
        dialog.setWidth("600px");

        H3 title = new H3(model == null ? "Nuevo Modelo" : "Editar Modelo");
        title.getStyle().set("margin", "0 0 1rem 0");

        ComboBox<VehicleBrandDTO> brandField = new ComboBox<>("Marca");
        brandField.setItems(catalogService.findActiveBrands());
        brandField.setItemLabelGenerator(VehicleBrandDTO::getName);
        brandField.setRequired(true);
        brandField.setWidthFull();

        TextField nameField = new TextField("Nombre del Modelo");
        nameField.setRequired(true);
        nameField.setWidthFull();

        ComboBox<VehicleType> typeField = new ComboBox<>("Tipo de Vehículo");
        typeField.setItems(VehicleType.values());
        typeField.setItemLabelGenerator(VehicleType::getLabel);
        typeField.setRequired(true);
        typeField.setWidthFull();

        IntegerField yearStartField = new IntegerField("Año Inicio");
        yearStartField.setMin(1900);
        yearStartField.setMax(2100);
        yearStartField.setStepButtonsVisible(true);

        IntegerField yearEndField = new IntegerField("Año Fin");
        yearEndField.setMin(1900);
        yearEndField.setMax(2100);
        yearEndField.setStepButtonsVisible(true);
        yearEndField.setHelperText("Dejar vacío si es modelo actual");

        if (model != null) {
            catalogService.findAllBrands().stream()
                    .filter(b -> b.getId().equals(model.getBrandId()))
                    .findFirst()
                    .ifPresent(brandField::setValue);
            nameField.setValue(model.getName());
            typeField.setValue(VehicleType.valueOf(model.getVehicleType()));
            if (model.getYearStart() != null) yearStartField.setValue(model.getYearStart());
            if (model.getYearEnd() != null) yearEndField.setValue(model.getYearEnd());
        }

        FormLayout form = new FormLayout();
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 2));
        form.add(brandField, 2);
        form.add(nameField, typeField);
        form.add(yearStartField, yearEndField);

        Button saveButton = new Button("Guardar", VaadinIcon.CHECK.create());
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveButton.addClickListener(e -> {
            if (brandField.getValue() == null) {
                showErrorNotification("Seleccione una marca");
                return;
            }
            if (nameField.getValue().isBlank()) {
                showErrorNotification("El nombre es requerido");
                return;
            }
            if (typeField.getValue() == null) {
                showErrorNotification("Seleccione el tipo de vehículo");
                return;
            }

            try {
                if (model == null) {
                    catalogService.createModel(
                            brandField.getValue().getId(),
                            nameField.getValue(),
                            typeField.getValue(),
                            yearStartField.getValue(),
                            yearEndField.getValue()
                    );
                    showSuccessNotification("Modelo creado exitosamente");
                } else {
                    catalogService.updateModel(
                            model.getId(),
                            brandField.getValue().getId(),
                            nameField.getValue(),
                            typeField.getValue(),
                            yearStartField.getValue(),
                            yearEndField.getValue()
                    );
                    showSuccessNotification("Modelo actualizado exitosamente");
                }
                updateModelGrid();
                updateBrandGrid();
                dialog.close();
            } catch (Exception ex) {
                showErrorNotification("Error: " + ex.getMessage());
            }
        });

        Button cancelButton = new Button("Cancelar", VaadinIcon.CLOSE.create());
        cancelButton.addClickListener(e -> dialog.close());

        HorizontalLayout buttons = new HorizontalLayout(saveButton, cancelButton);
        buttons.setJustifyContentMode(FlexComponent.JustifyContentMode.END);

        VerticalLayout content = new VerticalLayout(title, form, buttons);
        content.setPadding(true);

        dialog.add(content);
        dialog.open();
    }

    private void toggleModel(VehicleModelDTO model) {
        try {
            catalogService.toggleModelActive(model.getId());
            updateModelGrid();
            showSuccessNotification("Modelo " + (model.getActive() ? "desactivado" : "activado"));
        } catch (Exception e) {
            showErrorNotification("Error: " + e.getMessage());
        }
    }

    private void deleteModel(VehicleModelDTO model) {
        ConfirmDialog confirmDialog = new ConfirmDialog();
        confirmDialog.setHeader("Eliminar Modelo");
        confirmDialog.setText("¿Está seguro de eliminar el modelo " + model.getFullName() + "?");
        confirmDialog.setCancelable(true);
        confirmDialog.setCancelText("Cancelar");
        confirmDialog.setConfirmText("Eliminar");
        confirmDialog.setConfirmButtonTheme("error primary");

        confirmDialog.addConfirmListener(event -> {
            try {
                catalogService.deleteModel(model.getId());
                updateModelGrid();
                updateBrandGrid();
                showSuccessNotification("Modelo eliminado exitosamente");
            } catch (Exception e) {
                showErrorNotification("Error: " + e.getMessage());
            }
        });

        confirmDialog.open();
    }

    private void updateModelBrandFilter() {
        brandFilter.setItems(catalogService.findActiveBrands());
    }

    private void updateModelGrid() {
        List<VehicleModelDTO> models;
        VehicleBrandDTO selectedBrand = brandFilter.getValue();

        if (selectedBrand != null) {
            models = catalogService.findModelsByBrand(selectedBrand.getId());
        } else {
            models = catalogService.findAllModels();
        }

        modelGrid.setItems(models);
    }

    // ========================================
    // NOTIFICACIONES
    // ========================================

    private void showSuccessNotification(String message) {
        Notification notification = Notification.show(message, 3000, Notification.Position.TOP_CENTER);
        notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }

    private void showErrorNotification(String message) {
        Notification notification = Notification.show(message, 5000, Notification.Position.TOP_CENTER);
        notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
    }
}
