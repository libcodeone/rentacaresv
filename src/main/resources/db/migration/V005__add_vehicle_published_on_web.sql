-- Agregar campo para controlar visibilidad del vehículo en la página web pública
ALTER TABLE vehicle ADD COLUMN published_on_web BOOLEAN NOT NULL DEFAULT TRUE;
