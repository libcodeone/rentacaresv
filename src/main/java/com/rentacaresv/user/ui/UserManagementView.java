package com.rentacaresv.user.ui;

import com.rentacaresv.security.Role;
import com.rentacaresv.user.application.UserService;
import com.rentacaresv.user.domain.User;
import com.rentacaresv.views.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.CheckboxGroup;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import lombok.extern.slf4j.Slf4j;
import org.vaadin.lineawesome.LineAwesomeIconUrl;

import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Vista para gestión de usuarios del sistema
 */
@Route(value = "settings/users", layout = MainLayout.class)
@PageTitle("Gestión de Usuarios | RentaCarESV")
@Menu(order = 99.3, icon = LineAwesomeIconUrl.USERS_SOLID)
@RolesAllowed("ADMIN")
@Slf4j
public class UserManagementView extends VerticalLayout {

    private final UserService userService;
    private final Grid<User> grid;
    private TextField searchField;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public UserManagementView(UserService userService) {
        this.userService = userService;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        // Header
        add(createHeader());

        // Grid
        grid = createGrid();
        add(grid);

        // Cargar datos
        refreshGrid();
    }

    private HorizontalLayout createHeader() {
        H3 title = new H3("Gestión de Usuarios");
        title.getStyle().set("margin", "0");

        searchField = new TextField();
        searchField.setPlaceholder("Buscar usuarios...");
        searchField.setPrefixComponent(VaadinIcon.SEARCH.create());
        searchField.setClearButtonVisible(true);
        searchField.setValueChangeMode(ValueChangeMode.LAZY);
        searchField.setValueChangeTimeout(500);
        searchField.addValueChangeListener(e -> refreshGrid());
        searchField.setWidth("300px");

        Button addButton = new Button("Nuevo Usuario", VaadinIcon.PLUS.create());
        addButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        addButton.addClickListener(e -> openUserDialog(null));

        HorizontalLayout header = new HorizontalLayout(title, searchField, addButton);
        header.setWidthFull();
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.expand(searchField);
        header.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);

