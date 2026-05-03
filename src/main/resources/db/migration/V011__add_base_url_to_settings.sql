-- Agregar columna base_url a la tabla settings
ALTER TABLE settings ADD COLUMN IF NOT EXISTS base_url VARCHAR(500) NULL;
