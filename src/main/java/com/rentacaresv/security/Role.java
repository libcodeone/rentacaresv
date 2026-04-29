package com.rentacaresv.security;

/**
 * Roles de usuario en el sistema RentaCar ESV
 *
 * - ADMIN:    Acceso completo al sistema
 * - OPERATOR: Operador con acceso a todas las operaciones diarias
 * - AGENT:    Agente de entrega — solo ve rentas, gestiona contratos y entrega vehículos
 */
public enum Role {
    ADMIN,
    OPERATOR,
    AGENT
}
