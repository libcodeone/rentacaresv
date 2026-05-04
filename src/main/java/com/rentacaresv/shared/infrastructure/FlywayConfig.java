package com.rentacaresv.shared.infrastructure;

import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.springframework.boot.flyway.autoconfigure.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configura Flyway para reparar automáticamente migraciones fallidas antes de migrar.
 * Esto elimina la necesidad de correr manualmente DELETE FROM flyway_schema_history
 * cuando una migración falla a mitad del proceso.
 */
@Configuration
@Slf4j
public class FlywayConfig {

    @Bean
    public FlywayMigrationStrategy repairThenMigrate() {
        return flyway -> {
            log.info("Flyway: ejecutando repair antes de migrate para limpiar migraciones fallidas...");
            flyway.repair();
            log.info("Flyway: repair completado, iniciando migrate...");
            flyway.migrate();
        };
    }
}
