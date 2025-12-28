package com.rentacaresv.payment.application;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO para transferir datos de Payment
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentDTO {
    
    private Long id;
    private String paymentNumber;
    
    // Información de la renta
    private Long rentalId;
    private String rentalContractNumber;
    
    // Información del pago
    private BigDecimal amount;
    private String paymentMethod;
    private String paymentMethodLabel;
    private String status;
    private String statusLabel;
    private LocalDateTime paymentDate;
    
    // Datos adicionales
    private String referenceNumber;
    private String cardLastDigits;
    private String notes;
    
    // Auditoría
    private String createdBy;
    private LocalDateTime createdAt;
    
    // Campos calculados
    private Boolean isSuccessful;
    private Boolean canBeRefunded;
    private String description;
}
