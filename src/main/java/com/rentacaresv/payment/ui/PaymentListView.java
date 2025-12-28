package com.rentacaresv.payment.ui;

import com.rentacaresv.payment.application.PaymentDTO;
import com.rentacaresv.payment.application.PaymentService;
import com.rentacaresv.payment.domain.PaymentMethod;
import com.rentacaresv.payment.domain.PaymentStatus;
import com.rentacaresv.shared.util.FormatUtils;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Vista de gestión de pagos
 */
@Route(value = "payments", layout = MainLayout.class)
@PageTitle("Gestión de Pagos")
@Menu(order = 4, icon = LineAwesomeIconUrl.MONEY_BILL_WAVE_SOLID)
@PermitAll
public class PaymentListView extends VerticalLayout {

    private final PaymentService paymentService;
    
    private Grid<PaymentDTO> grid;
    private TextField searchField;
    private ComboBox<PaymentMethod> methodFilter;
    private ComboBox<PaymentStatus> statusFilter;

    public PaymentListView(PaymentService paymentService) {
        this.paymentService = paymentService;
        
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
        H2 title = new H2("Gestión de Pagos");
        title.getStyle()
            .set("margin", "0")
            .set("font-size", "1.5rem");
        
        // Calcular totales del día
        LocalDateTime today = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        LocalDateTime tomorrow = today.plusDays(1);
        BigDecimal todayIncome = paymentService.calculateTotalIncome(today, tomorrow);
        
        // Calcular totales del mes
        LocalDateTime monthStart = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        LocalDateTime monthEnd = monthStart.plusMonths(1);
        BigDecimal monthIncome = paymentService.calculateTotalIncome(monthStart, monthEnd);
        
        Span todayCounter = new Span(String.format("Hoy: %s", FormatUtils.formatPrice(todayIncome)));
        todayCounter.getStyle()
            .set("color", "var(--lumo-success-color)")
            .set("font-weight", "bold")
            .set("padding", "0.5rem 1rem")
            .set("background", "var(--lumo-success-color-10pct)")
            .set("border-radius", "var(--lumo-border-radius-m)");
        
        Span monthCounter = new Span(String.format("Este mes: %s", FormatUtils.formatPrice(monthIncome)));
        monthCounter.getStyle()
            .set("color", "var(--lumo-primary-color)")
            .set("font-weight", "bold")
            .set("padding", "0.5rem 1rem")
            .set("background", "var(--lumo-primary-color-10pct)")
            .set("border-radius", "var(--lumo-border-radius-m)");
        
        HorizontalLayout counters = new HorizontalLayout(todayCounter, monthCounter);
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
        searchField = new TextField();
        searchField.setPlaceholder("Buscar por número de pago o contrato...");
        searchField.setPrefixComponent(VaadinIcon.SEARCH.create());
        searchField.setValueChangeMode(ValueChangeMode.LAZY);
        searchField.addValueChangeListener(e -> updateGrid());
        searchField.setWidth("350px");

        methodFilter = new ComboBox<>("Método");
        methodFilter.setItems(PaymentMethod.values());
        methodFilter.setItemLabelGenerator(PaymentMethod::getLabel);
        methodFilter.setPlaceholder("Todos");
        methodFilter.setClearButtonVisible(true);
        methodFilter.addValueChangeListener(e -> updateGrid());
        methodFilter.setWidth("150px");

        statusFilter = new ComboBox<>("Estado");
        statusFilter.setItems(PaymentStatus.values());
        statusFilter.setItemLabelGenerator(PaymentStatus::getLabel);
        statusFilter.setPlaceholder("Todos");
        statusFilter.setClearButtonVisible(true);
        statusFilter.addValueChangeListener(e -> updateGrid());
        statusFilter.setWidth("150px");

        HorizontalLayout toolbar = new HorizontalLayout(searchField, methodFilter, statusFilter);
        toolbar.setWidthFull();
        toolbar.setJustifyContentMode(FlexComponent.JustifyContentMode.START);
        toolbar.setAlignItems(FlexComponent.Alignment.END);
        toolbar.getStyle().set("padding", "1rem 0");

        return toolbar;
    }

