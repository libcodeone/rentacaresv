package com.rentacaresv.rental.application;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Command para editar una renta existente (solo en estado PENDING).
 * Permite cambiar fechas, cliente, itinerario y datos de salida del país.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateRentalCommand {

    @NotNull
    private Long rentalId;

    @NotNull(message = "El cliente es obligatorio")
    private Long customerId;

    @NotNull(message = "La fecha de inicio es obligatoria")
    @FutureOrPresent(message = "La fecha de inicio no puede ser en el pasado")
    private LocalDate startDate;

    @NotNull(message = "La fecha de fin es obligatoria")
    @Future(message = "La fecha de fin debe ser futura")
    private LocalDate endDate;

    private String notes;

    // Información de viaje (opcional)
    @Size(max = 20)
    private String flightNumber;

    @Size(max = 2000)
    private String travelItinerary;

    @Size(max = 200)
    private String accommodation;

    @Size(max = 20)
    private String contactPhone;

    // Salida del país (opcional)
    private boolean sacarPais = false;

    @Size(max = 300)
    private String destinosFueraPais;

    @Min(value = 0)
    private Integer diasFueraPais;

    public void validate() {
        if (endDate != null && startDate != null) {
            if (!endDate.isAfter(startDate)) {
                throw new IllegalArgumentException("La fecha de fin debe ser posterior a la fecha de inicio");
            }
        }
    }
}
