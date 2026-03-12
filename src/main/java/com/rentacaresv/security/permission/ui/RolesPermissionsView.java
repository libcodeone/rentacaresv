package com.rentacaresv.security.permission.ui;

import com.rentacaresv.security.permission.application.RolePermissionService;
import com.rentacaresv.security.permission.domain.Permission;
import com.rentacaresv.security.permission.domain.SystemRole;
import com.rentacaresv.views.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
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
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * Vista para gestión de roles y permisos del sistema
 */
@Route(value = "security/roles", layout = MainLayout.class)
@PageTitle("Roles y Permisos | RentaCarESV")
@Menu(order = 90, icon = "vaadin:key")
@RolesAllowed("ADMIN")
@Slf4j
public class RolesPermissionsView extends VerticalLayout {

    private final RolePermissionService rolePermissionService;
    private final Grid<SystemRole> rolesGrid;
    private SystemRole selectedRole;

    public RolesPermissionsView(RolePermissionService rolePermissionService) {
        this.rolePermissionService = rolePermissionService;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        // Header
        HorizontalLayout header = createHeader();
        add(header);

        // Grid de roles
        rolesGrid = createRolesGrid();
        add(rolesGrid);

        // Cargar datos
        refreshGrid();
    }

    private HorizontalLayout createHeader() {
        H3 title = new H3("Gestión de Roles y Permisos");
        title.getStyle().set("margin", "0");

        Button addButton = new Button("Nuevo Rol", VaadinIcon.PLUS.create());
        addButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        addButton.addClickListener(e -> openRoleDialog(null));

        HorizontalLayout header = new HorizontalLayout(title, addButton);
        header.setWidthFull();
        header.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        header.setAlignItems(FlexComponent.Alignment.CENTER);

        return header;
    }

    private Grid<SystemRole> createRolesGrid() {
        Grid<SystemRole> grid = new Grid<>(SystemRole.class, false);
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        grid.setHeight("400px");

        // Columna de color/badge
        grid.addComponentColumn(role -> {
            Span badge = new Span(role.getDisplayName());
            badge.getStyle()
                    .set("background-color", role.getColor())
                    .set("color", "white")
                    .set("padding", "4px 12px")
                    .set("border-radius", "12px")
                    .set("font-size", "14px")
                    .set("font-weight", "500");
            return badge;
        }).setHeader("Rol").setAutoWidth(true);

        grid.addColumn(SystemRole::getName).setHeader("Código").setAutoWidth(true);
        grid.addColumn(SystemRole::getDescription).setHeader("Descripción").setFlexGrow(1);

        grid.addColumn(role -> role.getPermissions() != null ? role.getPermissions().size() : 0)
                .setHeader("Permisos").setAutoWidth(true);

        // Columna de estado
        grid.addComponentColumn(role -> {
            Span status = new Span(role.getActive() ? "Activo" : "Inactivo");
            status.getStyle()
                    .set("color", role.getActive() ? "var(--lumo-success-color)" : "var(--lumo-error-color)")
                    .set("font-weight", "500");
            return status;
        }).setHeader("Estado").setAutoWidth(true);

        // Columna de tipo
        grid.addComponentColumn(role -> {
            if (Boolean.TRUE.equals(role.getIsSystemRole())) {
                Span badge = new Span("Sistema");
                badge.getStyle()
                        .set("background-color", "var(--lumo-contrast-10pct)")
                        .set("padding", "2px 8px")
                        .set("border-radius", "4px")
                        .set("font-size", "12px");
                return badge;
            }
            return new Span("");
        }).setHeader("Tipo").setAutoWidth(true);

        // Columna de acciones
        grid.addComponentColumn(role -> {
            HorizontalLayout actions = new HorizontalLayout();
            actions.setSpacing(true);
            actions.setPadding(false);

            Button permissionsBtn = new Button(new Icon(VaadinIcon.KEY));
            permissionsBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
            permissionsBtn.setTooltipText("Gestionar permisos");
            permissionsBtn.addClickListener(e -> openPermissionsDialog(role));

            Button editBtn = new Button(new Icon(VaadinIcon.EDIT));
            editBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
            editBtn.setTooltipText("Editar rol");
            editBtn.addClickListener(e -> openRoleDialog(role));

            actions.add(permissionsBtn, editBtn);

            if (role.canBeDeleted()) {
                Button deleteBtn = new Button(new Icon(VaadinIcon.TRASH));
                deleteBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR);
                deleteBtn.setTooltipText("Eliminar rol");
                deleteBtn.addClickListener(e -> confirmDeleteRole(role));
                actions.add(deleteBtn);
            }

            return actions;
        }).setHeader("Acciones").setAutoWidth(true);

