package com.rentacaresv.rental.application;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO para reservas públicas desde la web.
 * Contiene todos los datos que el cliente llena en el formulario multi-step.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicReservationDTO {

    // ========================================
    // Paso 1: Datos personales
    // ========================================

    @NotBlank(message = "El nombre completo es obligatorio")
    @Size(max = 200)
    private String fullName;

    @NotBlank(message = "El tipo de documento es obligatorio")
    private String documentType; // DUI, PASSPORT

    @NotBlank(message = "El número de documento es obligatorio")
    @Size(max = 50)
    private String documentNumber;

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "Email inválido")
    @Size(max = 100)
    private String email;

    @NotBlank(message = "El teléfono es obligatorio")
    @Size(max = 20)
    private String phone;

    // ========================================
    // Paso 2: Documento de identidad
    // Los archivos se reciben como multipart en el submit final
    // ========================================

    // ========================================
    // Paso 3: Licencia de conducir
    // ========================================

    @NotBlank(message = "El número de licencia es obligatorio")
    @Size(max = 50)
    private String driverLicenseNumber;

    @Size(max = 3)
    private String driverLicenseCountry; // ISO 3166-1 alpha-3

    @NotNull(message = "La fecha de vencimiento de la licencia es obligatoria")
    private LocalDate driverLicenseExpiry;

    // ========================================
    // Paso 4: Datos del viaje
    // ========================================

    @NotNull(message = "El vehículo es obligatorio")
    private Long vehicleId;

    @NotNull(message = "La fecha de inicio es obligatoria")
    private LocalDate startDate;

    @NotNull(message = "La fecha de fin es obligatoria")
    private LocalDate endDate;

    @Size(max = 100)
    private String deliveryLocation; // Opcional

    @Size(max = 20)
    private String flightNumber; // Opcional

    @Size(max = 200)
    private String accommodation; // Opcional

    @Size(max = 20)
    private String contactPhone;

    @Size(max = 20)
    private String phoneFamily; // Opcional - emergencia

    // Conductor adicional (todo opcional)
    @Size(max = 200)
    private String additionalDriverName;

    @Size(max = 50)
    private String additionalDriverLicense;

    @Size(max = 20)
    private String additionalDriverDui;

    // ========================================
    // Salida del país (opcional)
    // ========================================

    /** El cliente planea sacar el vehículo fuera del país */
    private boolean sacarPais = false;

    /** Países destino separados por coma (ej: "Guatemala,Honduras") */
    @Size(max = 300)
    private String destinosFueraPais;

    /** Cantidad de días que el vehículo estará fuera del país */
    private int diasFueraPais = 0;

    // ========================================
    // Honeypot (anti-bot)
    // ========================================

    /**
     * Campo oculto. Si tiene valor, es un bot.
     */
    private String website;
}
