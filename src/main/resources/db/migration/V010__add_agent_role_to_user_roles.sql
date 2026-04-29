-- V010: Agrega el valor AGENT al ENUM de la columna role en user_roles
-- Autor: Sistema | Fecha: 2026-04-29

ALTER TABLE user_roles
    MODIFY COLUMN role ENUM('ADMIN', 'OPERATOR', 'AGENT') NOT NULL;
