package com.rentacaresv.rental.application;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO para transferir datos de Rental
 * Se usa para enviar información de la renta a la capa de presentación
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RentalDTO {
    
    private Long id;
    private String contractNumber;
    
    // Vehículo
    private Long vehicleId;
    private String vehicleLicensePlate;
    private String vehicleDescription;  // Marca Modelo Año
    
    // Cliente
    private Long customerId;
    private String customerName;
    private String customerDocument;
    private Boolean customerIsVip;
    
    // Fechas
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDateTime actualDeliveryDate;
    private LocalDateTime actualReturnDate;
    
    // Montos
    private BigDecimal dailyRate;
    private Integer totalDays;
    private BigDecimal totalAmount;
    private BigDecimal amountPaid;
    private BigDecimal balance;
    
    // Estado
    private String status;
    private String statusLabel;
    private String notes;
    
    // Información de viaje (opcional - para clientes turistas)
    private String flightNumber;
    private String travelItinerary;
    private String accommodation;
    private String contactPhone;
    
    // Campos calculados
    private Integer actualDays;
    private Boolean isDelayed;
    private Integer delayDays;
    private BigDecimal delayPenalty;
    private Boolean isFullyPaid;
    private Boolean isActive;
    private Boolean canBeModified;
    private Boolean isTouristRental;
}
