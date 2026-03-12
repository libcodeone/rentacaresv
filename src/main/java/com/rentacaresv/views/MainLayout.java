package com.rentacaresv.views;

import com.rentacaresv.security.AuthenticatedUser;
import com.rentacaresv.settings.application.SettingsService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Footer;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Header;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.SvgIcon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.dom.ThemeList;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationObserver;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.menu.MenuConfiguration;
import com.vaadin.flow.server.menu.MenuEntry;
import com.vaadin.flow.theme.lumo.Lumo;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.PermitAll;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;

/**
 * Layout principal de la aplicación
 */
@PermitAll
@Slf4j
public class MainLayout extends AppLayout implements AfterNavigationObserver {

    private H1 viewTitle;
    private final AuthenticatedUser authenticatedUser;
    private final SettingsService settingsService;
    private Button themeToggleButton;
    private boolean isDarkMode = false;

    private static final String THEME_PREFERENCE_KEY = "theme-preference";

    public MainLayout(AuthenticatedUser authenticatedUser, SettingsService settingsService) {
        this.authenticatedUser = authenticatedUser;
        this.settingsService = settingsService;
        setPrimarySection(Section.DRAWER);
        addDrawerContent();
        addHeaderContent();
        
        // Cargar preferencia de tema guardada
        loadThemePreference();
    }

    private void addHeaderContent() {
        DrawerToggle toggle = new DrawerToggle();
        toggle.setAriaLabel("Menu toggle");

        viewTitle = new H1();
        viewTitle.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.Margin.NONE);

        // Spacer para empujar el toggle a la derecha
        Div spacer = new Div();
        spacer.getStyle().set("flex-grow", "1");

