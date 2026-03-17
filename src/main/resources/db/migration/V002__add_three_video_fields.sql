-- V002: Agregar 3 campos de video del veh\u00edculo
-- Fecha: 2025-01
-- Descripci\u00f3n: Reemplaza el campo \u00fanico vehicle_video_url por 3 campos espec\u00edficos:
--              - vehicle_exterior_video_url (Exterior)
--              - vehicle_interior_video_url (Interior)
--              - vehicle_details_video_url (Otros Detalles)

-- PASO 1: Agregar las 3 nuevas columnas
ALTER TABLE contract 
ADD COLUMN vehicle_exterior_video_url VARCHAR(500) NULL
COMMENT 'URL del video del EXTERIOR del veh\u00edculo al momento de la entrega';

ALTER TABLE contract 
ADD COLUMN vehicle_interior_video_url VARCHAR(500) NULL
COMMENT 'URL del video del INTERIOR del veh\u00edculo al momento de la entrega';

ALTER TABLE contract 
ADD COLUMN vehicle_details_video_url VARCHAR(500) NULL
COMMENT 'URL del video de OTROS DETALLES del veh\u00edculo al momento de la entrega';

-- PASO 2: Migrar datos existentes
-- Si ya existe un video en el campo antiguo, moverlo al campo de EXTERIOR
UPDATE contract 
SET vehicle_exterior_video_url = vehicle_video_url,
    updated_at = NOW()
WHERE vehicle_video_url IS NOT NULL 
  AND vehicle_video_url != ''
  AND vehicle_exterior_video_url IS NULL;

-- NOTA: El campo vehicle_video_url se mantiene para compatibilidad
-- con contratos antiguos pero ya no se usar\u00e1 en nuevos contratos
