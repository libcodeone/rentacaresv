package com.rentacaresv.settings.ui;

import com.rentacaresv.settings.application.DynamicMailService;
import com.rentacaresv.settings.application.SettingsService;
import com.rentacaresv.settings.domain.Settings;
import com.rentacaresv.views.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import lombok.extern.slf4j.Slf4j;
import org.vaadin.lineawesome.LineAwesomeIconUrl;

/**
 * Vista de configuración del sistema
 * Solo accesible para administradores
 */
@PageTitle("Configuración")
@Route(value = "settings", layout = MainLayout.class)
@Menu(order = 99, icon = LineAwesomeIconUrl.COG_SOLID)
@RolesAllowed("ADMIN")
@Slf4j
public class SettingsView extends VerticalLayout {

    private final SettingsService settingsService;
    private final DynamicMailService mailService;
    
    private Image logoPreview;
    private TextField companyNameField;
    private Span logoStatusSpan;
    
    // Campos de email
    private Checkbox emailEnabledCheckbox;
    private TextField mailHostField;
    private IntegerField mailPortField;
    private TextField mailUsernameField;
    private PasswordField mailPasswordField;
    private EmailField mailFromField;
    private TextField mailFromNameField;
    private Checkbox mailSmtpAuthCheckbox;
    private Checkbox mailStarttlsCheckbox;
    private Checkbox mailSslCheckbox;
    private EmailField testEmailField;
    
    // Campos de Google Calendar
    private Checkbox googleCalendarEnabledCheckbox;
    private TextField googleClientIdField;
    private PasswordField googleClientSecretField;

    // Campos de tarifas especiales
    private NumberField tarifaSacarPaisField;

    public SettingsView(SettingsService settingsService, DynamicMailService mailService) {
        this.settingsService = settingsService;
        this.mailService = mailService;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        H2 title = new H2("Configuración del Sistema");
        title.getStyle().set("margin-top", "0");

        add(title, createLogoSection(), createCompanySection(), createEmailSection(), createGoogleCalendarSection(), createTarifasSection());
    }