        return grid;
    }

    private void refreshGrid() {
        List<SystemRole> roles = rolePermissionService.getAllRoles();
        rolesGrid.setItems(roles);
    }

    private void openRoleDialog(SystemRole role) {
        boolean isNew = role == null;
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(isNew ? "Nuevo Rol" : "Editar Rol");
        dialog.setWidth("500px");

        FormLayout form = new FormLayout();

        TextField nameField = new TextField("Nombre del Rol");
        nameField.setRequired(true);
        nameField.setHelperText("Se convertirá a mayúsculas y sin espacios");
        nameField.setEnabled(isNew); // Solo editable al crear

        TextField displayNameField = new TextField("Nombre a Mostrar");
        displayNameField.setRequired(true);

        TextArea descriptionField = new TextArea("Descripción");
        descriptionField.setMaxLength(500);

        TextField colorField = new TextField("Color (hex)");
        colorField.setPattern("#[0-9A-Fa-f]{6}");
        colorField.setPlaceholder("#3B82F6");
        colorField.setHelperText("Ej: #3B82F6 para azul");

        if (!isNew) {
            nameField.setValue(role.getName());
            displayNameField.setValue(role.getDisplayName());
            descriptionField.setValue(role.getDescription() != null ? role.getDescription() : "");
            colorField.setValue(role.getColor() != null ? role.getColor() : "#3B82F6");
        }

        form.add(nameField, displayNameField, descriptionField, colorField);
        form.setColspan(descriptionField, 2);

        // Botones
        Button saveBtn = new Button("Guardar", e -> {
            try {
                if (displayNameField.isEmpty()) {
                    Notification.show("El nombre a mostrar es requerido", 3000, Notification.Position.MIDDLE)
                            .addThemeVariants(NotificationVariant.LUMO_ERROR);
                    return;
                }

                String color = colorField.getValue();
                if (color == null || color.isEmpty()) {
                    color = "#3B82F6";
                }

                if (isNew) {
                    if (nameField.isEmpty()) {
                        Notification.show("El nombre del rol es requerido", 3000, Notification.Position.MIDDLE)
                                .addThemeVariants(NotificationVariant.LUMO_ERROR);
                        return;
                    }
                    rolePermissionService.createRole(
                            nameField.getValue(),
                            displayNameField.getValue(),
                            descriptionField.getValue(),
                            color
                    );
                    Notification.show("Rol creado exitosamente", 3000, Notification.Position.BOTTOM_END)
                            .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                } else {
                    rolePermissionService.updateRole(
                            role.getId(),
                            displayNameField.getValue(),
                            descriptionField.getValue(),
                            color
                    );
                    Notification.show("Rol actualizado exitosamente", 3000, Notification.Position.BOTTOM_END)
                            .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                }

                dialog.close();
                refreshGrid();
            } catch (Exception ex) {
                Notification.show("Error: " + ex.getMessage(), 5000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancelBtn = new Button("Cancelar", e -> dialog.close());

        dialog.add(form);
        dialog.getFooter().add(cancelBtn, saveBtn);
        dialog.open();
    }

    private void openPermissionsDialog(SystemRole role) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Permisos: " + role.getDisplayName());
        dialog.setWidth("700px");
        dialog.setHeight("600px");

        // Obtener permisos agrupados por categoría
        Map<String, List<Permission>> permissionsByCategory = rolePermissionService.getPermissionsByCategory();
        Set<Permission> currentPermissions = new HashSet<>(role.getPermissions());
        Set<Permission> selectedPermissions = new HashSet<>(currentPermissions);

        VerticalLayout content = new VerticalLayout();
        content.setPadding(false);
        content.setSpacing(false);

        // Checkbox para seleccionar todos
        Checkbox selectAll = new Checkbox("Seleccionar todos");
        selectAll.setValue(selectedPermissions.size() == Permission.values().length);
        selectAll.addValueChangeListener(e -> {
            if (e.isFromClient()) {
                selectedPermissions.clear();
                if (e.getValue()) {
                    selectedPermissions.addAll(Arrays.asList(Permission.values()));
                }
                // Actualizar todos los checkboxes de categorías
                content.getChildren()
                        .filter(c -> c instanceof Div)
                        .forEach(div -> {
                            ((Div) div).getChildren()
                                    .filter(c -> c instanceof Checkbox)
                                    .map(c -> (Checkbox) c)
                                    .forEach(cb -> cb.setValue(e.getValue()));
                        });
            }
        });
        selectAll.getStyle().set("font-weight", "bold").set("margin-bottom", "var(--lumo-space-m)");
        content.add(selectAll);

        // Crear sección por cada categoría
        for (Map.Entry<String, List<Permission>> entry : permissionsByCategory.entrySet()) {
            Div categorySection = new Div();
            categorySection.getStyle()
                    .set("border", "1px solid var(--lumo-contrast-20pct)")
                    .set("border-radius", "var(--lumo-border-radius-m)")
                    .set("padding", "var(--lumo-space-m)")
                    .set("margin-bottom", "var(--lumo-space-m)");

            H4 categoryTitle = new H4(entry.getKey());
            categoryTitle.getStyle().set("margin", "0 0 var(--lumo-space-s) 0");
            categorySection.add(categoryTitle);

            VerticalLayout permissionsList = new VerticalLayout();
            permissionsList.setPadding(false);
            permissionsList.setSpacing(false);

            for (Permission permission : entry.getValue()) {
                Checkbox checkbox = new Checkbox(permission.getDisplayName());
                checkbox.setValue(currentPermissions.contains(permission));
                checkbox.setTooltipText(permission.getDescription());
                checkbox.addValueChangeListener(e -> {
                    if (e.getValue()) {
                        selectedPermissions.add(permission);
                    } else {
                        selectedPermissions.remove(permission);
                    }
                    // Actualizar checkbox de seleccionar todos
                    selectAll.setValue(selectedPermissions.size() == Permission.values().length);
                });
                permissionsList.add(checkbox);
            }

            categorySection.add(permissionsList);
            content.add(categorySection);
        }

        Scroller scroller = new Scroller(content);
        scroller.setScrollDirection(Scroller.ScrollDirection.VERTICAL);
        scroller.setSizeFull();

        // Botones
        Button saveBtn = new Button("Guardar Permisos", e -> {
            try {
                rolePermissionService.setRolePermissions(role.getId(), selectedPermissions);
                Notification.show("Permisos actualizados exitosamente", 3000, Notification.Position.BOTTOM_END)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                dialog.close();
                refreshGrid();
            } catch (Exception ex) {
                Notification.show("Error: " + ex.getMessage(), 5000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancelBtn = new Button("Cancelar", e -> dialog.close());

        dialog.add(scroller);
        dialog.getFooter().add(cancelBtn, saveBtn);
        dialog.open();
    }

    private void confirmDeleteRole(SystemRole role) {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Eliminar Rol");
        dialog.setText("¿Está seguro de eliminar el rol \"" + role.getDisplayName() + "\"? Esta acción no se puede deshacer.");
        dialog.setCancelable(true);
        dialog.setCancelText("Cancelar");
        dialog.setConfirmText("Eliminar");
        dialog.setConfirmButtonTheme("error primary");

        dialog.addConfirmListener(e -> {
            try {
                rolePermissionService.deleteRole(role.getId());
                Notification.show("Rol eliminado exitosamente", 3000, Notification.Position.BOTTOM_END)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                refreshGrid();
            } catch (Exception ex) {
                Notification.show("Error: " + ex.getMessage(), 5000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });

        dialog.open();
    }
}
