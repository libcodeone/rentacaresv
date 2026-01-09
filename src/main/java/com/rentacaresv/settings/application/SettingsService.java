package com.rentacaresv.settings.application;

import com.rentacaresv.settings.domain.Settings;
import com.rentacaresv.shared.storage.FileStorageService;
import com.rentacaresv.shared.storage.FolderType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;

/**
 * Servicio de aplicación para gestión de Settings.
 * Utiliza SettingsCache para evitar consultas repetidas a la BD.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SettingsService {

    private final SettingsCache settingsCache;
    private final FileStorageService fileStorageService;

    /**
     * Obtiene la configuración global desde cache
     */
    public Settings getSettings() {
        return settingsCache.getSettings();
    }

    /**
     * Obtiene la URL del logo actual desde cache
     */
    public String getLogoUrl() {
        return settingsCache.getLogoUrl();
    }

    /**
     * Obtiene el nombre de la empresa desde cache
     */
    public String getCompanyName() {
        return settingsCache.getCompanyName();
    }

    /**
     * Obtiene el tenant ID desde cache
     */
    public String getTenantId() {
        return settingsCache.getTenantId();
    }

    /**
     * Sube un nuevo logo y actualiza la configuración
     */
    @Transactional
    public String uploadLogo(InputStream inputStream, String fileName, String contentType) {
        log.info("Subiendo nuevo logo: {}", fileName);

        Settings settings = settingsCache.getSettings();

        // Eliminar logo anterior si existe
        String currentLogoUrl = settings.getLogoUrl();
        if (currentLogoUrl != null && !currentLogoUrl.isEmpty()) {
            try {
                fileStorageService.deleteFile(currentLogoUrl);
                log.info("Logo anterior eliminado: {}", currentLogoUrl);
            } catch (Exception e) {
                log.warn("No se pudo eliminar el logo anterior: {}", e.getMessage());
            }
        }

        // Subir nuevo logo a la carpeta settings
        String newLogoUrl = fileStorageService.uploadFile(
                inputStream,
                fileName,
                contentType,
                FolderType.SETTINGS,
                "logo"
        );

        // Actualizar en BD y cache
        settings.updateLogoUrl(newLogoUrl);
        settingsCache.updateSettings(settings);

        log.info("Nuevo logo guardado: {}", newLogoUrl);
        return newLogoUrl;
    }

    /**
     * Elimina el logo actual y vuelve al logo por defecto
     */
    @Transactional
    public void resetLogo() {
        log.info("Reseteando logo al valor por defecto");

        Settings settings = settingsCache.getSettings();

        // Eliminar logo actual si existe en DO Spaces
        String currentLogoUrl = settings.getLogoUrl();
        if (currentLogoUrl != null && !currentLogoUrl.isEmpty()) {
            try {
                fileStorageService.deleteFile(currentLogoUrl);
                log.info("Logo eliminado de DO Spaces: {}", currentLogoUrl);
            } catch (Exception e) {
                log.warn("No se pudo eliminar el logo: {}", e.getMessage());
            }
        }

        // Resetear a null y actualizar cache
        settings.updateLogoUrl(null);
        settingsCache.updateSettings(settings);

        log.info("Logo reseteado al valor por defecto");
    }

    /**
     * Actualiza el nombre de la empresa
     */
    @Transactional
    public void updateCompanyName(String companyName) {
        Settings settings = settingsCache.getSettings();
        settings.setCompanyName(companyName);
        settingsCache.updateSettings(settings);
        log.info("Nombre de empresa actualizado: {}", companyName);
    }

    /**
     * Fuerza recarga del cache desde BD
     */
    public void refreshCache() {
        settingsCache.refresh();
    }
}
