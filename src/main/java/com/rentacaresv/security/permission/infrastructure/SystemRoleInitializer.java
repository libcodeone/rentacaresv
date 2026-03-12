package com.rentacaresv.security.permission.infrastructure;

import com.rentacaresv.security.permission.application.RolePermissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Inicializador de roles del sistema.
 * Se ejecuta al arrancar la aplicación para asegurar que los roles básicos existen.
 */
@Component
@Order(1) // Ejecutar temprano
@RequiredArgsConstructor
@Slf4j
public class SystemRoleInitializer implements CommandLineRunner {

    private final RolePermissionService rolePermissionService;

    @Override
    public void run(String... args) {
        log.info("Inicializando roles del sistema...");
        try {
            rolePermissionService.initializeSystemRoles();
            log.info("Roles del sistema inicializados correctamente");
        } catch (Exception e) {
            log.error("Error inicializando roles del sistema: {}", e.getMessage(), e);
        }
    }
}
