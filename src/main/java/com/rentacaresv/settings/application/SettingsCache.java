package com.rentacaresv.settings.application;

import com.rentacaresv.settings.domain.Settings;
import com.rentacaresv.settings.infrastructure.SettingsRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Cache singleton para la configuración global del sistema.
 * Carga la configuración al inicio de la aplicación y la mantiene en memoria.
 * Se actualiza manualmente cuando hay cambios.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SettingsCache {

    private final SettingsRepository settingsRepository;
    
    private Settings cachedSettings;
    private final Object lock = new Object();

    @PostConstruct
    public void init() {
        log.info("🚀 Inicializando cache de configuración...");
        loadSettings();
    }

    /**
     * Carga o recarga la configuración desde la base de datos
     */
    public void loadSettings() {
        synchronized (lock) {
            var all = settingsRepository.findAll();
            
            if (all.isEmpty()) {
                log.info("ℹ️ No se encontró configuración. Creando configuración inicial...");
                Settings settings = new Settings();
                settings.setTenantId(UUID.randomUUID().toString());
                settings.setCompanyName("RentaCarESV");
                settings.setFoldersInitialized(false);
                settings.setCreatedAt(LocalDateTime.now());
                cachedSettings = settingsRepository.save(settings);
                log.info("✅ Configuración inicial creada con tenant_id: {}", cachedSettings.getTenantId());
            } else {
                cachedSettings = all.get(0);
                
                // Self-healing: reparar tenant_id vacío
                if (cachedSettings.getTenantId() == null || cachedSettings.getTenantId().trim().isEmpty()) {
                    log.warn("⚠️ FOUND EMPTY TENANT_ID. REPAIRING RECORD...");
                    cachedSettings.setTenantId(UUID.randomUUID().toString());
                    cachedSettings = settingsRepository.save(cachedSettings);
                    log.info("✅ TENANT_ID REPAIRED: {}", cachedSettings.getTenantId());
                }
                
                log.info("✅ Configuración cargada - Empresa: {}, Tenant: {}", 
                        cachedSettings.getCompanyName(), 
                        cachedSettings.getTenantId());
            }
        }
    }

    /**
     * Obtiene la configuración desde cache (sin consultar BD)
     */
    public Settings getSettings() {
        synchronized (lock) {
            if (cachedSettings == null) {
                loadSettings();
            }
            return cachedSettings;
        }
    }

    /**
     * Obtiene el tenant ID desde cache
     */
    public String getTenantId() {
        return getSettings().getTenantId();
    }

    /**
     * Obtiene la URL del logo desde cache
     */
    public String getLogoUrl() {
        return getSettings().getLogoUrl();
    }

    /**
     * Obtiene el nombre de la empresa desde cache
     */
    public String getCompanyName() {
        String name = getSettings().getCompanyName();
        return (name != null && !name.isEmpty()) ? name : "RentaCarESV";
    }

    /**
     * Verifica si las carpetas están inicializadas
     */
    public boolean areFoldersInitialized() {
        return Boolean.TRUE.equals(getSettings().getFoldersInitialized());
    }

    /**
     * Actualiza la configuración en cache y en BD
     */
    public void updateSettings(Settings settings) {
        synchronized (lock) {
            cachedSettings = settingsRepository.save(settings);
            log.info("✅ Cache de configuración actualizado");
        }
    }

    /**
     * Invalida el cache forzando recarga en próxima consulta
     */
    public void invalidate() {
        synchronized (lock) {
            cachedSettings = null;
            log.info("🔄 Cache de configuración invalidado");
        }
    }

    /**
     * Recarga el cache desde la BD
     */
    public void refresh() {
        log.info("🔄 Refrescando cache de configuración...");
        loadSettings();
    }
}
