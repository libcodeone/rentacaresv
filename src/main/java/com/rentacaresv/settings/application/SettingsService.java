package com.rentacaresv.settings.application;

import com.rentacaresv.settings.domain.Settings;
import com.rentacaresv.settings.infrastructure.SettingsRepository;
import com.rentacaresv.shared.storage.FileStorageService;
import com.rentacaresv.shared.storage.FolderType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;

/**
 * Servicio de aplicación para gestión de Settings
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SettingsService {

    private final SettingsRepository settingsRepository;
    private final FileStorageService fileStorageService;

    /**
     * Obtiene la configuración global
     */
    public Settings getSettings() {
        return settingsRepository.findGlobalSettings()
                .orElseThrow(() -> new IllegalStateException("No se encontró configuración global"));
    }

    /**
     * Obtiene la URL del logo actual
     */
    public String getLogoUrl() {
        return settingsRepository.findGlobalSettings()
                .map(Settings::getLogoUrl)
                .orElse(null);
    }

    /**
     * Sube un nuevo logo y actualiza la configuración
     */
    @Transactional
    public String uploadLogo(InputStream inputStream, String fileName, String contentType) {
        log.info("Subiendo nuevo logo: {}", fileName);

        Settings settings = getSettings();

        // Eliminar logo anterior si existe
        String currentLogoUrl = settings.getLogoUrl();
        if (currentLogoUrl != null && !currentLogoUrl.isEmpty() && !currentLogoUrl.contains("images/logo.png")) {
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

        // Actualizar en base de datos
        settings.updateLogoUrl(newLogoUrl);
        settingsRepository.save(settings);

        log.info("Nuevo logo guardado: {}", newLogoUrl);
        return newLogoUrl;
    }

    /**
     * Elimina el logo actual y vuelve al logo por defecto
     */
    @Transactional
    public void resetLogo() {
        log.info("Reseteando logo al valor por defecto");

        Settings settings = getSettings();

        // Eliminar logo actual si existe en DO Spaces
        String currentLogoUrl = settings.getLogoUrl();
        if (currentLogoUrl != null && !currentLogoUrl.isEmpty() && !currentLogoUrl.contains("images/logo.png")) {
            try {
                fileStorageService.deleteFile(currentLogoUrl);
                log.info("Logo eliminado de DO Spaces: {}", currentLogoUrl);
            } catch (Exception e) {
                log.warn("No se pudo eliminar el logo: {}", e.getMessage());
            }
        }

        // Resetear a null (usará el logo por defecto)
        settings.updateLogoUrl(null);
        settingsRepository.save(settings);

        log.info("Logo reseteado al valor por defecto");
    }

    /**
     * Actualiza el nombre de la empresa
     */
    @Transactional
    public void updateCompanyName(String companyName) {
        Settings settings = getSettings();
        settings.setCompanyName(companyName);
        settingsRepository.save(settings);
        log.info("Nombre de empresa actualizado: {}", companyName);
    }

    /**
     * Obtiene el nombre de la empresa
     */
    public String getCompanyName() {
        return settingsRepository.findGlobalSettings()
                .map(Settings::getCompanyName)
                .orElse("RentaCarESV");
    }
}
