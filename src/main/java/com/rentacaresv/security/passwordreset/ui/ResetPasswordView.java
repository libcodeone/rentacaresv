package com.rentacaresv.security.passwordreset.ui;

import com.rentacaresv.security.passwordreset.application.PasswordResetService;
import com.rentacaresv.security.passwordreset.domain.PasswordResetToken;
import com.rentacaresv.settings.application.SettingsService;
import com.rentacaresv.user.domain.User;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Vista para restablecer la contraseña con un token válido
 */
@Route("reset-password")
@PageTitle("Nueva Contraseña | RentaCarESV")
@AnonymousAllowed
@Slf4j
public class ResetPasswordView extends VerticalLayout implements BeforeEnterObserver {

    private final PasswordResetService passwordResetService;
    private final SettingsService settingsService;

    private String token;
    private User user;
    private Div contentContainer;

    public ResetPasswordView(PasswordResetService passwordResetService, SettingsService settingsService) {
        this.passwordResetService = passwordResetService;
        this.settingsService = settingsService;

        addClassName("reset-password-view");
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);

        contentContainer = new Div();
        add(contentContainer);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        // Obtener token del query parameter
        Map<String, List<String>> params = event.getLocation().getQueryParameters().getParameters();
        List<String> tokenParams = params.get("token");

        if (tokenParams == null || tokenParams.isEmpty()) {
            showInvalidTokenView();
            return;
        }

        token = tokenParams.get(0);

        // Validar token
        Optional<PasswordResetToken> tokenOpt = passwordResetService.validateToken(token);
        if (tokenOpt.isEmpty()) {
            showInvalidTokenView();
            return;
        }

