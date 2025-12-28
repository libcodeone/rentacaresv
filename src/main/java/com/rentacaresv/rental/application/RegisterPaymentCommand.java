package com.rentacaresv.rental.application;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Command para registrar un pago de una renta
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterPaymentCommand {

    @NotNull(message = "El ID de la renta es obligatorio")
    private Long rentalId;

    @NotNull(message = "El monto es obligatorio")
    @DecimalMin(value = "0.01", message = "El monto debe ser mayor a 0")
    private BigDecimal amount;

    private String paymentMethod;
    private String notes;
}