    private VerticalLayout createLogoSection() {
        VerticalLayout section = new VerticalLayout();
        section.setPadding(true);
        section.setSpacing(true);
        section.getStyle()
                .set("background", "var(--lumo-base-color)")
                .set("border-radius", "var(--lumo-border-radius-l)")
                .set("box-shadow", "var(--lumo-box-shadow-s)");

        H3 sectionTitle = new H3("Logo de la Aplicación");
        sectionTitle.getStyle().set("margin-top", "0");

        Paragraph description = new Paragraph(
                "El logo se mostrará en el sidebar y en la pantalla de login. " +
                "Formatos recomendados: PNG o JPG. Tamaño recomendado: 200x60 píxeles.");
        description.getStyle()
                .set("color", "var(--lumo-secondary-text-color)")
                .set("font-size", "var(--lumo-font-size-s)");

        // Preview del logo actual
        Div previewContainer = createLogoPreview();

        // Upload de nuevo logo
        MemoryBuffer buffer = new MemoryBuffer();
        Upload upload = new Upload(buffer);
        upload.setAcceptedFileTypes("image/png", "image/jpeg", "image/jpg", "image/gif", "image/webp");
        upload.setMaxFiles(1);
        upload.setMaxFileSize(5 * 1024 * 1024); // 5MB máximo
        upload.setDropAllowed(true);

        Button uploadButton = new Button("Seleccionar imagen", VaadinIcon.UPLOAD.create());
        upload.setUploadButton(uploadButton);

        Span dropLabel = new Span("o arrastra la imagen aquí");
        upload.setDropLabelIcon(VaadinIcon.PICTURE.create());
        upload.setDropLabel(dropLabel);

        upload.addSucceededListener(event -> {
            try {
                String fileName = event.getFileName();
                String mimeType = event.getMIMEType();

                String newLogoUrl = settingsService.uploadLogo(
                        buffer.getInputStream(),
                        fileName,
                        mimeType
                );

                // Actualizar preview
                updateLogoPreview(newLogoUrl);
                
                Notification.show("Logo actualizado correctamente", 3000, Notification.Position.BOTTOM_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);

                // Recargar la página para actualizar el sidebar
                getUI().ifPresent(ui -> ui.getPage().reload());

            } catch (Exception e) {
                log.error("Error al subir logo: {}", e.getMessage(), e);
                Notification.show("Error al subir el logo: " + e.getMessage(), 5000, Notification.Position.BOTTOM_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });

        upload.addFileRejectedListener(event -> {
            Notification.show("Archivo rechazado: " + event.getErrorMessage(), 5000, Notification.Position.BOTTOM_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        });

        // Botón para resetear al logo por defecto
        Button resetButton = new Button("Restaurar logo por defecto", VaadinIcon.REFRESH.create());
        resetButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);
        resetButton.addClickListener(e -> {
            try {
                settingsService.resetLogo();
                updateLogoPreview(null);
                
                Notification.show("Logo restaurado al valor por defecto", 3000, Notification.Position.BOTTOM_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);

                // Recargar la página
                getUI().ifPresent(ui -> ui.getPage().reload());

            } catch (Exception ex) {
                log.error("Error al resetear logo: {}", ex.getMessage(), ex);
                Notification.show("Error: " + ex.getMessage(), 5000, Notification.Position.BOTTOM_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });

        section.add(sectionTitle, description, previewContainer, upload, resetButton);
        return section;
    }

    private Div createLogoPreview() {
        Div container = new Div();
        container.getStyle()
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("align-items", "center")
                .set("gap", "var(--lumo-space-s)")
                .set("padding", "var(--lumo-space-m)")
                .set("background", "var(--lumo-contrast-5pct)")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("margin-bottom", "var(--lumo-space-m)");

        Span label = new Span("Logo actual:");
        label.getStyle()
                .set("font-weight", "500")
                .set("color", "var(--lumo-secondary-text-color)");

        // Obtener logo actual
        String currentLogoUrl = settingsService.getLogoUrl();
        
        logoPreview = new Image();
        logoPreview.setAlt("Logo actual");
        logoPreview.getStyle()
                .set("max-height", "80px")
                .set("max-width", "250px")
                .set("object-fit", "contain")
                .set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("border-radius", "var(--lumo-border-radius-s)")
                .set("padding", "var(--lumo-space-s)")
                .set("background", "white");

        // Inicializar logoStatusSpan ANTES de llamar a updateLogoPreview
        logoStatusSpan = new Span();
        logoStatusSpan.getStyle()
                .set("font-size", "var(--lumo-font-size-xs)")
                .set("color", "var(--lumo-tertiary-text-color)");

        // Ahora sí podemos actualizar
        updateLogoPreview(currentLogoUrl);

        container.add(label, logoPreview, logoStatusSpan);
        return container;
    }

    private void updateLogoPreview(String logoUrl) {
        if (logoUrl != null && !logoUrl.isEmpty()) {
            // Agregar timestamp para evitar cache
            logoPreview.setSrc(logoUrl + "?t=" + System.currentTimeMillis());
        } else {
            logoPreview.setSrc("images/logo.png");
        }
        updateLogoStatus(logoUrl);
    }

    private void updateLogoStatus(String logoUrl) {
        if (logoUrl != null && !logoUrl.isEmpty()) {
            logoStatusSpan.setText("Logo personalizado activo");
            logoStatusSpan.getStyle().set("color", "var(--lumo-success-text-color)");
        } else {
            logoStatusSpan.setText("Usando logo por defecto");
            logoStatusSpan.getStyle().set("color", "var(--lumo-tertiary-text-color)");
        }
    }

    private VerticalLayout createCompanySection() {
        VerticalLayout section = new VerticalLayout();
        section.setPadding(true);
        section.setSpacing(true);
        section.getStyle()
                .set("background", "var(--lumo-base-color)")
                .set("border-radius", "var(--lumo-border-radius-l)")
                .set("box-shadow", "var(--lumo-box-shadow-s)");

        H3 sectionTitle = new H3("Información de la Empresa");
        sectionTitle.getStyle().set("margin-top", "0");

        Settings settings = settingsService.getSettings();

        companyNameField = new TextField("Nombre de la Empresa");
        companyNameField.setValue(settings.getCompanyName() != null ? settings.getCompanyName() : "");
        companyNameField.setWidthFull();
        companyNameField.setMaxWidth("400px");
        companyNameField.setHelperText("Este nombre se mostrará en el título de la aplicación");

        Button saveButton = new Button("Guardar", VaadinIcon.CHECK.create());
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveButton.addClickListener(e -> {
            try {
                settingsService.updateCompanyName(companyNameField.getValue());
                Notification.show("Configuración guardada", 3000, Notification.Position.BOTTOM_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } catch (Exception ex) {
                log.error("Error al guardar configuración: {}", ex.getMessage(), ex);
                Notification.show("Error: " + ex.getMessage(), 5000, Notification.Position.BOTTOM_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });

        HorizontalLayout formLayout = new HorizontalLayout(companyNameField, saveButton);
        formLayout.setAlignItems(FlexComponent.Alignment.END);
        formLayout.setWidthFull();

        section.add(sectionTitle, formLayout);
        return section;
    }

    private VerticalLayout createEmailSection() {
        VerticalLayout section = new VerticalLayout();
        section.setPadding(true);
        section.setSpacing(true);
        section.getStyle()
                .set("background", "var(--lumo-base-color)")
                .set("border-radius", "var(--lumo-border-radius-l)")
                .set("box-shadow", "var(--lumo-box-shadow-s)");

        H3 sectionTitle = new H3("Configuración de Email (SMTP)");
        sectionTitle.getStyle().set("margin-top", "0");

        Paragraph description = new Paragraph(
                "Configure el servidor SMTP para enviar correos automáticos (contratos firmados, notificaciones, etc.)");
        description.getStyle()
                .set("color", "var(--lumo-secondary-text-color)")
                .set("font-size", "var(--lumo-font-size-s)");

        Settings settings = settingsService.getSettings();

        // Habilitar/deshabilitar email
        emailEnabledCheckbox = new Checkbox("Habilitar envío de correos");
        emailEnabledCheckbox.setValue(Boolean.TRUE.equals(settings.getEmailEnabled()));

        // Servidor y puerto
        HorizontalLayout serverRow = new HorizontalLayout();
        serverRow.setWidthFull();
        serverRow.setSpacing(true);

        mailHostField = new TextField("Servidor SMTP");
        mailHostField.setValue(settings.getMailHost() != null ? settings.getMailHost() : "");
        mailHostField.setPlaceholder("smtp.hostinger.com");
        mailHostField.setWidth("300px");
        mailHostField.setHelperText("Ej: smtp.gmail.com, smtp.hostinger.com");

        mailPortField = new IntegerField("Puerto");
        mailPortField.setValue(settings.getMailPort() != null ? settings.getMailPort() : 587);
        mailPortField.setMin(1);
        mailPortField.setMax(65535);
        mailPortField.setWidth("100px");
        mailPortField.setHelperText("587 (STARTTLS) o 465 (SSL)");

        serverRow.add(mailHostField, mailPortField);

        // Usuario y contraseña
        HorizontalLayout authRow = new HorizontalLayout();
        authRow.setWidthFull();
        authRow.setSpacing(true);

        mailUsernameField = new TextField("Usuario SMTP");
        mailUsernameField.setValue(settings.getMailUsername() != null ? settings.getMailUsername() : "");
        mailUsernameField.setPlaceholder("usuario@dominio.com");
        mailUsernameField.setWidth("250px");

        mailPasswordField = new PasswordField("Contraseña SMTP");
        mailPasswordField.setValue(settings.getMailPassword() != null ? settings.getMailPassword() : "");
        mailPasswordField.setWidth("200px");
        mailPasswordField.setHelperText("La contraseña se guarda de forma segura");

        authRow.add(mailUsernameField, mailPasswordField);

        // From (remitente)
        HorizontalLayout fromRow = new HorizontalLayout();
        fromRow.setWidthFull();
        fromRow.setSpacing(true);

        mailFromField = new EmailField("Email Remitente (From)");
        mailFromField.setValue(settings.getMailFrom() != null ? settings.getMailFrom() : "");
        mailFromField.setPlaceholder("correo@empresa.com");
        mailFromField.setWidth("250px");
        mailFromField.setHelperText("Dejar vacío para usar el usuario SMTP");

        mailFromNameField = new TextField("Nombre Remitente");
        mailFromNameField.setValue(settings.getMailFromName() != null ? settings.getMailFromName() : "");
        mailFromNameField.setPlaceholder("Mi Empresa");
        mailFromNameField.setWidth("200px");
        mailFromNameField.setHelperText("Nombre que aparece al recibir el correo");

        fromRow.add(mailFromField, mailFromNameField);

        // Opciones de seguridad
        HorizontalLayout securityRow = new HorizontalLayout();
        securityRow.setSpacing(true);
        securityRow.setAlignItems(FlexComponent.Alignment.CENTER);

        mailSmtpAuthCheckbox = new Checkbox("Autenticación SMTP");
        mailSmtpAuthCheckbox.setValue(Boolean.TRUE.equals(settings.getMailSmtpAuth()));

        mailStarttlsCheckbox = new Checkbox("STARTTLS (puerto 587)");
        mailStarttlsCheckbox.setValue(Boolean.TRUE.equals(settings.getMailStarttlsEnable()));

        mailSslCheckbox = new Checkbox("SSL/TLS (puerto 465)");
        mailSslCheckbox.setValue(Boolean.TRUE.equals(settings.getMailSslEnable()));

        // Cuando se activa SSL, desactivar STARTTLS y viceversa
        mailStarttlsCheckbox.addValueChangeListener(e -> {
            if (e.getValue()) {
                mailSslCheckbox.setValue(false);
                mailPortField.setValue(587);
            }
        });

        mailSslCheckbox.addValueChangeListener(e -> {
            if (e.getValue()) {
                mailStarttlsCheckbox.setValue(false);
                mailPortField.setValue(465);
            }
        });

        securityRow.add(mailSmtpAuthCheckbox, mailStarttlsCheckbox, mailSslCheckbox);

        // Botones de acción
        HorizontalLayout actionsRow = new HorizontalLayout();
        actionsRow.setSpacing(true);
        actionsRow.setAlignItems(FlexComponent.Alignment.END);
        actionsRow.setWidthFull();

        Button saveEmailButton = new Button("Guardar Configuración", VaadinIcon.CHECK.create());
        saveEmailButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveEmailButton.addClickListener(e -> saveEmailSettings());

        Button testConnectionButton = new Button("Probar Conexión", VaadinIcon.CONNECT.create());
        testConnectionButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        testConnectionButton.addClickListener(e -> testConnection());

        // Campo para email de prueba
        testEmailField = new EmailField("Email de prueba");
        testEmailField.setPlaceholder("test@email.com");
        testEmailField.setWidth("200px");
        testEmailField.setClearButtonVisible(true);

        Button sendTestButton = new Button("Enviar Email de Prueba", VaadinIcon.ENVELOPE.create());
        sendTestButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        sendTestButton.addClickListener(e -> sendTestEmail());

        actionsRow.add(saveEmailButton, testConnectionButton, testEmailField, sendTestButton);

        // Presets comunes
        HorizontalLayout presetsRow = new HorizontalLayout();
        presetsRow.setSpacing(true);

        Span presetsLabel = new Span("Configuración rápida:");
        presetsLabel.getStyle().set("color", "var(--lumo-secondary-text-color)");

        Button hostingerPreset = new Button("Hostinger", e -> applyPreset("hostinger"));
        hostingerPreset.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);

        Button gmailPreset = new Button("Gmail", e -> applyPreset("gmail"));
        gmailPreset.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);

        Button outlookPreset = new Button("Outlook/Office365", e -> applyPreset("outlook"));
        outlookPreset.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);

        presetsRow.add(presetsLabel, hostingerPreset, gmailPreset, outlookPreset);

        section.add(sectionTitle, description, emailEnabledCheckbox, serverRow, authRow, fromRow, 
                    securityRow, presetsRow, actionsRow);

        return section;
    }

    private void applyPreset(String preset) {
        switch (preset) {
            case "hostinger" -> {
                mailHostField.setValue("smtp.hostinger.com");
                mailPortField.setValue(587);
                mailSmtpAuthCheckbox.setValue(true);
                mailStarttlsCheckbox.setValue(true);
                mailSslCheckbox.setValue(false);
            }
            case "gmail" -> {
                mailHostField.setValue("smtp.gmail.com");
                mailPortField.setValue(587);
                mailSmtpAuthCheckbox.setValue(true);
                mailStarttlsCheckbox.setValue(true);
                mailSslCheckbox.setValue(false);
                Notification.show("Gmail requiere contraseña de aplicación (2FA)", 5000, 
                        Notification.Position.BOTTOM_CENTER);
            }
            case "outlook" -> {
                mailHostField.setValue("smtp.office365.com");
                mailPortField.setValue(587);
                mailSmtpAuthCheckbox.setValue(true);
                mailStarttlsCheckbox.setValue(true);
                mailSslCheckbox.setValue(false);
            }
        }
        Notification.show("Preset aplicado: " + preset, 2000, Notification.Position.BOTTOM_CENTER);
    }

    private void saveEmailSettings() {
        try {
            Settings settings = settingsService.getSettings();
            
            settings.setEmailEnabled(emailEnabledCheckbox.getValue());
            settings.setMailHost(mailHostField.getValue());
            settings.setMailPort(mailPortField.getValue());
            settings.setMailUsername(mailUsernameField.getValue());
            
            // Solo actualizar contraseña si se ingresó una nueva
            String newPassword = mailPasswordField.getValue();
            if (newPassword != null && !newPassword.isEmpty()) {
                settings.setMailPassword(newPassword);
            }
            
            settings.setMailFrom(mailFromField.getValue());
            settings.setMailFromName(mailFromNameField.getValue());
            settings.setMailSmtpAuth(mailSmtpAuthCheckbox.getValue());
            settings.setMailStarttlsEnable(mailStarttlsCheckbox.getValue());
            settings.setMailSslEnable(mailSslCheckbox.getValue());
            
            settingsService.updateSettings(settings);
            
            Notification.show("Configuración de email guardada correctamente", 3000, 
                    Notification.Position.BOTTOM_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            
        } catch (Exception e) {
            log.error("Error guardando configuración de email: {}", e.getMessage(), e);
            Notification.show("Error: " + e.getMessage(), 5000, Notification.Position.BOTTOM_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void testConnection() {
        // Primero guardar la configuración
        saveEmailSettings();
        
        try {
            boolean success = mailService.testConnection();
            
            if (success) {
                Notification.show("✅ Conexión SMTP exitosa", 3000, Notification.Position.BOTTOM_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } else {
                Notification.show("❌ Error de conexión. Verifique la configuración.", 5000, 
                        Notification.Position.BOTTOM_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
            
        } catch (Exception e) {
            log.error("Error probando conexión SMTP: {}", e.getMessage(), e);
            Notification.show("Error: " + e.getMessage(), 5000, Notification.Position.BOTTOM_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void sendTestEmail() {
        String testEmail = testEmailField.getValue();
        
        if (testEmail == null || testEmail.isEmpty()) {
            Notification.show("Ingrese un email de destino para la prueba", 3000, 
                    Notification.Position.BOTTOM_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_WARNING);
            return;
        }
        
        // Primero guardar la configuración
        saveEmailSettings();
        
        try {
            mailService.sendTestEmail(testEmail);
            Notification.show("✅ Email de prueba enviado a: " + testEmail, 3000, 
                    Notification.Position.BOTTOM_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            
        } catch (Exception e) {
            log.error("Error enviando email de prueba: {}", e.getMessage(), e);
            Notification.show("Error: " + e.getMessage(), 5000, Notification.Position.BOTTOM_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    // ========================================
    // SECCIÓN GOOGLE CALENDAR
    // ========================================

    private VerticalLayout createGoogleCalendarSection() {
        VerticalLayout section = new VerticalLayout();
        section.setPadding(true);
        section.setSpacing(true);
        section.getStyle()
                .set("background", "var(--lumo-base-color)")
                .set("border-radius", "var(--lumo-border-radius-l)")
                .set("box-shadow", "var(--lumo-box-shadow-s)");

        H3 sectionTitle = new H3("Google Calendar");
        sectionTitle.getStyle().set("margin-top", "0");

        Paragraph description = new Paragraph(
                "Permite a los usuarios vincular su cuenta de Google Calendar para sincronizar automáticamente las rentas. " +
                "Necesita credenciales OAuth2 de Google Cloud Console.");
        description.getStyle()
                .set("color", "var(--lumo-secondary-text-color)")
                .set("font-size", "var(--lumo-font-size-s)");

        // Link a Google Cloud Console
        Anchor googleConsoleLink = new Anchor(
                "https://console.cloud.google.com/apis/credentials", 
                "Ir a Google Cloud Console");
        googleConsoleLink.setTarget("_blank");
        googleConsoleLink.getStyle()
                .set("font-size", "var(--lumo-font-size-s)")
                .set("margin-bottom", "var(--lumo-space-m)");

        Settings settings = settingsService.getSettings();

        // Habilitar Google Calendar
        googleCalendarEnabledCheckbox = new Checkbox("Habilitar integración con Google Calendar");
        googleCalendarEnabledCheckbox.setValue(Boolean.TRUE.equals(settings.getGoogleCalendarEnabled()));

        // Client ID
        googleClientIdField = new TextField("Client ID");
        googleClientIdField.setValue(settings.getGoogleClientId() != null ? settings.getGoogleClientId() : "");
        googleClientIdField.setWidthFull();
        googleClientIdField.setMaxWidth("600px");
        googleClientIdField.setPlaceholder("123456789-abc123.apps.googleusercontent.com");
        googleClientIdField.setHelperText("OAuth 2.0 Client ID de Google Cloud Console");

        // Client Secret
        googleClientSecretField = new PasswordField("Client Secret");
        googleClientSecretField.setValue(settings.getGoogleClientSecret() != null ? settings.getGoogleClientSecret() : "");
        googleClientSecretField.setWidthFull();
        googleClientSecretField.setMaxWidth("400px");
        googleClientSecretField.setPlaceholder("GOCSPX-...");
        googleClientSecretField.setHelperText("OAuth 2.0 Client Secret");

        // Instrucciones
        VerticalLayout instructionsLayout = new VerticalLayout();
        instructionsLayout.setPadding(false);
        instructionsLayout.setSpacing(false);
        instructionsLayout.getStyle()
                .set("background", "var(--lumo-contrast-5pct)")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("padding", "var(--lumo-space-m)")
                .set("margin-top", "var(--lumo-space-m)");

        Span instructionsTitle = new Span("Cómo obtener las credenciales:");
        instructionsTitle.getStyle().set("font-weight", "600");

        Paragraph step1 = new Paragraph("1. Ve a Google Cloud Console > APIs & Services > Credentials");
        Paragraph step2 = new Paragraph("2. Crea un OAuth 2.0 Client ID (tipo: Web Application)");
        Paragraph step3 = new Paragraph("3. Agrega URI de redirección: " + getRedirectUri());
        Paragraph step4 = new Paragraph("4. Copia el Client ID y Client Secret aquí");
        Paragraph step5 = new Paragraph("5. Habilita Google Calendar API en tu proyecto de GCP");

        step1.getStyle().set("margin", "var(--lumo-space-xs) 0").set("font-size", "var(--lumo-font-size-s)");
        step2.getStyle().set("margin", "var(--lumo-space-xs) 0").set("font-size", "var(--lumo-font-size-s)");
        step3.getStyle().set("margin", "var(--lumo-space-xs) 0").set("font-size", "var(--lumo-font-size-s)");
        step4.getStyle().set("margin", "var(--lumo-space-xs) 0").set("font-size", "var(--lumo-font-size-s)");
        step5.getStyle().set("margin", "var(--lumo-space-xs) 0").set("font-size", "var(--lumo-font-size-s)");

        instructionsLayout.add(instructionsTitle, step1, step2, step3, step4, step5);

        // Botón guardar
        Button saveButton = new Button("Guardar Configuración", VaadinIcon.CHECK.create());
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveButton.addClickListener(e -> saveGoogleCalendarSettings());

        // Estado actual
        Div statusDiv = createGoogleCalendarStatus(settings);

        section.add(sectionTitle, description, googleConsoleLink, statusDiv, 
                    googleCalendarEnabledCheckbox, googleClientIdField, googleClientSecretField,
                    instructionsLayout, saveButton);

        return section;
    }

    private Div createGoogleCalendarStatus(Settings settings) {
        Div statusDiv = new Div();
        statusDiv.getStyle()
                .set("padding", "var(--lumo-space-s) var(--lumo-space-m)")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("margin-bottom", "var(--lumo-space-m)");

        if (settings.isGoogleCalendarConfigured()) {
            statusDiv.getStyle().set("background", "var(--lumo-success-color-10pct)");
            statusDiv.add(new Span("✅ Google Calendar está configurado y habilitado"));
        } else if (Boolean.TRUE.equals(settings.getGoogleCalendarEnabled())) {
            statusDiv.getStyle().set("background", "var(--lumo-warning-color-10pct)");
            statusDiv.add(new Span("⚠️ Habilitado pero faltan credenciales"));
        } else {
            statusDiv.getStyle().set("background", "var(--lumo-contrast-5pct)");
            statusDiv.add(new Span("❌ Google Calendar no está habilitado"));
        }

        return statusDiv;
    }

    private String getRedirectUri() {
        // Construir la URI de redirección para mostrar al usuario
        return "http://localhost:8091/api/google-calendar/callback";
    }

    // ========================================
    // SECCIÓN TARIFAS ESPECIALES
    // ========================================

    private VerticalLayout createTarifasSection() {
        VerticalLayout section = new VerticalLayout();
        section.setPadding(true);
        section.setSpacing(true);
        section.getStyle()
                .set("background", "var(--lumo-base-color)")
                .set("border-radius", "var(--lumo-border-radius-l)")
                .set("box-shadow", "var(--lumo-box-shadow-s)");

        H3 sectionTitle = new H3("Tarifas Especiales de Renta");
        sectionTitle.getStyle().set("margin-top", "0");

        Paragraph description = new Paragraph(
                "Cargos adicionales que se suman automáticamente al precio de la reserva web según las opciones que el cliente seleccione.");
        description.getStyle()
                .set("color", "var(--lumo-secondary-text-color)")
                .set("font-size", "var(--lumo-font-size-s)");

        Settings settings = settingsService.getSettings();

        tarifaSacarPaisField = new NumberField("Tarifa por sacar el vehículo del país ($/día)");
        tarifaSacarPaisField.setMin(0);
        tarifaSacarPaisField.setStep(0.01);
        tarifaSacarPaisField.setValue(settings.getTarifaSacarPais() != null
                ? settings.getTarifaSacarPais().doubleValue() : 0.0);
        tarifaSacarPaisField.setWidth("280px");
        tarifaSacarPaisField.setHelperText(
                "Se multiplica por los días que el cliente declare estar fuera del país. Poner 0 para deshabilitar.");
        tarifaSacarPaisField.setPrefixComponent(new Span("$"));

        Button saveButton = new Button("Guardar Tarifas", VaadinIcon.CHECK.create());
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveButton.addClickListener(e -> {
            try {
                Settings s = settingsService.getSettings();
                double val = tarifaSacarPaisField.getValue() != null ? tarifaSacarPaisField.getValue() : 0.0;
                s.setTarifaSacarPais(java.math.BigDecimal.valueOf(val));
                settingsService.updateSettings(s);
                Notification.show("Tarifas guardadas correctamente", 3000, Notification.Position.BOTTOM_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } catch (Exception ex) {
                log.error("Error guardando tarifas: {}", ex.getMessage(), ex);
                Notification.show("Error: " + ex.getMessage(), 5000, Notification.Position.BOTTOM_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });

        section.add(sectionTitle, description, tarifaSacarPaisField, saveButton);
        return section;
    }

    private void saveGoogleCalendarSettings() {
        try {
            Settings settings = settingsService.getSettings();
            
            settings.setGoogleCalendarEnabled(googleCalendarEnabledCheckbox.getValue());
            settings.setGoogleClientId(googleClientIdField.getValue());
            
            // Solo actualizar secret si se ingresó uno nuevo
            String newSecret = googleClientSecretField.getValue();
            if (newSecret != null && !newSecret.isEmpty()) {
                settings.setGoogleClientSecret(newSecret);
            }
            
            settingsService.updateSettings(settings);
            
            Notification.show("Configuración de Google Calendar guardada", 3000, 
                    Notification.Position.BOTTOM_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            
            // Recargar para actualizar el estado
            getUI().ifPresent(ui -> ui.getPage().reload());
            
        } catch (Exception e) {
            log.error("Error guardando configuración de Google Calendar: {}", e.getMessage(), e);
            Notification.show("Error: " + e.getMessage(), 5000, Notification.Position.BOTTOM_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }
}
