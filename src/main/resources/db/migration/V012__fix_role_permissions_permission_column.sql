-- V012: Convierte role_permissions.permission de ENUM a VARCHAR(50)
-- El ENUM anterior no incluía RENTAL_DELIVER ni permite agregar nuevos permisos sin migración.
-- VARCHAR(50) es más flexible y acepta cualquier valor del enum Permission.java.

ALTER TABLE role_permissions
    MODIFY COLUMN permission VARCHAR(50) NOT NULL;
