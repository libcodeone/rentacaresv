package com.rentacaresv.views;

import com.rentacaresv.settings.application.SettingsService;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.component.login.LoginI18n;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import lombok.extern.slf4j.Slf4j;

@Route("login")
@PageTitle("Login | RentaCareSV")
@AnonymousAllowed
@Slf4j
public class LoginView extends VerticalLayout implements BeforeEnterObserver {

    private final LoginForm login = new LoginForm();
    private final SettingsService settingsService;

    public LoginView(SettingsService settingsService) {
        this.settingsService = settingsService;
        
        addClassName("login-view");
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);

        login.setAction("login");
        login.setForgotPasswordButtonVisible(true);
        
        // Configurar textos en español
        LoginI18n i18n = LoginI18n.createDefault();
        LoginI18n.Form i18nForm = i18n.getForm();
        i18nForm.setTitle("Iniciar Sesión");
        i18nForm.setUsername("Usuario");
        i18nForm.setPassword("Contraseña");
        i18nForm.setSubmit("Ingresar");
        i18nForm.setForgotPassword("¿Olvidaste tu contraseña?");
        i18n.setForm(i18nForm);
        
        LoginI18n.ErrorMessage i18nErrorMessage = i18n.getErrorMessage();
        i18nErrorMessage.setTitle("Usuario o contraseña incorrectos");
        i18nErrorMessage.setMessage("Verifica tus credenciales e intenta de nuevo.");
        i18n.setErrorMessage(i18nErrorMessage);
        
        login.setI18n(i18n);
        
        // Manejar clic en "Olvidaste tu contraseña"
        login.addForgotPasswordListener(e -> {
            getUI().ifPresent(ui -> ui.navigate("forgot-password"));
        });

        // Obtener logo dinámicamente
        String logoUrl = getLogoUrl();
        Image logo = new Image(logoUrl, "RentaCareSV Logo");
        logo.setWidth("200px");
        logo.getStyle().set("object-fit", "contain");

        // Obtener nombre de la empresa
        String companyName = getCompanyName();

        add(logo, new H1(companyName), login);
    }

    /**
     * Obtiene la URL del logo desde Settings o usa el logo por defecto
     */
    private String getLogoUrl() {
        try {
            String customLogoUrl = settingsService.getLogoUrl();
            if (customLogoUrl != null && !customLogoUrl.isEmpty()) {
                log.debug("Usando logo personalizado en login: {}", customLogoUrl);
                return customLogoUrl;
            }
        } catch (Exception e) {
            log.warn("Error obteniendo logo personalizado: {}", e.getMessage());
        }
        return "images/logo.png";
    }

    /**
     * Obtiene el nombre de la empresa desde Settings
     */
    private String getCompanyName() {
        try {
            String name = settingsService.getCompanyName();
            if (name != null && !name.isEmpty()) {
                return name;
            }
        } catch (Exception e) {
            log.warn("Error obteniendo nombre de empresa: {}", e.getMessage());
        }
        return "RentaCareSV";
    }

    @Override
    public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
        // inform the user about an authentication error
        if (beforeEnterEvent.getLocation()
                .getQueryParameters()
                .getParameters()
                .containsKey("error")) {
            login.setError(true);
        }
    }
}
