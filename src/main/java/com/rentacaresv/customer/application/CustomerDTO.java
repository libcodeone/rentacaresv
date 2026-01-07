package com.rentacaresv.customer.application;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO para transferir datos de Customer
 * Se usa para enviar información del cliente a la capa de presentación
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerDTO {
    
    private Long id;
    private String fullName;
    private String documentType;
    private String documentNumber;
    private String email;
    private String phone;
    private String address;
    private LocalDate birthDate;
    
    // Licencia de conducir
    private String driverLicenseNumber;
    private String driverLicenseCountry;
    private LocalDate driverLicenseExpiry;
    
    private String category;
    private String notes;
    private Boolean active;
    
    // Campos calculados
    private String displayName;
    private Integer age;
    private Boolean isVip;
    private Boolean isActiveCustomer;
    private Boolean hasDriverLicense;
    private Boolean canRentVehicle;
    private String cannotRentReason;
}