        return header;
    }

    private Grid<User> createGrid() {
        Grid<User> grid = new Grid<>(User.class, false);
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        grid.setSizeFull();

        // Columna de acciones (primera, frozen)
        grid.addColumn(new ComponentRenderer<>(user -> {
            HorizontalLayout actions = new HorizontalLayout();
            actions.setSpacing(true);
            actions.setPadding(false);

            // Botón editar
            Button editBtn = new Button(new Icon(VaadinIcon.EDIT));
            editBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
            editBtn.setTooltipText("Editar usuario");
            editBtn.addClickListener(e -> openUserDialog(user));

            // Botón cambiar contraseña
            Button passwordBtn = new Button(new Icon(VaadinIcon.KEY));
            passwordBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
            passwordBtn.setTooltipText("Cambiar contraseña");
            passwordBtn.addClickListener(e -> openChangePasswordDialog(user));

            // Botón activar/desactivar
            Button toggleBtn;
            if (user.getActive()) {
                toggleBtn = new Button(new Icon(VaadinIcon.BAN));
                toggleBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
                toggleBtn.setTooltipText("Desactivar usuario");
            } else {
                toggleBtn = new Button(new Icon(VaadinIcon.CHECK));
                toggleBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_SUCCESS);
                toggleBtn.setTooltipText("Activar usuario");
            }
            toggleBtn.addClickListener(e -> toggleUserActive(user));

            // Botón eliminar
            Button deleteBtn = new Button(new Icon(VaadinIcon.TRASH));
            deleteBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR);
            deleteBtn.setTooltipText("Eliminar usuario");
            deleteBtn.addClickListener(e -> confirmDeleteUser(user));

            actions.add(editBtn, passwordBtn, toggleBtn, deleteBtn);
            return actions;
        })).setHeader("Acciones").setAutoWidth(true).setFrozen(true);

        // Columna de estado (activo/inactivo)
        grid.addColumn(new ComponentRenderer<>(user -> {
            Icon icon;
            if (user.getActive()) {
                icon = VaadinIcon.CHECK_CIRCLE.create();
                icon.setColor("var(--lumo-success-color)");
                icon.setTooltipText("Usuario activo");
            } else {
                icon = VaadinIcon.CLOSE_CIRCLE.create();
                icon.setColor("var(--lumo-error-color)");
                icon.setTooltipText("Usuario inactivo");
            }
            return icon;
        })).setHeader("").setWidth("60px").setFlexGrow(0);

        // Columna de username
        grid.addColumn(User::getUsername).setHeader("Usuario").setAutoWidth(true).setSortable(true);

        // Columna de nombre completo
        grid.addColumn(User::getName).setHeader("Nombre Completo").setFlexGrow(1).setSortable(true);

        // Columna de email
        grid.addColumn(user -> user.getEmail() != null ? user.getEmail() : "-")
                .setHeader("Email").setAutoWidth(true);

        // Columna de roles
        grid.addColumn(new ComponentRenderer<>(user -> {
            HorizontalLayout rolesLayout = new HorizontalLayout();
            rolesLayout.setSpacing(true);
            rolesLayout.setPadding(false);

            if (user.getRoles() != null) {
                for (Role role : user.getRoles()) {
                    Span badge = new Span(role.name());
                    String bgColor = role == Role.ADMIN ? "#DC2626" : "#2563EB";
                    badge.getStyle()
                            .set("background-color", bgColor)
                            .set("color", "white")
                            .set("padding", "2px 8px")
                            .set("border-radius", "4px")
                            .set("font-size", "12px")
                            .set("font-weight", "500");
                    rolesLayout.add(badge);
                }
            }
            return rolesLayout;
        })).setHeader("Roles").setAutoWidth(true);

        // Columna de último login
        grid.addColumn(user -> user.getLastLogin() != null ? 
                user.getLastLogin().format(DATE_FORMATTER) : "Nunca")
                .setHeader("Último Acceso").setAutoWidth(true).setSortable(true);

        // Columna de fecha de creación
        grid.addColumn(user -> user.getCreatedAt() != null ? 
                user.getCreatedAt().format(DATE_FORMATTER) : "-")
                .setHeader("Creado").setAutoWidth(true).setSortable(true);

        return grid;
    }

    private void refreshGrid() {
        String search = searchField.getValue();
        if (search != null && !search.trim().isEmpty()) {
            // Filtrar en memoria por simplicidad
            grid.setItems(userService.getAllUsers().stream()
                    .filter(u -> u.getUsername().toLowerCase().contains(search.toLowerCase()) ||
                                 u.getName().toLowerCase().contains(search.toLowerCase()) ||
                                 (u.getEmail() != null && u.getEmail().toLowerCase().contains(search.toLowerCase())))
                    .collect(Collectors.toList()));
        } else {
            grid.setItems(userService.getAllUsers());
        }
    }

    private void openUserDialog(User user) {
        boolean isNew = user == null;
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(isNew ? "Nuevo Usuario" : "Editar Usuario");
        dialog.setWidth("500px");

        FormLayout form = new FormLayout();

        TextField usernameField = new TextField("Nombre de Usuario");
        usernameField.setRequired(true);
        usernameField.setEnabled(isNew); // Solo editable al crear
        usernameField.setHelperText("Solo letras, números y guiones bajos");

        TextField nameField = new TextField("Nombre Completo");
        nameField.setRequired(true);

        EmailField emailField = new EmailField("Email");
        emailField.setClearButtonVisible(true);

        PasswordField passwordField = new PasswordField("Contraseña");
        passwordField.setRequired(isNew);
        passwordField.setHelperText(isNew ? "Mínimo 6 caracteres" : "Dejar vacío para mantener la actual");

        PasswordField confirmPasswordField = new PasswordField("Confirmar Contraseña");
        confirmPasswordField.setRequired(isNew);

        // Checkbox de roles
        CheckboxGroup<Role> rolesGroup = new CheckboxGroup<>("Roles");
        rolesGroup.setItems(Role.values());
        rolesGroup.setItemLabelGenerator(Role::name);

        // Pre-llenar si es edición
        if (!isNew) {
            usernameField.setValue(user.getUsername());
            nameField.setValue(user.getName());
            emailField.setValue(user.getEmail() != null ? user.getEmail() : "");
            rolesGroup.setValue(new HashSet<>(user.getRoles()));
        } else {
            rolesGroup.setValue(Set.of(Role.OPERATOR)); // Por defecto
        }

        form.add(usernameField, nameField, emailField, passwordField, confirmPasswordField, rolesGroup);
        form.setColspan(rolesGroup, 2);

        // Botones
        Button saveBtn = new Button("Guardar", e -> {
            try {
                // Validaciones
                if (nameField.isEmpty()) {
                    Notification.show("El nombre es requerido", 3000, Notification.Position.MIDDLE)
                            .addThemeVariants(NotificationVariant.LUMO_ERROR);
                    return;
                }

                if (isNew && usernameField.isEmpty()) {
                    Notification.show("El nombre de usuario es requerido", 3000, Notification.Position.MIDDLE)
                            .addThemeVariants(NotificationVariant.LUMO_ERROR);
                    return;
                }

                Set<Role> selectedRoles = rolesGroup.getValue();
                if (selectedRoles.isEmpty()) {
                    Notification.show("Debe seleccionar al menos un rol", 3000, Notification.Position.MIDDLE)
                            .addThemeVariants(NotificationVariant.LUMO_ERROR);
                    return;
                }

                if (isNew) {
                    // Validar contraseña para nuevo usuario
                    if (passwordField.isEmpty()) {
                        Notification.show("La contraseña es requerida", 3000, Notification.Position.MIDDLE)
                                .addThemeVariants(NotificationVariant.LUMO_ERROR);
                        return;
                    }

                    if (passwordField.getValue().length() < 6) {
                        Notification.show("La contraseña debe tener al menos 6 caracteres", 3000, Notification.Position.MIDDLE)
                                .addThemeVariants(NotificationVariant.LUMO_ERROR);
                        return;
                    }

                    if (!passwordField.getValue().equals(confirmPasswordField.getValue())) {
                        Notification.show("Las contraseñas no coinciden", 3000, Notification.Position.MIDDLE)
                                .addThemeVariants(NotificationVariant.LUMO_ERROR);
                        return;
                    }

                    userService.createUser(
                            usernameField.getValue(),
                            nameField.getValue(),
                            emailField.getValue(),
                            passwordField.getValue(),
                            selectedRoles
                    );
                    Notification.show("Usuario creado exitosamente", 3000, Notification.Position.BOTTOM_END)
                            .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                } else {
                    userService.updateUser(
                            user.getId(),
                            nameField.getValue(),
                            emailField.getValue(),
                            selectedRoles
                    );
                    Notification.show("Usuario actualizado exitosamente", 3000, Notification.Position.BOTTOM_END)
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

    private void openChangePasswordDialog(User user) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Cambiar Contraseña: " + user.getUsername());
        dialog.setWidth("400px");

        FormLayout form = new FormLayout();

        PasswordField newPasswordField = new PasswordField("Nueva Contraseña");
        newPasswordField.setRequired(true);
        newPasswordField.setHelperText("Mínimo 6 caracteres");

        PasswordField confirmPasswordField = new PasswordField("Confirmar Contraseña");
        confirmPasswordField.setRequired(true);

        form.add(newPasswordField, confirmPasswordField);

        Button saveBtn = new Button("Cambiar Contraseña", e -> {
            try {
                if (newPasswordField.isEmpty()) {
                    Notification.show("La contraseña es requerida", 3000, Notification.Position.MIDDLE)
                            .addThemeVariants(NotificationVariant.LUMO_ERROR);
                    return;
                }

                if (newPasswordField.getValue().length() < 6) {
                    Notification.show("La contraseña debe tener al menos 6 caracteres", 3000, Notification.Position.MIDDLE)
                            .addThemeVariants(NotificationVariant.LUMO_ERROR);
                    return;
                }

                if (!newPasswordField.getValue().equals(confirmPasswordField.getValue())) {
                    Notification.show("Las contraseñas no coinciden", 3000, Notification.Position.MIDDLE)
                            .addThemeVariants(NotificationVariant.LUMO_ERROR);
                    return;
                }

                userService.changePassword(user.getId(), newPasswordField.getValue());
                Notification.show("Contraseña cambiada exitosamente", 3000, Notification.Position.BOTTOM_END)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                dialog.close();
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

    private void toggleUserActive(User user) {
        String action = user.getActive() ? "desactivar" : "activar";
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader((user.getActive() ? "Desactivar" : "Activar") + " Usuario");
        dialog.setText("¿Está seguro de " + action + " al usuario \"" + user.getUsername() + "\"?");
        dialog.setCancelable(true);
        dialog.setCancelText("Cancelar");
        dialog.setConfirmText(user.getActive() ? "Desactivar" : "Activar");
        dialog.setConfirmButtonTheme(user.getActive() ? "error primary" : "success primary");

        dialog.addConfirmListener(e -> {
            try {
                userService.toggleUserActive(user.getId());
                Notification.show("Usuario " + (user.getActive() ? "desactivado" : "activado") + " exitosamente", 
                        3000, Notification.Position.BOTTOM_END)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                refreshGrid();
            } catch (Exception ex) {
                Notification.show("Error: " + ex.getMessage(), 5000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });

        dialog.open();
    }

    private void confirmDeleteUser(User user) {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Eliminar Usuario");
        dialog.setText("¿Está seguro de eliminar al usuario \"" + user.getUsername() + "\"? Esta acción no se puede deshacer.");
        dialog.setCancelable(true);
        dialog.setCancelText("Cancelar");
        dialog.setConfirmText("Eliminar");
        dialog.setConfirmButtonTheme("error primary");

        dialog.addConfirmListener(e -> {
            try {
                userService.deleteUser(user.getId());
                Notification.show("Usuario eliminado exitosamente", 3000, Notification.Position.BOTTOM_END)
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
