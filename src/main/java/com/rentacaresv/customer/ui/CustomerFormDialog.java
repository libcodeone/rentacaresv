package com.rentacaresv.customer.ui;

import com.rentacaresv.customer.application.CreateCustomerCommand;
import com.rentacaresv.customer.application.CustomerDTO;
import com.rentacaresv.customer.application.CustomerService;
import com.rentacaresv.customer.domain.CustomerCategory;
import com.rentacaresv.customer.domain.DocumentType;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.shared.Registration;

import java.time.LocalDate;
import java.util.Locale;

/**
 * Diálogo para crear/editar clientes
 */
public class CustomerFormDialog extends Dialog {

    private final CustomerService customerService;
    private final BeanValidationBinder<CreateCustomerCommand> binder;
    private CreateCustomerCommand command;
    
    // Campos del formulario
    private TextField fullName;
    private ComboBox<DocumentType> documentType;
    private TextField documentNumber;
    private EmailField email;
    private TextField phone;
    private TextArea address;
    private DatePicker birthDate;
    
    // Licencia de conducir
    private TextField driverLicenseNumber;
    private TextField driverLicenseCountry;
    private DatePicker driverLicenseExpiry;
    
    private ComboBox<CustomerCategory> category;
    private TextArea notes;
    
    // Botones
    private Button saveButton;
    private Button cancelButton;
    
    private final boolean isEdit;

    public CustomerFormDialog(CustomerService customerService, CustomerDTO customerToEdit) {
        this.customerService = customerService;
        this.isEdit = customerToEdit != null;
        this.command = new CreateCustomerCommand();
        
        this.binder = new BeanValidationBinder<>(CreateCustomerCommand.class);
        
        configureDialog();
        createForm();
        configureButtons();
        
        if (isEdit) {
            populateForm(customerToEdit);
        }
    }

    private void configureDialog() {
        setModal(true);
        setDraggable(false);
        setResizable(false);
        setWidth("650px");
        setMaxHeight("90vh");
    }

    private void createForm() {
        H3 title = new H3(isEdit ? "Editar Cliente" : "Nuevo Cliente");
        title.getStyle().set("margin", "0 0 1rem 0");
        
        // Información personal
        fullName = new TextField("Nombre Completo");
        fullName.setPlaceholder("Ej: Juan Pérez García");
        fullName.setRequired(true);
        fullName.setMaxLength(200);
        
        documentType = new ComboBox<>("Tipo de Documento");
        documentType.setItems(DocumentType.values());
        documentType.setItemLabelGenerator(DocumentType::getLabel);
        documentType.setRequired(true);
        documentType.setValue(DocumentType.DUI); // Default
        
        documentNumber = new TextField("Número de Documento");
        documentNumber.setPlaceholder("Ej: 12345678-9");
        documentNumber.setRequired(true);
        documentNumber.setMaxLength(50);
        
        birthDate = new DatePicker("Fecha de Nacimiento");
        birthDate.setLocale(new Locale("es", "SV"));
        birthDate.setMax(LocalDate.now().minusYears(18)); // Mínimo 18 años
        birthDate.setPlaceholder("dd/MM/yyyy");
        
        // Licencia de conducir (REQUERIDO para rentar)
        driverLicenseNumber = new TextField("🚦 Número de Licencia");
        driverLicenseNumber.setPlaceholder("Ej: L-123456 o DUI-12345678-9");
        driverLicenseNumber.setRequired(true);
        driverLicenseNumber.setMaxLength(50);
        driverLicenseNumber.setHelperText("Requerido para rentar vehículos");
        
        driverLicenseCountry = new TextField("🌍 País Emisor (Código)");
        driverLicenseCountry.setPlaceholder("SLV");
        driverLicenseCountry.setValue("SLV"); // Default El Salvador
        driverLicenseCountry.setMaxLength(3);
        driverLicenseCountry.setHelperText("ISO 3166-1 (SLV, USA, MEX, etc.)");
        driverLicenseCountry.setPattern("[A-Z]{3}");
        driverLicenseCountry.setAllowedCharPattern("[A-Z]");
        
        driverLicenseExpiry = new DatePicker("📅 Vencimiento (Opcional)");
        driverLicenseExpiry.setLocale(new Locale("es", "SV"));
        driverLicenseExpiry.setPlaceholder("dd/MM/yyyy");
        driverLicenseExpiry.setHelperText("Informativo - no bloquea rentas");
        
        // Información de contacto
        email = new EmailField("Email");
        email.setPlaceholder("cliente@ejemplo.com");
        email.setMaxLength(100);
        email.setClearButtonVisible(true);
        
        phone = new TextField("Teléfono");
        phone.setPlaceholder("Ej: 7123-4567");
        phone.setMaxLength(20);
        phone.setClearButtonVisible(true);
        
        address = new TextArea("Dirección");
        address.setPlaceholder("Dirección completa del cliente...");
        address.setMaxLength(500);
        address.setHelperText("Máximo 500 caracteres");
        
        // Categoría
        category = new ComboBox<>("Categoría");
        category.setItems(CustomerCategory.values());
        category.setItemLabelGenerator(cat -> cat == CustomerCategory.VIP ? "VIP" : "Normal");
        category.setRequired(true);
        category.setValue(CustomerCategory.NORMAL); // Default
        category.setHelperText("VIP tiene precios especiales");
        
        notes = new TextArea("Notas");
        notes.setPlaceholder("Información adicional...");
        notes.setMaxLength(500);
        notes.setHelperText("Máximo 500 caracteres");
        
        // Layout del formulario
        FormLayout formLayout = new FormLayout();
        formLayout.setResponsiveSteps(
            new FormLayout.ResponsiveStep("0", 1),
            new FormLayout.ResponsiveStep("500px", 2)
        );
        
        // Organizar campos
        formLayout.add(fullName, 2);
        formLayout.add(documentType, documentNumber);
        formLayout.add(birthDate, category);
        
        // Sección de licencia (destacada)
        formLayout.add(driverLicenseNumber, 2);
        formLayout.add(driverLicenseCountry, driverLicenseExpiry);
        
        formLayout.add(email, phone);
        formLayout.add(address, 2);
        formLayout.add(notes, 2);
        
        // Binding
        binder.forField(fullName).bind("fullName");
        binder.forField(documentType)
            .withConverter(
                type -> type != null ? type.name() : null,
                name -> name != null ? DocumentType.valueOf(name) : null
            )
            .bind("documentType");
        binder.forField(documentNumber).bind("documentNumber");
        binder.forField(email).bind("email");
        binder.forField(phone).bind("phone");
        binder.forField(address).bind("address");
        binder.forField(birthDate).bind("birthDate");
        binder.forField(driverLicenseNumber).bind("driverLicenseNumber");
        binder.forField(driverLicenseCountry).bind("driverLicenseCountry");
        binder.forField(driverLicenseExpiry).bind("driverLicenseExpiry");
        binder.forField(category)
            .withConverter(
                cat -> cat != null ? cat.name() : null,
                name -> name != null ? CustomerCategory.valueOf(name) : null
            )
            .bind("category");
        binder.forField(notes).bind("notes");
        
        binder.readBean(command);
        
        // Layout principal
        VerticalLayout content = new VerticalLayout(title, formLayout);
        content.setPadding(true);
        content.setSpacing(true);
        
        add(content);
    }

