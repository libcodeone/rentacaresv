package com.rentacaresv.contract.ui;

import com.rentacaresv.contract.domain.AccessoryCatalog;
import com.rentacaresv.contract.domain.AccessoryCategory;
import com.rentacaresv.contract.infrastructure.AccessoryCatalogRepository;
import com.rentacaresv.views.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import lombok.extern.slf4j.Slf4j;
import org.vaadin.lineawesome.LineAwesomeIconUrl;

/**
 * Vista para gestionar el catálogo de accesorios de los contratos.
 * Los accesorios son items que se verifican al momento de entregar el vehículo
 * (ej: llanta de repuesto, gato hidráulico, extintor, etc.)
 */
@PageTitle("Catálogo de Accesorios")
@Route(value = "contract-catalog", layout = MainLayout.class)
@Menu(order = 8, icon = LineAwesomeIconUrl.LIST_ALT_SOLID)
@RolesAllowed("ADMIN")
@Slf4j
public class ContractCatalogView extends VerticalLayout {

    private final AccessoryCatalogRepository accessoryRepository;
    private Grid<AccessoryCatalog> accessoryGrid;

    public ContractCatalogView(AccessoryCatalogRepository accessoryRepository) {
        this.accessoryRepository = accessoryRepository;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        H2 title = new H2("Catálogo de Accesorios");
        title.getStyle().set("margin-top", "0");

        Paragraph description = new Paragraph(
            "Gestione los accesorios que se verifican al momento de entregar el vehículo. " +
            "Estos items aparecerán en el checklist del contrato digital.");
        description.getStyle().set("color", "var(--lumo-secondary-text-color)");

        // Botón agregar
        Button addButton = new Button("Agregar Accesorio", VaadinIcon.PLUS.create());
        addButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        addButton.addClickListener(e -> openAccessoryDialog(null));

        HorizontalLayout toolbar = new HorizontalLayout(addButton);
        toolbar.setWidthFull();
        toolbar.setJustifyContentMode(FlexComponent.JustifyContentMode.END);

        // Grid de accesorios
        accessoryGrid = new Grid<>(AccessoryCatalog.class, false);
        
        // Columna de Acciones (PRIMERA - FIJA)
        accessoryGrid.addComponentColumn(this::createAccessoryActions)
                .setHeader("Acciones")
                .setWidth("120px")
                .setFlexGrow(0)
                .setFrozen(true);
        
        accessoryGrid.addColumn(AccessoryCatalog::getName)
                .setHeader("Nombre")
                .setFlexGrow(2)
                .setSortable(true);
        
        accessoryGrid.addColumn(acc -> acc.getCategory().name())
                .setHeader("Categoría")
                .setFlexGrow(1)
                .setSortable(true);
        
        accessoryGrid.addColumn(AccessoryCatalog::getDisplayOrder)
                .setHeader("Orden")
                .setWidth("80px")
                .setSortable(true);
        
        accessoryGrid.addComponentColumn(acc -> {
            if (acc.getIsActive()) {
                return createBadge("Activo", "var(--lumo-success-color)", "var(--lumo-success-color-10pct)");
            } else {
                return createBadge("Inactivo", "var(--lumo-error-color)", "var(--lumo-error-color-10pct)");
            }
        }).setHeader("Estado").setWidth("100px");

        accessoryGrid.setItems(accessoryRepository.findAll());
        accessoryGrid.setSizeFull();

        add(title, description, toolbar, accessoryGrid);
        setFlexGrow(1, accessoryGrid);
    }

    private com.vaadin.flow.component.html.Span createBadge(String text, String color, String bgColor) {
        com.vaadin.flow.component.html.Span badge = new com.vaadin.flow.component.html.Span(text);
        badge.getStyle()
                .set("color", color)
                .set("background-color", bgColor)
                .set("padding", "0.25em 0.5em")
                .set("border-radius", "var(--lumo-border-radius-s)")
                .set("font-size", "var(--lumo-font-size-xs)")
                .set("font-weight", "500");
        return badge;
    }

