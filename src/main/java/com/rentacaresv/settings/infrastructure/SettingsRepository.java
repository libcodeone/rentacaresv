package com.rentacaresv.settings.infrastructure;

import com.rentacaresv.settings.domain.Settings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

/**
 * Repositorio de Settings
 */
public interface SettingsRepository extends JpaRepository<Settings, Long> {

    /**
     * Busca la configuración por tenant ID
     */
    Optional<Settings> findByTenantId(String tenantId);

    /**
     * Obtiene la configuración global (solo debería haber una)
     */
    @Query("SELECT s FROM Settings s ORDER BY s.id ASC")
    Optional<Settings> findGlobalSettings();

    /**
     * Verifica si ya existe configuración
     */
    boolean existsByTenantId(String tenantId);
}
