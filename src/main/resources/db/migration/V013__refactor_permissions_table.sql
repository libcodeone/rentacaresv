-- V013: Refactoriza permisos de columna ENUM a tabla relacional con FKs
-- Totalmente idempotente: usa IF NOT EXISTS / IF EXISTS / DDL condicional
-- para tolerar ejecuciones parciales previas en cualquier estado.

-- 1. Crear tabla permission (IF NOT EXISTS por si ya existe)
CREATE TABLE IF NOT EXISTS permission (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    name         VARCHAR(100) NOT NULL,
    display_name VARCHAR(200) NOT NULL,
    description  VARCHAR(500),
    category     VARCHAR(100) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_permission_name (name)
);

-- 2. Insertar permisos (INSERT IGNORE omite duplicados)
INSERT IGNORE INTO permission (name, display_name, description, category) VALUES
-- Vehículos
('VEHICLE_VIEW',    'Ver vehículos',      'Permite ver el listado de vehículos',             'Vehículos'),
('VEHICLE_CREATE',  'Crear vehículos',    'Permite agregar nuevos vehículos',                'Vehículos'),
('VEHICLE_EDIT',    'Editar vehículos',   'Permite modificar información de vehículos',      'Vehículos'),
('VEHICLE_DELETE',  'Eliminar vehículos', 'Permite eliminar vehículos del sistema',          'Vehículos'),
-- Clientes
('CUSTOMER_VIEW',   'Ver clientes',       'Permite ver el listado de clientes',              'Clientes'),
('CUSTOMER_CREATE', 'Crear clientes',     'Permite agregar nuevos clientes',                 'Clientes'),
('CUSTOMER_EDIT',   'Editar clientes',    'Permite modificar información de clientes',       'Clientes'),
('CUSTOMER_DELETE', 'Eliminar clientes',  'Permite eliminar clientes del sistema',           'Clientes'),
-- Rentas
('RENTAL_VIEW',     'Ver rentas',              'Permite ver el listado de rentas',                        'Rentas'),
('RENTAL_CREATE',   'Crear rentas',            'Permite crear nuevas rentas',                             'Rentas'),
('RENTAL_EDIT',     'Editar rentas',           'Permite modificar información de rentas',                 'Rentas'),
('RENTAL_DELETE',   'Eliminar rentas',         'Permite eliminar rentas del sistema',                     'Rentas'),
('RENTAL_DELIVER',  'Entregar vehículos',      'Permite registrar la entrega de un vehículo al cliente',  'Rentas'),
('RENTAL_COMPLETE', 'Completar rentas',        'Permite marcar rentas como completadas',                  'Rentas'),
('RENTAL_CANCEL',   'Cancelar rentas',         'Permite cancelar rentas activas',                        'Rentas'),
-- Contratos
('CONTRACT_VIEW',     'Ver contratos',       'Permite ver contratos',                        'Contratos'),
('CONTRACT_CREATE',   'Crear contratos',     'Permite generar contratos',                    'Contratos'),
('CONTRACT_EDIT',     'Editar contratos',    'Permite modificar contratos',                  'Contratos'),
('CONTRACT_DELETE',   'Eliminar contratos',  'Permite eliminar contratos',                   'Contratos'),
('CONTRACT_SIGN',     'Firmar contratos',    'Permite gestionar firmas de contratos',        'Contratos'),
('CONTRACT_DOWNLOAD', 'Descargar contratos', 'Permite descargar PDFs de contratos',          'Contratos'),
-- Pagos
('PAYMENT_VIEW',    'Ver pagos',         'Permite ver el historial de pagos',               'Pagos'),
('PAYMENT_CREATE',  'Registrar pagos',   'Permite registrar nuevos pagos',                  'Pagos'),
('PAYMENT_EDIT',    'Editar pagos',      'Permite modificar información de pagos',          'Pagos'),
('PAYMENT_DELETE',  'Eliminar pagos',    'Permite eliminar pagos del sistema',              'Pagos'),
('PAYMENT_REFUND',  'Reembolsar pagos',  'Permite procesar reembolsos',                    'Pagos'),
-- Catálogos
('CATALOG_VIEW',    'Ver catálogos',       'Permite ver catálogos del sistema',                   'Catálogos'),
('CATALOG_MANAGE',  'Gestionar catálogos', 'Permite crear, editar y eliminar ítems de catálogos', 'Catálogos'),
-- Calendario
('CALENDAR_VIEW',   'Ver calendario',        'Permite ver el calendario de rentas',      'Calendario'),
('CALENDAR_SYNC',   'Sincronizar calendario','Permite vincular con Google Calendar',      'Calendario'),
-- Reportes
('REPORT_VIEW',     'Ver reportes',      'Permite ver reportes del sistema',             'Reportes'),
('REPORT_EXPORT',   'Exportar reportes', 'Permite exportar reportes a PDF/Excel',       'Reportes'),
-- Usuarios
('USER_VIEW',       'Ver usuarios',    'Permite ver el listado de usuarios',             'Usuarios'),
('USER_CREATE',     'Crear usuarios',  'Permite crear nuevos usuarios',                  'Usuarios'),
('USER_EDIT',       'Editar usuarios', 'Permite modificar información de usuarios',      'Usuarios'),
('USER_DELETE',     'Eliminar usuarios','Permite eliminar usuarios del sistema',         'Usuarios'),
('USER_ROLES',      'Gestionar roles', 'Permite asignar roles a usuarios',               'Usuarios'),
-- Configuración
('SETTINGS_VIEW',   'Ver configuración',    'Permite ver la configuración del sistema',       'Configuración'),
('SETTINGS_EDIT',   'Editar configuración', 'Permite modificar la configuración del sistema', 'Configuración'),
-- Seguridad
('SECURITY_ACCESS_LOG',  'Ver registro de acceso', 'Permite ver el log de accesos al sistema',    'Seguridad'),
('SECURITY_ROLES',       'Gestionar roles',         'Permite crear y modificar roles del sistema', 'Seguridad'),
('SECURITY_PERMISSIONS', 'Gestionar permisos',      'Permite asignar permisos a roles',            'Seguridad');