    private HorizontalLayout createAccessoryActions(AccessoryCatalog accessory) {
        Button editBtn = new Button(VaadinIcon.EDIT.create());
        editBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        editBtn.setTooltipText("Editar");
        editBtn.addClickListener(e -> openAccessoryDialog(accessory));

        Button toggleBtn = new Button(accessory.getIsActive() ? VaadinIcon.EYE_SLASH.create() : VaadinIcon.EYE.create());
        toggleBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        toggleBtn.setTooltipText(accessory.getIsActive() ? "Desactivar" : "Activar");
        toggleBtn.addClickListener(e -> toggleAccessoryStatus(accessory));

        Button deleteBtn = new Button(VaadinIcon.TRASH.create());
        deleteBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);
        deleteBtn.setTooltipText("Eliminar");
        deleteBtn.addClickListener(e -> confirmDeleteAccessory(accessory));

        HorizontalLayout actions = new HorizontalLayout(editBtn, toggleBtn, deleteBtn);
        actions.setSpacing(false);
        return actions;
    }

    private void toggleAccessoryStatus(AccessoryCatalog accessory) {
        accessory.setIsActive(!accessory.getIsActive());
        accessoryRepository.save(accessory);
        refreshAccessoryGrid();
        
        String status = accessory.getIsActive() ? "activado" : "desactivado";
        Notification.show("Accesorio " + status, 2000, Notification.Position.BOTTOM_CENTER)
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }

    private void openAccessoryDialog(AccessoryCatalog accessory) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(accessory == null ? "Nuevo Accesorio" : "Editar Accesorio");
        dialog.setWidth("450px");

        TextField nameField = new TextField("Nombre");
        nameField.setWidthFull();
        nameField.setRequired(true);
        nameField.setPlaceholder("Ej: Llanta de repuesto");

        ComboBox<AccessoryCategory> categoryCombo = new ComboBox<>("Categoría");
        categoryCombo.setItems(AccessoryCategory.values());
        categoryCombo.setItemLabelGenerator(AccessoryCategory::name);
        categoryCombo.setWidthFull();
        categoryCombo.setRequired(true);

        IntegerField orderField = new IntegerField("Orden de visualización");
        orderField.setMin(0);
        orderField.setValue(0);
        orderField.setWidthFull();
        orderField.setHelperText("Los accesorios se muestran ordenados de menor a mayor");

        if (accessory != null) {
            nameField.setValue(accessory.getName());
            categoryCombo.setValue(accessory.getCategory());
            orderField.setValue(accessory.getDisplayOrder() != null ? accessory.getDisplayOrder() : 0);
        } else {
            // Obtener el siguiente orden disponible
            int maxOrder = accessoryRepository.findAll().stream()
                    .mapToInt(a -> a.getDisplayOrder() != null ? a.getDisplayOrder() : 0)
                    .max()
                    .orElse(0);
            orderField.setValue(maxOrder + 1);
        }

        FormLayout form = new FormLayout(nameField, categoryCombo, orderField);

        Button saveBtn = new Button("Guardar", e -> {
            if (nameField.isEmpty() || categoryCombo.isEmpty()) {
                Notification.show("Complete los campos requeridos", 3000, Notification.Position.BOTTOM_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }

            AccessoryCatalog acc = accessory != null ? accessory : new AccessoryCatalog();
            acc.setName(nameField.getValue().trim());
            acc.setCategory(categoryCombo.getValue());
            acc.setDisplayOrder(orderField.getValue());
            
            if (accessory == null) {
                acc.setIsActive(true);
            }

            accessoryRepository.save(acc);
            refreshAccessoryGrid();
            dialog.close();

            Notification.show("Accesorio guardado", 2000, Notification.Position.BOTTOM_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        });
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancelBtn = new Button("Cancelar", e -> dialog.close());

        dialog.add(form);
        dialog.getFooter().add(cancelBtn, saveBtn);
        dialog.open();
    }

    private void confirmDeleteAccessory(AccessoryCatalog accessory) {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Eliminar Accesorio");
        dialog.setText("¿Está seguro de eliminar el accesorio '" + accessory.getName() + "'?\n" +
                "Esta acción no se puede deshacer.");
        dialog.setCancelable(true);
        dialog.setCancelText("Cancelar");
        dialog.setConfirmText("Eliminar");
        dialog.setConfirmButtonTheme("error primary");
        dialog.addConfirmListener(e -> {
            accessoryRepository.delete(accessory);
            refreshAccessoryGrid();
            Notification.show("Accesorio eliminado", 2000, Notification.Position.BOTTOM_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        });
        dialog.open();
    }

    private void refreshAccessoryGrid() {
        accessoryGrid.setItems(accessoryRepository.findAll());
    }
}
