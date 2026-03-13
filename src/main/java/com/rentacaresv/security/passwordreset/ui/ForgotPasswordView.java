package com.rentacaresv.security.passwordreset.ui;

import com.rentacaresv.security.passwordreset.application.PasswordResetService;
import com.rentacaresv.settings.application.SettingsService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import lombok.extern.slf4j.Slf4j;

/**
 * Vista para solicitar recuperación de contraseña
 */
@Route("forgot-password")
@PageTitle("Recuperar Contraseña | RentaCarESV")
@AnonymousAllowed
@Slf4j
public class ForgotPasswordView extends VerticalLayout {

    private final PasswordResetService passwordResetService;
    private final SettingsService settingsService;

    private EmailField emailField;
    private Button submitButton;
    private Div messageContainer;

    public ForgotPasswordView(PasswordResetService passwordResetService, SettingsService settingsService) {
        this.passwordResetService = passwordResetService;
        this.settingsService = settingsService;

        addClassName("forgot-password-view");
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);

        createLayout();
    }

    private void createLayout() {
        // Contenedor principal
        Div container = new Div();
        container.getStyle()
                .set("background", "var(--lumo-base-color)")
                .set("border-radius", "var(--lumo-border-radius-l)")
                .set("box-shadow", "var(--lumo-box-shadow-m)")
                .set("padding", "var(--lumo-space-xl)")
                .set("max-width", "400px")
                .set("width", "100%");

        // Logo
        String logoUrl = getLogoUrl();
        Image logo = new Image(logoUrl, "Logo");
        logo.setWidth("150px");
        logo.getStyle()
                .set("display", "block")
                .set("margin", "0 auto var(--lumo-space-l) auto")
                .set("object-fit", "contain");

        // Título
        H2 title = new H2("Recuperar Contraseña");
        title.getStyle()
                .set("text-align", "center")
                .set("margin", "0 0 var(--lumo-space-s) 0");

        // Descripción
        Paragraph description = new Paragraph(
                "Ingresa tu correo electrónico y te enviaremos un enlace para restablecer tu contraseña."
        );
        description.getStyle()
                .set("text-align", "center")
                .set("color", "var(--lumo-secondary-text-color)")
                .set("margin", "0 0 var(--lumo-space-l) 0");

        // Campo de email
        emailField = new EmailField("Correo Electrónico");
        emailField.setWidthFull();
        emailField.setRequiredIndicatorVisible(true);
        emailField.setPrefixComponent(VaadinIcon.ENVELOPE.create());
        emailField.setPlaceholder("tu@email.com");
        emailField.setClearButtonVisible(true);

        // Contenedor de mensajes
        messageContainer = new Div();
        messageContainer.setWidthFull();

        // Botón de enviar
        submitButton = new Button("Enviar Enlace de Recuperación", VaadinIcon.PAPERPLANE.create());
        submitButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        submitButton.setWidthFull();
        submitButton.addClickListener(e -> handleSubmit());

        // Enter para enviar
        emailField.addKeyPressListener(com.vaadin.flow.component.Key.ENTER, e -> handleSubmit());

        // Enlace para volver al login
        Anchor backToLogin = new Anchor("login", "← Volver al inicio de sesión");
        backToLogin.getStyle()
                .set("display", "block")
                .set("text-align", "center")
                .set("margin-top", "var(--lumo-space-l)")
                .set("color", "var(--lumo-primary-color)");

        // Verificar si el email está configurado
        if (!passwordResetService.isEmailConfigured()) {
            showWarning("⚠️ El envío de correos no está configurado. Contacta al administrador del sistema.");
            submitButton.setEnabled(false);
        }

        // Agregar al contenedor
        VerticalLayout formLayout = new VerticalLayout(
                logo, title, description, emailField, messageContainer, submitButton, backToLogin
        );
        formLayout.setPadding(false);
        formLayout.setSpacing(true);
        formLayout.setAlignItems(Alignment.STRETCH);

        container.add(formLayout);
        add(container);
    }

    private void handleSubmit() {
        String email = emailField.getValue();

        // Validar email
        if (email == null || email.trim().isEmpty()) {
            emailField.setInvalid(true);
            emailField.setErrorMessage("El correo electrónico es requerido");
            return;
        }

        if (!email.contains("@") || !email.contains(".")) {
            emailField.setInvalid(true);
            emailField.setErrorMessage("Ingresa un correo electrónico válido");
            return;
        }

        emailField.setInvalid(false);

        // Deshabilitar botón mientras procesa
        submitButton.setEnabled(false);
        submitButton.setText("Enviando...");

        try {
            boolean sent = passwordResetService.requestPasswordReset(email.trim());
            
            // Siempre mostrar mensaje de éxito (por seguridad, no revelar si el email existe)
            showSuccess();
            emailField.clear();
            
        } catch (Exception e) {
            log.error("Error al solicitar recuperación de contraseña: {}", e.getMessage());
            showError("Ocurrió un error al procesar tu solicitud. Intenta nuevamente más tarde.");
        } finally {
            submitButton.setEnabled(true);
            submitButton.setText("Enviar Enlace de Recuperación");
        }
    }

    private void showSuccess() {
        messageContainer.removeAll();
        
        Div successMessage = new Div();
        successMessage.getStyle()
                .set("background", "var(--lumo-success-color-10pct)")
                .set("color", "var(--lumo-success-text-color)")
                .set("padding", "var(--lumo-space-m)")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("text-align", "center")
                .set("margin-bottom", "var(--lumo-space-m)");
        
        successMessage.add(new Paragraph("✅ Si el correo está registrado, recibirás un enlace de recuperación en los próximos minutos."));
        successMessage.add(new Paragraph("Revisa también tu carpeta de spam."));
        
        messageContainer.add(successMessage);
    }

    private void showError(String message) {
        messageContainer.removeAll();
        
        Div errorMessage = new Div();
        errorMessage.getStyle()
                .set("background", "var(--lumo-error-color-10pct)")
                .set("color", "var(--lumo-error-text-color)")
                .set("padding", "var(--lumo-space-m)")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("text-align", "center")
                .set("margin-bottom", "var(--lumo-space-m)");
        
        errorMessage.setText(message);
        messageContainer.add(errorMessage);
    }

    private void showWarning(String message) {
        messageContainer.removeAll();
        
        Div warningMessage = new Div();
        warningMessage.getStyle()
                .set("background", "var(--lumo-warning-color-10pct)")
                .set("color", "var(--lumo-warning-text-color)")
                .set("padding", "var(--lumo-space-m)")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("text-align", "center")
                .set("margin-bottom", "var(--lumo-space-m)");
        
        warningMessage.setText(message);
        messageContainer.add(warningMessage);
    }

    private String getLogoUrl() {
        try {
            String customLogoUrl = settingsService.getLogoUrl();
            if (customLogoUrl != null && !customLogoUrl.isEmpty()) {
                return customLogoUrl;
            }
        } catch (Exception e) {
            log.warn("Error obteniendo logo: {}", e.getMessage());
        }
        return "images/logo.png";
    }
}