        // Botón de toggle de tema
        themeToggleButton = new Button();
        themeToggleButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON);
        themeToggleButton.getStyle()
            .set("margin-right", "var(--lumo-space-m)")
            .set("cursor", "pointer");
        themeToggleButton.addClickListener(e -> toggleTheme());
        updateThemeButtonIcon();

        // Layout del header
        HorizontalLayout headerLayout = new HorizontalLayout(toggle, viewTitle, spacer, themeToggleButton);
        headerLayout.setWidthFull();
        headerLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        headerLayout.setPadding(false);
        headerLayout.setSpacing(false);
        headerLayout.getStyle().set("padding-right", "var(--lumo-space-s)");

        addToNavbar(true, headerLayout);
    }

    private void addDrawerContent() {
        // Contenedor para el logo
        Div logoContainer = new Div();
        logoContainer.getStyle()
            .set("display", "flex")
            .set("align-items", "center")
            .set("justify-content", "center")
            .set("padding", "var(--lumo-space-m)")
            .set("background", "var(--lumo-contrast-5pct)")
            .set("border-bottom", "1px solid var(--lumo-contrast-10pct)")
            .set("height", "64px")
            .set("overflow", "hidden");

        // Obtener logo dinámicamente
        String logoUrl = getLogoUrl();
        
        Image logo = new Image(logoUrl, "Logo");
        logo.getStyle()
            .set("height", "56px")
            .set("width", "auto")
            .set("object-fit", "contain")
            .set("object-position", "center");
        
        logoContainer.add(logo);

        Header header = new Header(logoContainer);

        Scroller scroller = new Scroller(createNavigation());

        addToDrawer(header, scroller, createFooter());
    }

    /**
     * Obtiene la URL del logo desde Settings o usa el logo por defecto
     */
    private String getLogoUrl() {
        try {
            String customLogoUrl = settingsService.getLogoUrl();
            if (customLogoUrl != null && !customLogoUrl.isEmpty()) {
                log.debug("Usando logo personalizado: {}", customLogoUrl);
                return customLogoUrl;
            }
        } catch (Exception e) {
            log.warn("Error obteniendo logo personalizado: {}", e.getMessage());
        }
        return "images/logo.png";
    }

    private SideNav createNavigation() {
        SideNav nav = new SideNav();

        List<MenuEntry> menuEntries = MenuConfiguration.getMenuEntries();
        
        // Crear submenú de configuración
        SideNavItem settingsParent = null;
        
        for (MenuEntry entry : menuEntries) {
            String path = entry.path();
            
            // Agrupar vistas bajo settings/ en un submenú
            if (path.startsWith("settings/") && !path.equals("settings")) {
                // Si es la primera vez, crear el padre
                if (settingsParent == null) {
                    // Buscar la entrada principal de settings
                    MenuEntry settingsEntry = menuEntries.stream()
                            .filter(e -> e.path().equals("settings"))
                            .findFirst()
                            .orElse(null);
                    
                    if (settingsEntry != null && settingsEntry.icon() != null) {
                        settingsParent = new SideNavItem("Configuración");
                        settingsParent.setPrefixComponent(new SvgIcon(settingsEntry.icon()));
                        // Agregar la vista principal de settings como hijo
                        SideNavItem mainSettings = new SideNavItem("Sistema", "settings", VaadinIcon.COG.create());
                        settingsParent.addItem(mainSettings);
                        nav.addItem(settingsParent);
                    }
                }
                
                // Agregar como hijo del submenú
                if (settingsParent != null) {
                    SideNavItem childItem;
                    if (entry.icon() != null) {
                        childItem = new SideNavItem(entry.title(), path, new SvgIcon(entry.icon()));
                    } else {
                        childItem = new SideNavItem(entry.title(), path);
                    }
                    settingsParent.addItem(childItem);
                }
            } else if (!path.equals("settings")) {
                // Vistas normales (no settings)
                if (entry.icon() != null) {
                    nav.addItem(new SideNavItem(entry.title(), path, new SvgIcon(entry.icon())));
                } else {
                    nav.addItem(new SideNavItem(entry.title(), path));
                }
            }
        }
        
        // Si no se encontraron subitems de settings, agregar settings normal
        if (settingsParent == null) {
            menuEntries.stream()
                    .filter(e -> e.path().equals("settings"))
                    .findFirst()
                    .ifPresent(entry -> {
                        if (entry.icon() != null) {
                            nav.addItem(new SideNavItem(entry.title(), entry.path(), new SvgIcon(entry.icon())));
                        } else {
                            nav.addItem(new SideNavItem(entry.title(), entry.path()));
                        }
                    });
        }

        return nav;
    }

    private Footer createFooter() {
        Footer layout = new Footer();

        Optional<com.rentacaresv.user.domain.User> maybeUser = authenticatedUser.get();
        if (maybeUser.isPresent()) {
            com.rentacaresv.user.domain.User user = maybeUser.get();

            Avatar avatar = new Avatar(user.getName());
            avatar.setThemeName("xsmall");
            avatar.getElement().setAttribute("tabindex", "-1");

            MenuBar userMenu = new MenuBar();
            userMenu.setThemeName("tertiary-inline luate-user-menu");

            MenuItem userName = userMenu.addItem("");
            Div div = new Div();
            div.add(avatar);
            div.add(user.getName());
            div.add(new Icon("lumo", "dropdown"));
            div.getElement().getStyle().set("display", "flex");
            div.getElement().getStyle().set("align-items", "center");
            div.getElement().getStyle().set("gap", "var(--lumo-space-s)");
            userName.add(div);
            userName.getSubMenu().addItem("Cerrar Sesión", e -> {
                authenticatedUser.logout();
            });

            layout.add(userMenu);
        } else {
            Anchor loginLink = new Anchor("login", "Iniciar Sesión");
            layout.add(loginLink);
        }

        return layout;
    }

    /**
     * Cambia entre modo oscuro y claro
     */
    private void toggleTheme() {
        isDarkMode = !isDarkMode;
        applyTheme();
        saveThemePreference();
        updateThemeButtonIcon();
    }

    /**
     * Aplica el tema actual al UI
     */
    private void applyTheme() {
        UI ui = UI.getCurrent();
        if (ui != null) {
            ThemeList themeList = ui.getElement().getThemeList();
            if (isDarkMode) {
                themeList.add(Lumo.DARK);
            } else {
                themeList.remove(Lumo.DARK);
            }
        }
    }

    /**
     * Actualiza el ícono del botón según el tema actual
     */
    private void updateThemeButtonIcon() {
        if (isDarkMode) {
            // En modo oscuro, mostrar ícono de sol (para cambiar a claro)
            themeToggleButton.setIcon(VaadinIcon.SUN_O.create());
            themeToggleButton.setTooltipText("Cambiar a modo claro");
        } else {
            // En modo claro, mostrar ícono de luna (para cambiar a oscuro)
            themeToggleButton.setIcon(VaadinIcon.MOON_O.create());
            themeToggleButton.setTooltipText("Cambiar a modo oscuro");
        }
    }

    /**
     * Guarda la preferencia de tema en la sesión
     */
    private void saveThemePreference() {
        VaadinSession session = VaadinSession.getCurrent();
        if (session != null) {
            session.setAttribute(THEME_PREFERENCE_KEY, isDarkMode);
        }
        
        // También guardar en localStorage del navegador para persistencia
        UI ui = UI.getCurrent();
        if (ui != null) {
            ui.getPage().executeJs(
                "localStorage.setItem('theme-preference', $0 ? 'dark' : 'light');",
                isDarkMode
            );
        }
    }

    /**
     * Carga la preferencia de tema guardada
     */
    private void loadThemePreference() {
        // Primero intentar cargar de la sesión
        VaadinSession session = VaadinSession.getCurrent();
        if (session != null) {
            Boolean savedPreference = (Boolean) session.getAttribute(THEME_PREFERENCE_KEY);
            if (savedPreference != null) {
                isDarkMode = savedPreference;
                applyTheme();
                updateThemeButtonIcon();
                return;
            }
        }
        
        // Si no hay en sesión, cargar de localStorage
        UI ui = UI.getCurrent();
        if (ui != null) {
            ui.getPage().executeJs(
                "return localStorage.getItem('theme-preference');"
            ).then(String.class, preference -> {
                if ("dark".equals(preference)) {
                    isDarkMode = true;
                    applyTheme();
                    updateThemeButtonIcon();
                    // También guardar en sesión
                    if (session != null) {
                        session.setAttribute(THEME_PREFERENCE_KEY, true);
                    }
                }
            });
        }
    }

    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        viewTitle.setText(getCurrentPageTitle());
    }

    private String getCurrentPageTitle() {
        return MenuConfiguration.getPageHeader(getContent()).orElse("");
    }
}
