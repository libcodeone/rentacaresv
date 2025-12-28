package com.rentacaresv.payment.ui;

import com.rentacaresv.payment.application.PaymentService;
import com.rentacaresv.payment.application.RegisterPaymentCommand;
import com.rentacaresv.payment.domain.PaymentMethod;
import com.rentacaresv.rental.application.RentalDTO;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.shared.Registration;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Diálogo para registrar pagos de una renta
 */
public class RegisterPaymentDialog extends Dialog {

    private final PaymentService paymentService;
    private final RentalDTO rental;
    private final BeanValidationBinder<RegisterPaymentCommand> binder;
    private RegisterPaymentCommand command;
    
    private NumberField amountField;
    private ComboBox<PaymentMethod> paymentMethodCombo;
    private TextField referenceField;
    private TextField cardDigitsField;
    private TextArea notesField;
    
    private Button saveButton;
    private Button cancelButton;

    public RegisterPaymentDialog(PaymentService paymentService, RentalDTO rental) {
        this.paymentService = paymentService;
        this.rental = rental;
        this.command = new RegisterPaymentCommand();
        this.binder = new BeanValidationBinder<>(RegisterPaymentCommand.class);
        
        configureDialog();
        createForm();
        configureButtons();
    }

    private void configureDialog() {
        setModal(true);
        setDraggable(false);
        setResizable(false);
        setWidth("500px");
    }

    private void createForm() {
        H3 title = new H3("Registrar Pago");
        
        // Información de la renta
        Span contractInfo = new Span("Contrato: " + rental.getContractNumber());
        contractInfo.getStyle().set("font-weight", "bold");
        
        Span balanceInfo = new Span("Saldo: " + formatPrice(rental.getBalance()));
        balanceInfo.getStyle()
            .set("font-weight", "bold")
            .set("font-size", "1.2rem")
            .set("color", "var(--lumo-error-color)");
        
        VerticalLayout infoLayout = new VerticalLayout(contractInfo, balanceInfo);
        infoLayout.setPadding(true);
        infoLayout.getStyle()
            .set("background", "var(--lumo-contrast-5pct)")
            .set("border-radius", "var(--lumo-border-radius-m)");
        
        // Campos del formulario
        amountField = new NumberField("Monto a Pagar");
        amountField.setPrefixComponent(new Span("$"));
        amountField.setMin(0.01);
        amountField.setMax(rental.getBalance().doubleValue());
        amountField.setStep(0.01);
        amountField.setRequired(true);
        amountField.setValue(rental.getBalance().doubleValue());
        
        paymentMethodCombo = new ComboBox<>("Método de Pago");
        paymentMethodCombo.setItems(PaymentMethod.values());
        paymentMethodCombo.setItemLabelGenerator(PaymentMethod::getLabel);
        paymentMethodCombo.setRequired(true);
        paymentMethodCombo.setValue(PaymentMethod.CASH);
        paymentMethodCombo.addValueChangeListener(e -> updateFieldsVisibility());
        
        referenceField = new TextField("Número de Referencia");
        referenceField.setPlaceholder("Ej: Autorización, No. Cheque");
        referenceField.setMaxLength(100);
        
        cardDigitsField = new TextField("Últimos 4 Dígitos");
        cardDigitsField.setPlaceholder("1234");
        cardDigitsField.setMaxLength(4);
        cardDigitsField.setVisible(false);
        
        notesField = new TextArea("Notas");
        notesField.setPlaceholder("Información adicional...");
        notesField.setMaxLength(500);
        
        FormLayout formLayout = new FormLayout();
        formLayout.setResponsiveSteps(
            new FormLayout.ResponsiveStep("0", 1)
        );
        
        formLayout.add(amountField, paymentMethodCombo, referenceField, cardDigitsField, notesField);
        
        // Binding
        binder.forField(amountField)
            .withConverter(
                value -> value != null ? BigDecimal.valueOf(value) : null,
                value -> value != null ? value.doubleValue() : null
            )
            .bind("amount");
        
        binder.forField(paymentMethodCombo)
            .withConverter(
                method -> method != null ? method.name() : null,
                name -> name != null ? PaymentMethod.valueOf(name) : null
            )
            .bind("paymentMethod");
        
        binder.forField(referenceField).bind("referenceNumber");
        binder.forField(cardDigitsField).bind("cardLastDigits");
        binder.forField(notesField).bind("notes");
        
        command.setRentalId(rental.getId());
        binder.readBean(command);
        
        VerticalLayout content = new VerticalLayout(title, infoLayout, formLayout);
        content.setPadding(true);
        content.setSpacing(true);
        
        add(content);
    }

    private void configureButtons() {
        saveButton = new Button("Registrar Pago");
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

    private void updateFieldsVisibility() {
        PaymentMethod method = paymentMethodCombo.getValue();
        
        if (method == PaymentMethod.CARD) {
            cardDigitsField.setVisible(true);
            referenceField.setLabel("Número de Autorización");
        } else if (method == PaymentMethod.CHECK) {
            cardDigitsField.setVisible(false);
            referenceField.setLabel("Número de Cheque");
        } else if (method == PaymentMethod.BANK_TRANSFER) {
            cardDigitsField.setVisible(false);
            referenceField.setLabel("Número de Transferencia");
        } else {
            cardDigitsField.setVisible(false);
            referenceField.setLabel("Número de Referencia");
        }
    }

    private void save() {
        try {
            binder.writeBean(command);
            paymentService.registerPayment(command);
            fireEvent(new SaveEvent(this));
            close();
        } catch (ValidationException e) {
            showErrorNotification("Por favor corrige los errores en el formulario");
        } catch (Exception e) {
            showErrorNotification("Error al registrar pago: " + e.getMessage());
        }
    }

    private String formatPrice(BigDecimal price) {
        if (price == null) return "$0.00";
        NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("es", "SV"));
        return formatter.format(price);
    }

    private void showErrorNotification(String message) {
        Notification notification = Notification.show(message, 5000, Notification.Position.TOP_CENTER);
        notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
    }

    // Eventos
    public static class SaveEvent extends ComponentEvent<RegisterPaymentDialog> {
        public SaveEvent(RegisterPaymentDialog source) {
            super(source, false);
        }
    }

    public Registration addSaveListener(ComponentEventListener<SaveEvent> listener) {
        return addListener(SaveEvent.class, listener);
    }
}
