package com.rentacaresv.payment.application;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Command para registrar un nuevo pago
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterPaymentCommand {

    @NotNull(message = "La renta es obligatoria")
    private Long rentalId;

    @NotNull(message = "El monto es obligatorio")
    @DecimalMin(value = "0.01", message = "El monto debe ser mayor a 0")
    private BigDecimal amount;

    @NotBlank(message = "El método de pago es obligatorio")
    private String paymentMethod;

    private String referenceNumber;
    private String cardLastDigits;
    private String notes;
}
