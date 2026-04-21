-- V009: Tabla de eventos de analíticas web
-- Autor: Sistema | Fecha: 2026-04-15

CREATE TABLE analytics_event (
    id              BIGINT          AUTO_INCREMENT PRIMARY KEY,
    event_type      VARCHAR(50)     NOT NULL,
    page_path       VARCHAR(300)    NULL,
    vehicle_id      BIGINT          NULL,
    vehicle_name    VARCHAR(200)    NULL,
    step_number     INT             NULL,
    step_name       VARCHAR(100)    NULL,
    source          VARCHAR(50)     NULL,
    contract_number VARCHAR(50)     NULL,
    total_amount    DECIMAL(10, 2)  NULL,
    session_id      VARCHAR(100)    NULL,
    ip              VARCHAR(50)     NULL,
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_ae_event_type  (event_type),
    INDEX idx_ae_created_at  (created_at),
    INDEX idx_ae_session_id  (session_id),
    INDEX idx_ae_vehicle_id  (vehicle_id)
);
