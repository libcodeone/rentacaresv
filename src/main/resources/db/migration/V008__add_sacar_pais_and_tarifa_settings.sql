-- V008: Campos para salida del país en rentas y tarifa configurable en settings
-- Autor: Sistema | Fecha: 2026-04-14

-- ============================================================
-- Tabla: rental — campos de salida del país
-- ============================================================

ALTER TABLE rental
    ADD COLUMN IF NOT EXISTS sacar_pais            BOOLEAN        NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS destinos_fuera_pais   VARCHAR(300)   NULL,
    ADD COLUMN IF NOT EXISTS dias_fuera_pais       INT            NULL,
    ADD COLUMN IF NOT EXISTS cargo_sacar_pais      DECIMAL(10, 2) NULL;

-- ============================================================
-- Tabla: settings — tarifa diaria por sacar vehículo del país
-- ============================================================

ALTER TABLE settings
    ADD COLUMN IF NOT EXISTS tarifa_sacar_pais DECIMAL(10, 2) NULL;
