package com.rentacaresv.calendar.application;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO para representar eventos del calendario
 * Mapea rentas a eventos visuales en el calendario
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CalendarEventDTO {

    private Long rentalId;
    private String title;
    private LocalDate start;
    private LocalDate end;
    private String color;
    private String status;

    // Información adicional para el tooltip/detalles
    private String customerName;
    private String vehicleInfo;
    private String contractNumber;
}
