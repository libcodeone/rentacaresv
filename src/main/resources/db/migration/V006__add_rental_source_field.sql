-- Agregar campo source para identificar origen de la reserva (admin o web)
ALTER TABLE rental ADD COLUMN source VARCHAR(20) NOT NULL DEFAULT 'ADMIN';
