package com.rentacaresv.contract.ui;

import com.rentacaresv.contract.application.ContractService;
import com.rentacaresv.contract.domain.Contract;
import com.rentacaresv.contract.domain.ContractStatus;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import lombok.extern.slf4j.Slf4j;

import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * Diálogo para gestionar el contrato digital de una renta.
 * 
 * Flujo simplificado:
 * 1. Crear contrato (si no existe)
 * 2. Ver/Editar contrato (abre la vista en nueva pestaña)
 * 3. Compartir link por WhatsApp
 * 4. Una vez firmado, descargar PDF
 */
@Slf4j
public class ContractDialog extends Dialog {

    private final ContractService contractService;
    private final Long rentalId;
    private Contract contract;
    private Runnable onContractCreated;

    public ContractDialog(ContractService contractService, Long rentalId) {
        this.contractService = contractService;
        this.rentalId = rentalId;

        setHeaderTitle("Contrato Digital");
        setWidth("500px");

        loadContract();
        buildContent();
    }

    private void loadContract() {
        Optional<Contract> existing = contractService.findByRentalId(rentalId);
        contract = existing.orElse(null);
    }

    private void buildContent() {
        removeAll();

        VerticalLayout content = new VerticalLayout();
        content.setPadding(false);
        content.setSpacing(true);

        if (contract == null) {
            // No existe contrato - mostrar opción de crear
            content.add(createNoContractView());
        } else {
            // Existe contrato - mostrar estado y acciones
            content.add(createContractStatusView());
        }

        add(content);

        // Botón cerrar
        Button closeBtn = new Button("Cerrar", e -> close());
        getFooter().add(closeBtn);
    }

    private VerticalLayout createNoContractView() {
        VerticalLayout layout = new VerticalLayout();
        layout.setAlignItems(FlexComponent.Alignment.CENTER);
        layout.setPadding(true);

        Div icon = new Div();
        icon.add(VaadinIcon.FILE_TEXT_O.create());
        icon.getStyle()
                .set("font-size", "64px")
                .set("color", "var(--lumo-contrast-40pct)");

        H3 title = new H3("Sin contrato digital");
        title.getStyle().set("margin", "var(--lumo-space-m) 0");
        
        Paragraph description = new Paragraph(
                "Esta renta aún no tiene contrato digital. " +
                "Cree uno para poder completar los datos y obtener la firma del cliente."
        );
        description.getStyle()
                .set("text-align", "center")
                .set("color", "var(--lumo-secondary-text-color)");

        Button createBtn = new Button("Crear Contrato", VaadinIcon.PLUS.create());
        createBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_LARGE);
        createBtn.addClickListener(e -> createContract());

