-- V013: Refactoriza permisos de columna ENUM a tabla relacional con FKs
-- Crea tabla permission con filas reales y convierte role_permissions a join table.

-- 1. Crear tabla permission
CREATE TABLE permission (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    name        VARCHAR(100) NOT NULL,
    display_name VARCHAR(200) NOT NULL,
    description VARCHAR(500),
    category    VARCHAR(100) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_permission_name (name)
);

-- 2. Insertar todos los permisos del sistema
INSERT INTO permission (name, display_name, description, category) VALUES
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
('CONTRACT_VIEW',     'Ver contratos',         'Permite ver contratos',                          'Contratos'),
('CONTRACT_CREATE',   'Crear contratos',       'Permite generar contratos',                      'Contratos'),
('CONTRACT_EDIT',     'Editar contratos',      'Permite modificar contratos',                    'Contratos'),
('CONTRACT_DELETE',   'Eliminar contratos',    'Permite eliminar contratos',                     'Contratos'),
('CONTRACT_SIGN',     'Firmar contratos',      'Permite gestionar firmas de contratos',          'Contratos'),
('CONTRACT_DOWNLOAD', 'Descargar contratos',   'Permite descargar PDFs de contratos',            'Contratos'),
-- Pagos
('PAYMENT_VIEW',    'Ver pagos',          'Permite ver el historial de pagos',               'Pagos'),
('PAYMENT_CREATE',  'Registrar pagos',    'Permite registrar nuevos pagos',                  'Pagos'),
('PAYMENT_EDIT',    'Editar pagos',       'Permite modificar información de pagos',          'Pagos'),
('PAYMENT_DELETE',  'Eliminar pagos',     'Permite eliminar pagos del sistema',              'Pagos'),
('PAYMENT_REFUND',  'Reembolsar pagos',   'Permite procesar reembolsos',                    'Pagos'),
-- Catálogos
('CATALOG_VIEW',    'Ver catálogos',      'Permite ver catálogos del sistema',               'Catálogos'),
('CATALOG_MANAGE',  'Gestionar catálogos','Permite crear, editar y eliminar ítems de catálogos', 'Catálogos'),
-- Calendario
('CALENDAR_VIEW',   'Ver calendario',     'Permite ver el calendario de rentas',             'Calendario'),
('CALENDAR_SYNC',   'Sincronizar calendario','Permite vincular con Google Calendar',         'Calendario'),
-- Reportes
('REPORT_VIEW',     'Ver reportes',       'Permite ver reportes del sistema',                'Reportes'),
('REPORT_EXPORT',   'Exportar reportes',  'Permite exportar reportes a PDF/Excel',          'Reportes'),
-- Usuarios
('USER_VIEW',       'Ver usuarios',       'Permite ver el listado de usuarios',              'Usuarios'),
('USER_CREATE',     'Crear usuarios',     'Permite crear nuevos usuarios',                   'Usuarios'),
('USER_EDIT',       'Editar usuarios',    'Permite modificar información de usuarios',       'Usuarios'),
('USER_DELETE',     'Eliminar usuarios',  'Permite eliminar usuarios del sistema',           'Usuarios'),
('USER_ROLES',      'Gestionar roles',    'Permite asignar roles a usuarios',                'Usuarios'),
-- Configuración
('SETTINGS_VIEW',   'Ver configuración',    'Permite ver la configuración del sistema',       'Configuración'),
('SETTINGS_EDIT',   'Editar configuración', 'Permite modificar la configuración del sistema', 'Configuración'),
-- Seguridad
('SECURITY_ACCESS_LOG',  'Ver registro de acceso', 'Permite ver el log de accesos al sistema',       'Seguridad'),
('SECURITY_ROLES',       'Gestionar roles',         'Permite crear y modificar roles del sistema',    'Seguridad'),
('SECURITY_PERMISSIONS', 'Gestionar permisos',      'Permite asignar permisos a roles',               'Seguridad');

-- 3. Crear nueva tabla join con FKs reales
CREATE TABLE role_permissions_new (
    role_id       BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_rp_role       FOREIGN KEY (role_id)       REFERENCES system_role(id) ON DELETE CASCADE,
    CONSTRAINT fk_rp_permission FOREIGN KEY (permission_id) REFERENCES permission(id)  ON DELETE CASCADE
);

-- 4. Migrar datos existentes (rp.permission es ENUM o VARCHAR con el nombre del permiso)
INSERT INTO role_permissions_new (role_id, permission_id)
SELECT rp.role_id, p.id
FROM role_permissions rp
INNER JOIN permission p ON p.name = rp.permission;

-- 5. Reemplazar tabla antigua
DROP TABLE role_permissions;
RENAME TABLE role_permissions_new TO role_permissions;