-- 3. Agregar columna permission_id si no existe
ALTER TABLE role_permissions ADD COLUMN IF NOT EXISTS permission_id BIGINT NULL;

-- 4. Poblar permission_id donde falte (SOLO si la columna 'permission' aún existe)
SET @old_col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME   = 'role_permissions'
      AND COLUMN_NAME  = 'permission'
);
SET @sql_update = IF(@old_col_exists > 0,
    'UPDATE role_permissions rp INNER JOIN permission p ON p.name = rp.permission SET rp.permission_id = p.id WHERE rp.permission_id IS NULL',
    'SELECT 1 /* columna permission ya eliminada, nada que poblar */'
);
PREPARE _stmt_upd FROM @sql_update;
EXECUTE _stmt_upd;
DEALLOCATE PREPARE _stmt_upd;

-- 5. Eliminar PRIMARY KEY SOLO si existe actualmente
SET @pk_exists = (
    SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME   = 'role_permissions'
      AND CONSTRAINT_TYPE = 'PRIMARY KEY'
);
SET @sql_drop_pk = IF(@pk_exists > 0,
    'ALTER TABLE role_permissions DROP PRIMARY KEY',
    'SELECT 1 /* PRIMARY KEY ya eliminado */'
);
PREPARE _stmt FROM @sql_drop_pk;
EXECUTE _stmt;
DEALLOCATE PREPARE _stmt;

-- 6. Hacer permission_id NOT NULL
ALTER TABLE role_permissions MODIFY COLUMN permission_id BIGINT NOT NULL;

-- 7. Agregar nuevo PK (role_id, permission_id) SOLO si no existe
SET @new_pk_exists = (
    SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME   = 'role_permissions'
      AND CONSTRAINT_TYPE = 'PRIMARY KEY'
);
SET @sql_add_pk = IF(@new_pk_exists = 0,
    'ALTER TABLE role_permissions ADD PRIMARY KEY (role_id, permission_id)',
    'SELECT 1 /* PRIMARY KEY ya creado */'
);
PREPARE _stmt2 FROM @sql_add_pk;
EXECUTE _stmt2;
DEALLOCATE PREPARE _stmt2;

-- 8. Eliminar columna antigua permission (IF EXISTS para idempotencia)
ALTER TABLE role_permissions DROP COLUMN IF EXISTS `permission`;

-- 9. Agregar FK permission_id → permission(id) (IF NOT EXISTS para idempotencia)
ALTER TABLE role_permissions
    ADD CONSTRAINT IF NOT EXISTS fk_rp_permission
    FOREIGN KEY (permission_id) REFERENCES permission (id) ON DELETE CASCADE;