    private Component createGrid() {
        grid = new Grid<>(PaymentDTO.class, false);
        grid.setSizeFull();
        
        grid.addColumn(PaymentDTO::getPaymentNumber)
            .setHeader("Número")
            .setWidth("150px")
            .setFlexGrow(0);
        
        grid.addColumn(PaymentDTO::getRentalContractNumber)
            .setHeader("Contrato")
            .setWidth("150px")
            .setFlexGrow(0);
        
        grid.addColumn(payment -> FormatUtils.formatDateTime(payment.getPaymentDate()))
            .setHeader("Fecha")
            .setWidth("160px")
            .setFlexGrow(0);
        
        grid.addColumn(payment -> FormatUtils.formatPrice(payment.getAmount()))
            .setHeader("Monto")
            .setWidth("120px")
            .setFlexGrow(0);
        
        grid.addComponentColumn(this::createMethodBadge)
            .setHeader("Método")
            .setWidth("130px")
            .setFlexGrow(0);
        
        grid.addComponentColumn(this::createStatusBadge)
            .setHeader("Estado")
            .setWidth("130px")
            .setFlexGrow(0);
        
        grid.addColumn(PaymentDTO::getCreatedBy)
            .setHeader("Usuario")
            .setWidth("120px")
            .setFlexGrow(0);
        
        grid.addComponentColumn(this::createActionButtons)
            .setHeader("Acciones")
            .setWidth("100px")
            .setFlexGrow(0);
        
        grid.addThemeVariants();
        grid.getStyle()
            .set("border", "1px solid var(--lumo-contrast-10pct)")
            .set("border-radius", "var(--lumo-border-radius-m)");

        return grid;
    }

    private Component createMethodBadge(PaymentDTO payment) {
        Span badge = new Span(payment.getPaymentMethodLabel());
        badge.getElement().getThemeList().add("badge");
        
        String color = switch (payment.getPaymentMethod()) {
            case "CASH" -> "success";
            case "CARD" -> "primary";
            case "BANK_TRANSFER" -> "contrast";
            case "CHECK" -> "warning";
            default -> "contrast";
        };
        
        badge.getElement().getThemeList().add(color);
        return badge;
    }

    private Component createStatusBadge(PaymentDTO payment) {
        Span badge = new Span(payment.getStatusLabel());
        badge.getElement().getThemeList().add("badge");
        
        String color = switch (payment.getStatus()) {
            case "COMPLETED" -> "success";
            case "PENDING" -> "contrast";
            case "REJECTED" -> "error";
            case "REFUNDED" -> "warning";
            default -> "contrast";
        };
        
        badge.getElement().getThemeList().add(color);
        return badge;
    }

    private Component createActionButtons(PaymentDTO payment) {
        HorizontalLayout actions = new HorizontalLayout();
        actions.setSpacing(false);

        if (payment.getCanBeRefunded()) {
            Button refundButton = new Button(VaadinIcon.ROTATE_LEFT.create());
            refundButton.addThemeVariants(
                ButtonVariant.LUMO_SMALL, 
                ButtonVariant.LUMO_TERTIARY,
                ButtonVariant.LUMO_ERROR
            );
            refundButton.getElement().setAttribute("title", "Reembolsar");
            refundButton.addClickListener(e -> refundPayment(payment));
            actions.add(refundButton);
        }

        return actions;
    }

    private void refundPayment(PaymentDTO payment) {
        try {
            paymentService.refundPayment(payment.getId());
            updateGrid();
            showSuccessNotification("Pago reembolsado exitosamente");
        } catch (Exception e) {
            showErrorNotification("Error al reembolsar: " + e.getMessage());
        }
    }

    private void updateGrid() {
        List<PaymentDTO> payments;
        
        PaymentMethod method = methodFilter.getValue();
        PaymentStatus status = statusFilter.getValue();
        String searchTerm = searchField.getValue();
        
        if (method != null) {
            payments = paymentService.findByPaymentMethod(method);
        } else if (status != null) {
            payments = paymentService.findByStatus(status);
        } else {
            payments = paymentService.findAll();
        }
        
        if (searchTerm != null && !searchTerm.isBlank()) {
            String lowerSearch = searchTerm.toLowerCase();
            payments = payments.stream()
                .filter(p -> 
                    p.getPaymentNumber().toLowerCase().contains(lowerSearch) ||
                    p.getRentalContractNumber().toLowerCase().contains(lowerSearch)
                )
                .toList();
        }
        
        grid.setItems(payments);
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
