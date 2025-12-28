package com.rentacaresv.rental.application;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Command para crear una nueva renta
 * Contiene validaciones de entrada
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateRentalCommand {

    @NotNull(message = "El vehículo es obligatorio")
    private Long vehicleId;

    @NotNull(message = "El cliente es obligatorio")
    private Long customerId;

    @NotNull(message = "La fecha de inicio es obligatoria")
    @Future(message = "La fecha de inicio debe ser futura")
    private LocalDate startDate;

    @NotNull(message = "La fecha de fin es obligatoria")
    @Future(message = "La fecha de fin debe ser futura")
    private LocalDate endDate;

    private String notes;
    
    // ========================================
    // Información de viaje (opcional)
    // ========================================
    
    @Size(max = 20, message = "El número de vuelo no puede tener más de 20 caracteres")
    private String flightNumber;
    
    @Size(max = 2000, message = "El itinerario no puede tener más de 2000 caracteres")
    private String travelItinerary;
    
    @Size(max = 200, message = "El hospedaje no puede tener más de 200 caracteres")
    private String accommodation;
    
    @Size(max = 20, message = "El teléfono no puede tener más de 20 caracteres")
    private String contactPhone;

    /**
     * Valida que la fecha de fin sea después de la fecha de inicio
     */
    public void validate() {
        if (endDate != null && startDate != null) {
            if (endDate.isBefore(startDate)) {
                throw new IllegalArgumentException(
                    "La fecha de fin debe ser posterior a la fecha de inicio"
                );
            }
            if (endDate.equals(startDate)) {
                throw new IllegalArgumentException(
                    "La renta debe ser de al menos 1 día"
                );
            }
        }
    }
}
