package com.rentacaresv.shared.storage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Inicializador manual de almacenamiento
 * Se llama explícitamente cuando se necesita
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class StorageInitializer {

    private final FileStorageService fileStorageService;
    private boolean initialized = false;

    /**
     * Inicializa el sistema de almacenamiento manualmente
     * Este método es idempotente - puede llamarse múltiples veces
     */
    public synchronized void initializeIfNeeded() {
        if (initialized) {
            log.debug("Sistema de almacenamiento ya inicializado, saltando...");
            return;
        }

        log.info("=".repeat(60));
        log.info("Verificando sistema de almacenamiento...");
        log.info("=".repeat(60));

        try {
            if (fileStorageService.isInitialized()) {
                // Ya no creamos carpetas automáticamente
                // Las carpetas se crearán cuando se suba el primer archivo
                initialized = true;
                log.info("✅ Sistema de almacenamiento listo");
                log.info("ℹ️  Las carpetas se crearán automáticamente al subir archivos");
            } else {
                log.warn("⚠️  FileStorageService no está inicializado");
                log.warn("⚠️  Verifica la configuración de Digital Ocean Spaces");
            }
        } catch (Exception e) {
            log.error("❌ Error verificando sistema de almacenamiento: {}", e.getMessage(), e);
        }

        log.info("=".repeat(60));
    }

    public boolean isInitialized() {
        return initialized;
    }
}
