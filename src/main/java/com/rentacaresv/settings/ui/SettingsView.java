package com.rentacaresv.settings.ui;

import com.rentacaresv.settings.application.SettingsService;
import com.rentacaresv.settings.domain.Settings;
import com.rentacaresv.views.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
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
    
    private Image logoPreview;
    private TextField companyNameField;
    private Span logoStatusSpan;

    public SettingsView(SettingsService settingsService) {
        this.settingsService = settingsService;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        H2 title = new H2("Configuración del Sistema");
        title.getStyle().set("margin-top", "0");

        add(title, createLogoSection(), createCompanySection());
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
}
