-- V001: Cambiar tipo de veh\u00edculo VAN a MICROBUS
-- Fecha: 2025-01
-- Descripci\u00f3n: Actualiza el tipo de veh\u00edculo VAN por MICROBUS

-- PASO 1: Ampliar el tama\u00f1o de la columna vehicle_type
ALTER TABLE vehicle 
MODIFY COLUMN vehicle_type VARCHAR(20) NULL;

ALTER TABLE vehicle_model 
MODIFY COLUMN vehicle_type VARCHAR(20) NULL;

-- PASO 2: Actualizar veh\u00edculos existentes de VAN a MICROBUS
UPDATE vehicle 
SET vehicle_type = 'MICROBUS',
    updated_at = NOW()
WHERE vehicle_type = 'VAN';

-- PASO 3: Actualizar modelos de veh\u00edculos de VAN a MICROBUS
UPDATE vehicle_model
SET vehicle_type = 'MICROBUS',
    updated_at = NOW()
WHERE vehicle_type = 'VAN';
