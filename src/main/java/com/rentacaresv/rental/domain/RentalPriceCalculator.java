package com.rentacaresv.rental.domain;

import com.rentacaresv.customer.domain.Customer;
import com.rentacaresv.vehicle.domain.Vehicle;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Domain Service para cálculo de precios de renta
 * 
 * Este es un ejemplo de Domain Service en DDD:
 * - No es una entidad
 * - Contiene lógica que involucra múltiples entidades (Vehicle + Customer)
 * - Es Java puro, sin dependencia de Spring
 */
public class RentalPriceCalculator {

    /**
     * Calcula el precio total de la renta según:
     * - Tipo de cliente (VIP o Normal)
     * - Duración de la renta (>15 días, >30 días)
     * - Precio del vehículo
     */
    public BigDecimal calculateTotalPrice(
            Vehicle vehicle,
            Customer customer,
            LocalDate startDate,
            LocalDate endDate
    ) {
        int days = calculateDays(startDate, endDate);
        BigDecimal dailyRate = selectDailyRate(vehicle, customer, days);
        
        return dailyRate.multiply(BigDecimal.valueOf(days));
    }

    /**
     * Calcula la tarifa diaria aplicable
     */
    public BigDecimal selectDailyRate(Vehicle vehicle, Customer customer, int days) {
        // Cliente VIP siempre tiene precio especial
        if (customer.isVip()) {
            return vehicle.getPriceVip();
        }
        
        // Cliente normal según duración
        if (days >= 30) {
            return vehicle.getPriceMonthly();
        } else if (days >= 15) {
            return vehicle.getPriceMoreThan15Days();
        } else {
            return vehicle.getPriceNormal();
        }
    }

    /**
     * Calcula los días de renta
     */
    public int calculateDays(LocalDate startDate, LocalDate endDate) {
        long days = ChronoUnit.DAYS.between(startDate, endDate);
        return (int) days; // No incluye el día de entrega, solo días completos
    }
}
