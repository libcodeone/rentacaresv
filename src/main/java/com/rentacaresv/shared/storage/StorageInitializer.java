package com.rentacaresv.shared.storage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Inicializador de carpetas en Digital Ocean Spaces
 * Se ejecuta automáticamente al iniciar la aplicación
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class StorageInitializer {

    private final FileStorageService fileStorageService;

    /**
     * Inicializa las carpetas del tenant cuando la aplicación está lista
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("=".repeat(60));
        log.info("Inicializando sistema de almacenamiento...");
        log.info("=".repeat(60));

        try {
            fileStorageService.initializeTenantFolders();
            log.info("Sistema de almacenamiento inicializado correctamente");
        } catch (Exception e) {
            log.error("Error inicializando sistema de almacenamiento: {}", e.getMessage(), e);
        }

        log.info("=".repeat(60));
    }
}