        user = tokenOpt.get().getUser();
        showResetForm(tokenOpt.get());
    }

    private void showResetForm(PasswordResetToken resetToken) {
        contentContainer.removeAll();

        Div container = createContainer();

        // Logo
        Image logo = createLogo();

        // Título
        H2 title = new H2("Nueva Contraseña");
        title.getStyle()
                .set("text-align", "center")
                .set("margin", "0 0 var(--lumo-space-s) 0");

        // Info del usuario
        Paragraph userInfo = new Paragraph("Estableciendo contraseña para: " + user.getEmail());
        userInfo.getStyle()
                .set("text-align", "center")
                .set("color", "var(--lumo-secondary-text-color)")
                .set("margin", "0 0 var(--lumo-space-m) 0");

        // Tiempo restante
        long minutesLeft = resetToken.getMinutesUntilExpiration();
        Span timeInfo = new Span("⏱️ Este enlace expira en " + formatTime(minutesLeft));
        timeInfo.getStyle()
                .set("display", "block")
                .set("text-align", "center")
                .set("font-size", "var(--lumo-font-size-s)")
                .set("color", minutesLeft < 60 ? "var(--lumo-error-text-color)" : "var(--lumo-secondary-text-color)")
                .set("margin-bottom", "var(--lumo-space-l)");

        // Campos de contraseña
        PasswordField passwordField = new PasswordField("Nueva Contraseña");
        passwordField.setWidthFull();
        passwordField.setRequiredIndicatorVisible(true);
        passwordField.setHelperText("Mínimo 6 caracteres");

        PasswordField confirmField = new PasswordField("Confirmar Contraseña");
        confirmField.setWidthFull();
        confirmField.setRequiredIndicatorVisible(true);

        // Contenedor de mensajes
        Div messageContainer = new Div();
        messageContainer.setWidthFull();

        // Botón de guardar
        Button submitButton = new Button("Guardar Nueva Contraseña", VaadinIcon.CHECK.create());
        submitButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        submitButton.setWidthFull();

        submitButton.addClickListener(e -> {
            messageContainer.removeAll();

            String password = passwordField.getValue();
            String confirm = confirmField.getValue();

            // Validaciones
            if (password == null || password.isEmpty()) {
                showFieldError(messageContainer, "La contraseña es requerida");
                return;
            }

            if (password.length() < 6) {
                showFieldError(messageContainer, "La contraseña debe tener al menos 6 caracteres");
                return;
            }

            if (!password.equals(confirm)) {
                showFieldError(messageContainer, "Las contraseñas no coinciden");
                return;
            }

            // Intentar restablecer
            submitButton.setEnabled(false);
            submitButton.setText("Guardando...");

            try {
                boolean success = passwordResetService.resetPassword(token, password);
                
                if (success) {
                    showSuccessView();
                } else {
                    showFieldError(messageContainer, "El enlace ha expirado o ya fue utilizado. Solicita uno nuevo.");
                    submitButton.setEnabled(true);
                    submitButton.setText("Guardar Nueva Contraseña");
                }
            } catch (Exception ex) {
                log.error("Error al restablecer contraseña: {}", ex.getMessage());
                showFieldError(messageContainer, "Ocurrió un error. Intenta nuevamente.");
                submitButton.setEnabled(true);
                submitButton.setText("Guardar Nueva Contraseña");
            }
        });

        // Enter para enviar
        confirmField.addKeyPressListener(com.vaadin.flow.component.Key.ENTER, e -> submitButton.click());

        // Layout
        VerticalLayout formLayout = new VerticalLayout(
                logo, title, userInfo, timeInfo, passwordField, confirmField, messageContainer, submitButton
        );
        formLayout.setPadding(false);
        formLayout.setSpacing(true);
        formLayout.setAlignItems(Alignment.STRETCH);

        container.add(formLayout);
        contentContainer.add(container);
    }

    private void showInvalidTokenView() {
        contentContainer.removeAll();

        Div container = createContainer();

        // Logo
        Image logo = createLogo();

        // Icono de error
        Div iconContainer = new Div();
        iconContainer.getStyle()
                .set("text-align", "center")
                .set("font-size", "48px")
                .set("margin", "var(--lumo-space-m) 0");
        iconContainer.setText("❌");

        // Título
        H2 title = new H2("Enlace Inválido");
        title.getStyle()
                .set("text-align", "center")
                .set("margin", "0 0 var(--lumo-space-m) 0")
                .set("color", "var(--lumo-error-text-color)");

        // Mensaje
        Paragraph message = new Paragraph(
                "El enlace de recuperación es inválido, ha expirado o ya fue utilizado."
        );
        message.getStyle()
                .set("text-align", "center")
                .set("color", "var(--lumo-secondary-text-color)");

        // Botones
        Button requestNewButton = new Button("Solicitar Nuevo Enlace", VaadinIcon.ENVELOPE.create());
        requestNewButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        requestNewButton.setWidthFull();
        requestNewButton.addClickListener(e -> 
                requestNewButton.getUI().ifPresent(ui -> ui.navigate("forgot-password")));

        Anchor backToLogin = new Anchor("login", "← Volver al inicio de sesión");
        backToLogin.getStyle()
                .set("display", "block")
                .set("text-align", "center")
                .set("margin-top", "var(--lumo-space-m)")
                .set("color", "var(--lumo-primary-color)");

        // Layout
        VerticalLayout layout = new VerticalLayout(
                logo, iconContainer, title, message, requestNewButton, backToLogin
        );
        layout.setPadding(false);
        layout.setSpacing(true);
        layout.setAlignItems(Alignment.STRETCH);

        container.add(layout);
        contentContainer.add(container);
    }

    private void showSuccessView() {
        contentContainer.removeAll();

        Div container = createContainer();

        // Logo
        Image logo = createLogo();

        // Icono de éxito
        Div iconContainer = new Div();
        iconContainer.getStyle()
                .set("text-align", "center")
                .set("font-size", "48px")
                .set("margin", "var(--lumo-space-m) 0");
        iconContainer.setText("✅");

        // Título
        H2 title = new H2("¡Contraseña Actualizada!");
        title.getStyle()
                .set("text-align", "center")
                .set("margin", "0 0 var(--lumo-space-m) 0")
                .set("color", "var(--lumo-success-text-color)");

        // Mensaje
        Paragraph message = new Paragraph(
                "Tu contraseña ha sido restablecida exitosamente. Ya puedes iniciar sesión con tu nueva contraseña."
        );
        message.getStyle()
                .set("text-align", "center")
                .set("color", "var(--lumo-secondary-text-color)");

        // Botón para ir al login
        Button loginButton = new Button("Iniciar Sesión", VaadinIcon.SIGN_IN.create());
        loginButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        loginButton.setWidthFull();
        loginButton.addClickListener(e -> 
                loginButton.getUI().ifPresent(ui -> ui.navigate("login")));

        // Layout
        VerticalLayout layout = new VerticalLayout(
                logo, iconContainer, title, message, loginButton
        );
        layout.setPadding(false);
        layout.setSpacing(true);
        layout.setAlignItems(Alignment.STRETCH);

        container.add(layout);
        contentContainer.add(container);
    }

    private void showFieldError(Div container, String message) {
        container.removeAll();
        
        Div errorDiv = new Div();
        errorDiv.getStyle()
                .set("background", "var(--lumo-error-color-10pct)")
                .set("color", "var(--lumo-error-text-color)")
                .set("padding", "var(--lumo-space-s)")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("text-align", "center")
                .set("margin-bottom", "var(--lumo-space-s)");
        errorDiv.setText(message);
        
        container.add(errorDiv);
    }

    private Div createContainer() {
        Div container = new Div();
        container.getStyle()
                .set("background", "var(--lumo-base-color)")
                .set("border-radius", "var(--lumo-border-radius-l)")
                .set("box-shadow", "var(--lumo-box-shadow-m)")
                .set("padding", "var(--lumo-space-xl)")
                .set("max-width", "400px")
                .set("width", "100%");
        return container;
    }

    private Image createLogo() {
        String logoUrl = getLogoUrl();
        Image logo = new Image(logoUrl, "Logo");
        logo.setWidth("150px");
        logo.getStyle()
                .set("display", "block")
                .set("margin", "0 auto var(--lumo-space-l) auto")
                .set("object-fit", "contain");
        return logo;
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

    private String formatTime(long minutes) {
        if (minutes < 60) {
            return minutes + " minutos";
        } else {
            long hours = minutes / 60;
            long mins = minutes % 60;
            if (mins == 0) {
                return hours + " hora" + (hours > 1 ? "s" : "");
            }
            return hours + " hora" + (hours > 1 ? "s" : "") + " y " + mins + " minutos";
        }
    }
}
