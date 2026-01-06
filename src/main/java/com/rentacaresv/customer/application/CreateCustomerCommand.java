package com.rentacaresv.customer.application;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Command para crear un nuevo cliente
 * Contiene validaciones de entrada
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCustomerCommand {

    @NotBlank(message = "El nombre completo es obligatorio")
    @Size(max = 200, message = "El nombre no puede tener más de 200 caracteres")
    private String fullName;

    @NotBlank(message = "El tipo de documento es obligatorio")
    private String documentType;

    @NotBlank(message = "El número de documento es obligatorio")
    @Size(max = 50, message = "El número de documento no puede tener más de 50 caracteres")
    private String documentNumber;

    @Email(message = "El email debe ser válido")
    @Size(max = 100, message = "El email no puede tener más de 100 caracteres")
    private String email;

    @Size(max = 20, message = "El teléfono no puede tener más de 20 caracteres")
    private String phone;

    private String address;

    @Past(message = "La fecha de nacimiento debe ser en el pasado")
    private LocalDate birthDate;

    // Licencia de conducir
    @NotBlank(message = "El número de licencia es obligatorio")
    @Size(max = 50, message = "El número de licencia no puede tener más de 50 caracteres")
    private String driverLicenseNumber;

    @Size(max = 3, message = "El código de país debe tener 3 caracteres")
    private String driverLicenseCountry;

    private LocalDate driverLicenseExpiry;

    @NotBlank(message = "La categoría es obligatoria")
    private String category;

    private String notes;
}
