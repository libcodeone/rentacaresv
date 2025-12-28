package com.rentacaresv.views.home;

import com.rentacaresv.security.AuthenticatedUser;
import com.rentacaresv.views.MainLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import lombok.extern.slf4j.Slf4j;
import org.vaadin.lineawesome.LineAwesomeIconUrl;

/**
 * Vista principal de inicio del sistema RentaCar ESV
 * Primera pantalla que ve el usuario después de hacer login
 */
@PageTitle("Inicio")
@Route(value = "", layout = MainLayout.class)
@Menu(order = 0, icon = LineAwesomeIconUrl.HOME_SOLID)
@PermitAll
@Slf4j
public class HomeView extends VerticalLayout {

    private final AuthenticatedUser authenticatedUser;

    public HomeView(AuthenticatedUser authenticatedUser) {
        this.authenticatedUser = authenticatedUser;
        
        // Actualizar último login
        authenticatedUser.updateLastLogin();
        
        setSpacing(true);
        setPadding(true);
        
        // Header
        H1 title = new H1("Bienvenido a RentaCar ESV");
        title.getStyle()
            .set("margin-top", "0")
            .set("color", "var(--lumo-primary-text-color)");
        
        // Mensaje personalizado
        Div welcomeMessage = new Div();
        authenticatedUser.get().ifPresent(user -> {
            H2 greeting = new H2("¡Hola, " + user.getName() + "!");
            greeting.getStyle().set("color", "var(--lumo-secondary-text-color)");
            
            Paragraph roleInfo = new Paragraph(
                "Rol: " + (user.isAdmin() ? "Administrador" : "Operador")
            );
            roleInfo.getStyle()
                .set("color", "var(--lumo-tertiary-text-color)")
                .set("font-size", "var(--lumo-font-size-s)");
            
            welcomeMessage.add(greeting, roleInfo);
            
            log.info("Usuario {} ({}) accedió al sistema", user.getUsername(), user.getName());
        });
        
        // Información del sistema
        Paragraph info = new Paragraph(
            "Sistema de gestión de rentas de vehículos. " +
            "Utiliza el menú lateral para navegar entre las diferentes secciones."
        );
        info.getStyle().set("max-width", "800px");
        
        // Sección de acciones rápidas (placeholder por ahora)
        Div quickActions = new Div();
        H2 quickActionsTitle = new H2("Acciones Rápidas");
        quickActionsTitle.getStyle().set("margin-top", "var(--lumo-space-l)");
        
        Paragraph placeholder = new Paragraph(
            "Próximamente: accesos directos a funciones principales como " +
            "crear nueva renta, registrar pago, entregar vehículo, etc."
        );
        placeholder.getStyle()
            .set("color", "var(--lumo-secondary-text-color)")
            .set("font-style", "italic");
        
        quickActions.add(quickActionsTitle, placeholder);
        
        // Agregar todos los componentes
        add(title, welcomeMessage, info, quickActions);
    }
}