    private void configureButtons() {
        saveButton = new Button("Guardar");
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveButton.addClickListener(e -> save());
        
        cancelButton = new Button("Cancelar");
        cancelButton.addClickListener(e -> close());
        
        HorizontalLayout buttons = new HorizontalLayout(saveButton, cancelButton);
        buttons.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        buttons.setWidthFull();
        buttons.getStyle().set("padding", "1rem");
        
        getFooter().add(buttons);
    }

    private void populateForm(CustomerDTO customer) {
        fullName.setValue(customer.getFullName());
        
        documentType.setValue(DocumentType.valueOf(customer.getDocumentType()));
        documentNumber.setValue(customer.getDocumentNumber());
        documentNumber.setReadOnly(true); // No se puede cambiar en edición
        
        if (customer.getEmail() != null) email.setValue(customer.getEmail());
        if (customer.getPhone() != null) phone.setValue(customer.getPhone());
        if (customer.getAddress() != null) address.setValue(customer.getAddress());
        if (customer.getBirthDate() != null) birthDate.setValue(customer.getBirthDate());
        
        // Licencia de conducir
        if (customer.getDriverLicenseNumber() != null) {
            driverLicenseNumber.setValue(customer.getDriverLicenseNumber());
        }
        if (customer.getDriverLicenseCountry() != null) {
            driverLicenseCountry.setValue(customer.getDriverLicenseCountry());
        }
        if (customer.getDriverLicenseExpiry() != null) {
            driverLicenseExpiry.setValue(customer.getDriverLicenseExpiry());
        }
        
        category.setValue(CustomerCategory.valueOf(customer.getCategory()));
        
        if (customer.getNotes() != null) notes.setValue(customer.getNotes());
    }

    private void save() {
        try {
            binder.writeBean(command);
            customerService.createCustomer(command);
            fireEvent(new SaveEvent(this));
            close();
        } catch (ValidationException e) {
            showErrorNotification("Por favor corrige los errores en el formulario");
        } catch (Exception e) {
            showErrorNotification("Error al guardar: " + e.getMessage());
        }
    }

    private void showErrorNotification(String message) {
        Notification notification = Notification.show(message, 5000, Notification.Position.TOP_CENTER);
        notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
    }

    // Eventos
    public static class SaveEvent extends ComponentEvent<CustomerFormDialog> {
        public SaveEvent(CustomerFormDialog source) {
            super(source, false);
        }
    }

    public Registration addSaveListener(ComponentEventListener<SaveEvent> listener) {
        return addListener(SaveEvent.class, listener);
    }
}
