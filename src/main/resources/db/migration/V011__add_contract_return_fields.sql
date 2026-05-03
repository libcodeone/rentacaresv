-- V011: Agrega campos de devolución al contrato
-- vehicle_returned_ok: el operador confirma que el vehículo fue devuelto en buen estado
-- vehicle_returned_at: fecha/hora real de confirmación de la devolución

ALTER TABLE contract
    ADD COLUMN vehicle_returned_ok  TINYINT(1)  NULL DEFAULT NULL,
    ADD COLUMN vehicle_returned_at  DATETIME    NULL DEFAULT NULL;
