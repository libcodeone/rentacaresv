package com.rentacaresv;

import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.theme.Theme;
import com.vaadin.flow.theme.lumo.Lumo;
import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * The entry point of the Spring Boot application.
 */
@SpringBootApplication
@org.springframework.boot.persistence.autoconfigure.EntityScan(basePackages = "com.rentacaresv")
@EnableJpaRepositories(basePackages = "com.rentacaresv")
@EnableAsync
@EnableScheduling
@StyleSheet(Lumo.STYLESHEET)
@StyleSheet("styles.css")
@Theme(value = "rentacaresv")
public class Application implements AppShellConfigurator {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    /**
     * Configura la zona horaria por defecto de la aplicación a El Salvador
     */
    @PostConstruct
    public void init() {
        java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("America/El_Salvador"));
    }
}
