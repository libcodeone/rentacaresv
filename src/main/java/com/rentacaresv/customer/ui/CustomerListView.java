package com.rentacaresv.customer.ui;

import com.rentacaresv.customer.application.CustomerDTO;
import com.rentacaresv.customer.application.CustomerService;
import com.rentacaresv.customer.domain.CustomerCategory;
import com.rentacaresv.views.MainLayout;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
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
 * Vista de gestión de clientes
 * Permite listar, crear, editar y eliminar clientes
 */
@Route(value = "customers", layout = MainLayout.class)
@PageTitle("Gestión de Clientes")
@Menu(order = 2, icon = LineAwesomeIconUrl.USER_FRIENDS_SOLID)
@PermitAll
public class CustomerListView extends VerticalLayout {

    private final CustomerService customerService;
    
    private Grid<CustomerDTO> grid;
    private TextField searchField;
    private ComboBox<CustomerCategory> categoryFilter;
    private Button addButton;

    public CustomerListView(CustomerService customerService) {
        this.customerService = customerService;
        
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
        H2 title = new H2("Gestión de Clientes");
        title.getStyle()
            .set("margin", "0")
            .set("font-size", "1.5rem");
        
        // Contador de clientes VIP
        long vipCount = customerService.countVipCustomers();
        long totalCount = customerService.countAllCustomers();
        
        Span vipCounter = new Span(String.format("VIP: %d", vipCount));
        vipCounter.getStyle()
            .set("color", "#7c5800")
            .set("font-weight", "bold")
            .set("padding", "0.5rem 1rem")
            .set("background", "var(--lumo-warning-color-10pct)")
            .set("border-radius", "var(--lumo-border-radius-m)");
        
        Span totalCounter = new Span(String.format("Total: %d", totalCount));
        totalCounter.getStyle()
            .set("color", "var(--lumo-primary-text-color)")
            .set("font-weight", "bold")
            .set("padding", "0.5rem 1rem")
            .set("background", "var(--lumo-primary-color-10pct)")
            .set("border-radius", "var(--lumo-border-radius-m)");
        
        HorizontalLayout counters = new HorizontalLayout(vipCounter, totalCounter);
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

    private Component createToolbar() {
        // Campo de búsqueda
        searchField = new TextField();
        searchField.setPlaceholder("Buscar por nombre, documento o teléfono...");
        searchField.setPrefixComponent(VaadinIcon.SEARCH.create());
        searchField.setValueChangeMode(ValueChangeMode.LAZY);
        searchField.addValueChangeListener(e -> updateGrid());
        searchField.setWidth("350px");

        // Filtro por categoría
        categoryFilter = new ComboBox<>("Categoría");
        categoryFilter.setItems(CustomerCategory.values());
        categoryFilter.setItemLabelGenerator(category -> 
            category == CustomerCategory.VIP ? "VIP" : "Normal"
        );
        categoryFilter.setPlaceholder("Todos");
        categoryFilter.setClearButtonVisible(true);
        categoryFilter.addValueChangeListener(e -> updateGrid());
        categoryFilter.setWidth("150px");

        // Botón nuevo cliente
        addButton = new Button("Nuevo Cliente", VaadinIcon.PLUS.create());
        addButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        addButton.addClickListener(e -> openCustomerDialog(null));

        HorizontalLayout toolbar = new HorizontalLayout(searchField, categoryFilter, addButton);
        toolbar.setWidthFull();
        toolbar.setJustifyContentMode(FlexComponent.JustifyContentMode.START);
        toolbar.setAlignItems(FlexComponent.Alignment.END);
        toolbar.getStyle().set("padding", "1rem 0");

        return toolbar;
    }

    private Component createGrid() {
        grid = new Grid<>(CustomerDTO.class, false);
        grid.setSizeFull();
        
        // Columnas
        grid.addColumn(CustomerDTO::getFullName)
            .setHeader("Nombre Completo")
            .setAutoWidth(true)
            .setFlexGrow(1);
        
        grid.addColumn(customer -> getDocumentTypeLabel(customer.getDocumentType()))
            .setHeader("Tipo Doc.")
            .setWidth("120px")
            .setFlexGrow(0);
        
        grid.addColumn(CustomerDTO::getDocumentNumber)
            .setHeader("Documento")
            .setWidth("140px")
            .setFlexGrow(0);
        
        grid.addColumn(CustomerDTO::getPhone)
            .setHeader("Teléfono")
            .setWidth("130px")
            .setFlexGrow(0);
        
        grid.addColumn(CustomerDTO::getEmail)
            .setHeader("Email")
            .setAutoWidth(true);
        
        grid.addColumn(CustomerDTO::getAge)
            .setHeader("Edad")
            .setWidth("80px")
            .setFlexGrow(0);
        
        grid.addComponentColumn(customer -> createCategoryBadge(customer.getCategory()))
            .setHeader("Categoría")
            .setWidth("120px")
            .setFlexGrow(0);
        
        grid.addComponentColumn(customer -> createStatusBadge(customer.getActive()))
            .setHeader("Estado")
            .setWidth("100px")
            .setFlexGrow(0);
        
        grid.addComponentColumn(this::createActionButtons)
            .setHeader("Acciones")
            .setWidth("200px")
            .setFlexGrow(0);
        
        // Estilo
        grid.addThemeVariants();
        grid.getStyle()
            .set("border", "1px solid var(--lumo-contrast-10pct)")
            .set("border-radius", "var(--lumo-border-radius-m)");

        return grid;
    }

    private Component createCategoryBadge(String category) {
        Span badge = new Span(category.equals("VIP") ? "VIP" : "Normal");
        badge.getElement().getThemeList().add("badge");
        
        if (category.equals("VIP")) {
            badge.getElement().getThemeList().add("warning");
            badge.getElement().setAttribute("title", "Cliente VIP - Precios especiales");
        } else {
            badge.getElement().getThemeList().add("contrast");
        }
        
        return badge;
    }

    private Component createStatusBadge(Boolean active) {
        Span badge = new Span(active ? "Activo" : "Inactivo");
        badge.getElement().getThemeList().add("badge");
        badge.getElement().getThemeList().add(active ? "success" : "error");
        return badge;
    }

    private String getDocumentTypeLabel(String type) {
        return switch (type) {
            case "DUI" -> "DUI";
            case "PASSPORT" -> "Pasaporte";
            case "DRIVERS_LICENSE" -> "Licencia";
            case "OTHER" -> "Otro";
            default -> type;
        };
    }

    private Component createActionButtons(CustomerDTO customer) {
        Button editButton = new Button(VaadinIcon.EDIT.create());
        editButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        editButton.getElement().setAttribute("title", "Editar");
        editButton.addClickListener(e -> openCustomerDialog(customer));

        Button vipButton = new Button(VaadinIcon.STAR.create());
        vipButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        if (customer.getIsVip()) {
            vipButton.getElement().setAttribute("title", "Quitar VIP");
            vipButton.getElement().getThemeList().add("warning");
            vipButton.addClickListener(e -> toggleVip(customer, false));
        } else {
            vipButton.getElement().setAttribute("title", "Hacer VIP");
            vipButton.addClickListener(e -> toggleVip(customer, true));
        }

        Button deleteButton = new Button(VaadinIcon.TRASH.create());
        deleteButton.addThemeVariants(
            ButtonVariant.LUMO_SMALL, 
            ButtonVariant.LUMO_TERTIARY,
            ButtonVariant.LUMO_ERROR
        );
        deleteButton.getElement().setAttribute("title", "Eliminar");
        deleteButton.addClickListener(e -> deleteCustomer(customer));

        HorizontalLayout actions = new HorizontalLayout(editButton, vipButton, deleteButton);
        actions.setSpacing(false);
        return actions;
    }

    private void openCustomerDialog(CustomerDTO customer) {
        CustomerFormDialog dialog = new CustomerFormDialog(customerService, customer);
        dialog.addSaveListener(e -> {
            updateGrid();
            showSuccessNotification(
                customer == null ? "Cliente creado exitosamente" : "Cliente actualizado exitosamente"
            );
        });
        dialog.open();
    }

    private void toggleVip(CustomerDTO customer, boolean makeVip) {
        try {
            if (makeVip) {
                customerService.promoteToVip(customer.getId());
                showSuccessNotification(customer.getFullName() + " promovido a VIP");
            } else {
                customerService.demoteToNormal(customer.getId());
                showSuccessNotification(customer.getFullName() + " cambiado a cliente Normal");
            }
            updateGrid();
        } catch (Exception e) {
            showErrorNotification("Error: " + e.getMessage());
        }
    }

    private void deleteCustomer(CustomerDTO customer) {
        // Diálogo de confirmación
        com.vaadin.flow.component.confirmdialog.ConfirmDialog confirmDialog = 
            new com.vaadin.flow.component.confirmdialog.ConfirmDialog();
        
        confirmDialog.setHeader("Eliminar Cliente");
        confirmDialog.setText(
            String.format("¿Está seguro de eliminar al cliente %s (Doc: %s)?\n\n" +
                         "Esta acción no se puede deshacer.",
                customer.getFullName(), customer.getDocumentNumber())
        );
        
        confirmDialog.setCancelable(true);
        confirmDialog.setCancelText("Cancelar");
        
        confirmDialog.setConfirmText("Eliminar");
        confirmDialog.setConfirmButtonTheme("error primary");
        
        confirmDialog.addConfirmListener(event -> {
            try {
                customerService.deleteCustomer(customer.getId());
                updateGrid();
                showSuccessNotification("Cliente eliminado exitosamente");
            } catch (Exception e) {
                showErrorNotification("Error al eliminar: " + e.getMessage());
            }
        });
        
        confirmDialog.open();
    }

    private void updateGrid() {
        List<CustomerDTO> customers;
        
        CustomerCategory category = categoryFilter.getValue();
        String searchTerm = searchField.getValue();
        
        if (category != null) {
            customers = customerService.findByCategory(category);
        } else {
            customers = customerService.findAll();
        }
        
        // Filtro de búsqueda local
        if (searchTerm != null && !searchTerm.isBlank()) {
            String lowerSearch = searchTerm.toLowerCase();
            customers = customers.stream()
                .filter(c -> 
                    c.getFullName().toLowerCase().contains(lowerSearch) ||
                    c.getDocumentNumber().toLowerCase().contains(lowerSearch) ||
                    (c.getPhone() != null && c.getPhone().contains(searchTerm)) ||
                    (c.getEmail() != null && c.getEmail().toLowerCase().contains(lowerSearch))
                )
                .toList();
        }
        
        grid.setItems(customers);
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
