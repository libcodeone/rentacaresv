-- V003: Limpieza de tablas legacy de diagramas de daños
-- Fecha: 2025-03-17
-- Descripción: Elimina tablas obsoletas que fueron reemplazadas por campos de video

-- IMPORTANTE: Este script es IDEMPOTENTE (se puede ejecutar múltiples veces)
-- Las tablas se eliminan solo si existen

-- PASO 1: Eliminar tabla de marcas de daños (si existe)
DROP TABLE IF EXISTS contract_damage_mark;

-- PASO 2: Eliminar tabla de diagramas de vehículos (si existe)  
DROP TABLE IF EXISTS vehicle_diagram;

-- PASO 3: Verificar que las columnas de video existen en contract
-- (estas fueron agregadas en V002, pero verificamos por si acaso)
-- Si no existen, las creamos

SET @col_exists_delivery = (
    SELECT COUNT(*) 
    FROM information_schema.COLUMNS 
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'contract' 
    AND COLUMN_NAME = 'video_delivery_url'
);

SET @col_exists_return = (
    SELECT COUNT(*) 
    FROM information_schema.COLUMNS 
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'contract' 
    AND COLUMN_NAME = 'video_return_url'
);

SET @col_exists_damages = (
    SELECT COUNT(*) 
    FROM information_schema.COLUMNS 
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'contract' 
    AND COLUMN_NAME = 'video_damages_url'
);

-- Agregar columnas si no existen
SET @sql_delivery = IF(@col_exists_delivery = 0,
    'ALTER TABLE contract ADD COLUMN video_delivery_url VARCHAR(500) NULL COMMENT ''URL del video de entrega del vehículo''',
    'SELECT ''Column video_delivery_url already exists'' AS info'
);

SET @sql_return = IF(@col_exists_return = 0,
    'ALTER TABLE contract ADD COLUMN video_return_url VARCHAR(500) NULL COMMENT ''URL del video de devolución del vehículo''',
    'SELECT ''Column video_return_url already exists'' AS info'
);

SET @sql_damages = IF(@col_exists_damages = 0,
    'ALTER TABLE contract ADD COLUMN video_damages_url VARCHAR(500) NULL COMMENT ''URL del video de daños del vehículo''',
    'SELECT ''Column video_damages_url already exists'' AS info'
);

PREPARE stmt_delivery FROM @sql_delivery;
EXECUTE stmt_delivery;
DEALLOCATE PREPARE stmt_delivery;

PREPARE stmt_return FROM @sql_return;
EXECUTE stmt_return;
DEALLOCATE PREPARE stmt_return;

PREPARE stmt_damages FROM @sql_damages;
EXECUTE stmt_damages;
DEALLOCATE PREPARE stmt_damages;

-- Resultado de la migración
SELECT 
    'V003 ejecutada exitosamente' AS status,
    'Tablas legacy eliminadas' AS accion,
    'contract_damage_mark, vehicle_diagram' AS tablas_eliminadas;