        layout.add(icon, title, description, createBtn);
        return layout;
    }

    private VerticalLayout createContractStatusView() {
        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(true);
        layout.setSpacing(true);

        // Estado del contrato
        HorizontalLayout statusRow = new HorizontalLayout();
        statusRow.setAlignItems(FlexComponent.Alignment.CENTER);
        statusRow.setWidthFull();

        Span statusLabel = new Span("Estado: ");
        Span statusBadge = createStatusBadge(contract.getStatus());

        statusRow.add(statusLabel, statusBadge);

        // Información del contrato
        Div infoDiv = new Div();
        infoDiv.getStyle()
                .set("background", "var(--lumo-contrast-5pct)")
                .set("padding", "var(--lumo-space-m)")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("width", "100%");

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        infoDiv.add(
                new Paragraph("Creado: " + contract.getCreatedAt().format(fmt)),
                new Paragraph("Expira: " + (contract.getExpiresAt() != null ? 
                        contract.getExpiresAt().format(fmt) : "N/A"))
        );

        if (contract.getStatus() == ContractStatus.SIGNED) {
            infoDiv.add(
                    new Paragraph("Firmado: " + contract.getSignedAt().format(fmt)),
                    new Paragraph("IP: " + contract.getSignedFromIp())
            );
        }

        layout.add(statusRow, infoDiv);

        // Acciones según estado
        if (contract.getStatus() == ContractStatus.PENDING) {
            layout.add(createPendingActions());
        } else if (contract.getStatus() == ContractStatus.SIGNED) {
            layout.add(createSignedActions());
        }

        return layout;
    }

    private Span createStatusBadge(ContractStatus status) {
        Span badge = new Span(status.getLabel());
        badge.getStyle()
                .set("padding", "var(--lumo-space-xs) var(--lumo-space-s)")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("font-size", "var(--lumo-font-size-s)")
                .set("font-weight", "500");

        switch (status) {
            case PENDING -> {
                badge.getStyle()
                        .set("background", "var(--lumo-warning-color-10pct)")
                        .set("color", "var(--lumo-warning-text-color)");
            }
            case SIGNED -> {
                badge.getStyle()
                        .set("background", "var(--lumo-success-color-10pct)")
                        .set("color", "var(--lumo-success-text-color)");
            }
            case EXPIRED -> {
                badge.getStyle()
                        .set("background", "var(--lumo-error-color-10pct)")
                        .set("color", "var(--lumo-error-text-color)");
            }
            case CANCELLED -> {
                badge.getStyle()
                        .set("background", "var(--lumo-contrast-10pct)")
                        .set("color", "var(--lumo-contrast-70pct)");
            }
        }

        return badge;
    }

    private VerticalLayout createPendingActions() {
        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(false);
        layout.setSpacing(true);

        // Botón principal: Ver/Editar Contrato
        Button editBtn = new Button("Abrir Contrato", VaadinIcon.EDIT.create());
        editBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_LARGE);
        editBtn.setWidthFull();
        editBtn.addClickListener(e -> {
            String contractUrl = getBaseUrl() + "/public/contract/" + contract.getToken();
            // Usar executeJs para mejor compatibilidad con Safari
            UI.getCurrent().getPage().executeJs(
                "const url = $0;" +
                "const newWindow = window.open(url, '_blank');" +
                "if (!newWindow || newWindow.closed || typeof newWindow.closed === 'undefined') {" +
                "  window.location.href = url;" + // Fallback si se bloquea el popup
                "}",
                contractUrl
            );
        });

        // URL del contrato (para copiar/compartir)
        String baseUrl = getBaseUrl();
        String contractUrl = baseUrl + "/public/contract/" + contract.getToken();

        TextField urlField = new TextField("Link del contrato");
        urlField.setValue(contractUrl);
        urlField.setReadOnly(true);
        urlField.setWidthFull();

        // Botones secundarios
        HorizontalLayout actions = new HorizontalLayout();
        actions.setWidthFull();
        actions.setJustifyContentMode(FlexComponent.JustifyContentMode.START);

        Button copyBtn = new Button("Copiar", VaadinIcon.COPY.create());
        copyBtn.addThemeVariants(ButtonVariant.LUMO_SMALL);
        copyBtn.addClickListener(e -> {
            UI.getCurrent().getPage().executeJs(
                    "navigator.clipboard.writeText($0).then(() => { " +
                            "  $1.$server.showCopiedNotification(); " +
                            "});",
                    contractUrl, getElement()
            );
        });

        Button whatsappBtn = new Button("WhatsApp", VaadinIcon.COMMENTS.create());
        whatsappBtn.addThemeVariants(ButtonVariant.LUMO_SUCCESS, ButtonVariant.LUMO_SMALL);
        whatsappBtn.addClickListener(e -> {
            String message = "¡Hola! Aquí está su contrato de alquiler de vehículo: " + contractUrl;
            String whatsappUrl = "https://wa.me/?text=" + java.net.URLEncoder.encode(message, java.nio.charset.StandardCharsets.UTF_8);
            UI.getCurrent().getPage().open(whatsappUrl, "_blank");
        });

        actions.add(copyBtn, whatsappBtn);

        // Botón cancelar (menos prominente)
        Button cancelBtn = new Button("Cancelar Contrato", VaadinIcon.TRASH.create());
        cancelBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        cancelBtn.addClickListener(e -> cancelContract());

        layout.add(editBtn, urlField, actions, cancelBtn);
        return layout;
    }

    private VerticalLayout createSignedActions() {
        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(false);
        layout.setSpacing(true);

        // Info de firma
        Div signedInfo = new Div();
        signedInfo.getStyle()
                .set("background", "var(--lumo-success-color-10pct)")
                .set("padding", "var(--lumo-space-m)")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("border-left", "4px solid var(--lumo-success-color)");

        signedInfo.add(
                new Paragraph("✅ Contrato firmado digitalmente")
        );
        
        if (contract.getDocumentType() != null && contract.getDocumentNumber() != null) {
            signedInfo.add(new Paragraph("Documento: " + contract.getDocumentType().getLabel() + 
                    " - " + contract.getDocumentNumber()));
        }

        // Botón principal: Ver contrato firmado
        Button viewContractBtn = new Button("Ver Contrato Firmado", VaadinIcon.FILE_TEXT_O.create());
        viewContractBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        viewContractBtn.setWidthFull();
        viewContractBtn.addClickListener(e -> {
            String contractUrl = getBaseUrl() + "/public/contract/" + contract.getToken();
            UI.getCurrent().getPage().open(contractUrl, "_blank");
        });

        // Botones secundarios
        HorizontalLayout actions = new HorizontalLayout();
        actions.setWidthFull();

        Button viewSignatureBtn = new Button("Ver Firma", VaadinIcon.PENCIL.create());
        viewSignatureBtn.addThemeVariants(ButtonVariant.LUMO_SMALL);
        viewSignatureBtn.addClickListener(e -> {
            if (contract.getSignatureUrl() != null) {
                UI.getCurrent().getPage().open(contract.getSignatureUrl(), "_blank");
            } else {
                Notification.show("Firma no disponible", 2000, Notification.Position.MIDDLE);
            }
        });

        Button downloadPdfBtn = new Button("Descargar PDF", VaadinIcon.DOWNLOAD.create());
        downloadPdfBtn.addThemeVariants(ButtonVariant.LUMO_SMALL);
        downloadPdfBtn.addClickListener(e -> {
            if (contract.getPdfUrl() != null) {
                UI.getCurrent().getPage().open(contract.getPdfUrl(), "_blank");
            } else {
                Notification.show("PDF aún no generado", 3000, Notification.Position.MIDDLE);
            }
        });
        
        Button whatsappBtn = new Button("Enviar por WhatsApp", VaadinIcon.COMMENTS.create());
        whatsappBtn.addThemeVariants(ButtonVariant.LUMO_SUCCESS, ButtonVariant.LUMO_SMALL);
        whatsappBtn.addClickListener(e -> {
            String contractUrl = getBaseUrl() + "/public/contract/" + contract.getToken();
            String message = "¡Hola! Aquí está su contrato de alquiler firmado: " + contractUrl;
            String whatsappUrl = "https://wa.me/?text=" + java.net.URLEncoder.encode(message, java.nio.charset.StandardCharsets.UTF_8);
            UI.getCurrent().getPage().open(whatsappUrl, "_blank");
        });

        actions.add(viewSignatureBtn, downloadPdfBtn, whatsappBtn);

        layout.add(signedInfo, viewContractBtn, actions);
        return layout;
    }

    private void createContract() {
        try {
            contract = contractService.createContract(rentalId);
            
            Notification.show("Contrato creado exitosamente", 3000, Notification.Position.BOTTOM_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);

            if (onContractCreated != null) {
                onContractCreated.run();
            }

            buildContent();

        } catch (Exception e) {
            log.error("Error creando contrato: {}", e.getMessage(), e);
            Notification.show("Error: " + e.getMessage(), 5000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void cancelContract() {
        Dialog confirmDialog = new Dialog();
        confirmDialog.setHeaderTitle("Confirmar cancelación");
        confirmDialog.add(new Paragraph("¿Está seguro de cancelar este contrato? Esta acción no se puede deshacer."));

        Button confirmBtn = new Button("Sí, cancelar", e -> {
            try {
                contractService.cancelContract(contract.getId());
                loadContract();
                buildContent();
                confirmDialog.close();
                
                Notification.show("Contrato cancelado", 3000, Notification.Position.BOTTOM_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);

            } catch (Exception ex) {
                Notification.show("Error: " + ex.getMessage(), 5000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        confirmBtn.addThemeVariants(ButtonVariant.LUMO_ERROR);

        Button cancelBtn = new Button("No", e -> confirmDialog.close());

        confirmDialog.getFooter().add(cancelBtn, confirmBtn);
        confirmDialog.open();
    }

    @com.vaadin.flow.component.ClientCallable
    public void showCopiedNotification() {
        Notification.show("Link copiado al portapapeles", 2000, Notification.Position.BOTTOM_CENTER)
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }

    private String getBaseUrl() {
        // Obtener URL base desde VaadinServletRequest
        var vaadinRequest = com.vaadin.flow.server.VaadinRequest.getCurrent();
        if (vaadinRequest instanceof com.vaadin.flow.server.VaadinServletRequest servletRequest) {
            var httpRequest = servletRequest.getHttpServletRequest();
            String scheme = httpRequest.getHeader("X-Forwarded-Proto");
            if (scheme == null) {
                scheme = httpRequest.getScheme();
            }
            String host = httpRequest.getHeader("Host");
            if (host == null) {
                host = httpRequest.getServerName();
                int port = httpRequest.getServerPort();
                if (("http".equals(scheme) && port != 80) || ("https".equals(scheme) && port != 443)) {
                    host += ":" + port;
                }
            }
            return scheme + "://" + host;
        }
        // Fallback
        return "http://localhost:8080";
    }

    public void setOnContractCreated(Runnable callback) {
        this.onContractCreated = callback;
    }
}
