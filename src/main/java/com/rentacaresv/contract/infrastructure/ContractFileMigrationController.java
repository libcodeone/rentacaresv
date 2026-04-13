package com.rentacaresv.contract.infrastructure;

import com.rentacaresv.contract.application.ContractFileMigrationService;
import com.rentacaresv.contract.application.ContractFileMigrationService.MigrationResult;
import jakarta.annotation.security.RolesAllowed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Endpoint ONE-TIME para migrar archivos de contratos existentes
 * a la nueva estructura de subcarpetas en DO Spaces.
 *
 * Uso:
 *   POST /api/admin/migrate/contract-files
 *   (requiere sesión autenticada con rol ADMIN)
 *
 * ⚠️ Ejecutar UNA SOLA VEZ después de desplegar los cambios.
 *    Las llamadas subsiguientes son seguras (los archivos ya en subcarpeta
 *    se detectan como "omitidos" sin volver a moverse).
 */
@RestController
@RequestMapping("/api/admin/migrate")
@RolesAllowed("ADMIN")
@RequiredArgsConstructor
@Slf4j
public class ContractFileMigrationController {

    private final ContractFileMigrationService migrationService;

    @PostMapping("/contract-files")
    public ResponseEntity<Map<String, Object>> migrateContractFiles() {
        log.info("🚀 Migración de archivos iniciada por request HTTP");

        MigrationResult result = migrationService.migrateAll();

        Map<String, Object> response = Map.of(
                "status",  result.hasErrors() ? "COMPLETED_WITH_ERRORS" : "OK",
                "moved",   result.moved(),
                "skipped", result.skipped(),
                "errors",  result.errors(),
                "log",     result.logs()
        );

        log.info("✅ Migración finalizada — movidos: {}, omitidos: {}, errores: {}",
                result.moved(), result.skipped(), result.errors());

        return ResponseEntity.ok(response);
    }
}
