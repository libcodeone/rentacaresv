package com.rentacaresv.calendar.application;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO para eventos obtenidos de Google Calendar
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoogleCalendarEventDTO {
    
    private String id;
    private String title;
    private String description;
    private LocalDate startDate;
    private LocalDate endDate;
    private boolean allDay;
    private String color;
    private String htmlLink;
}
