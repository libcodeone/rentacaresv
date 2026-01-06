package com.rentacaresv.customer.infrastructure;

import com.rentacaresv.customer.application.CustomerDTO;
import com.rentacaresv.customer.domain.Customer;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper para convertir entre Customer y CustomerDTO
 * (Infrastructure Layer)
 */
@Component
public class CustomerMapper {

    /**
     * Convierte una entidad Customer a DTO
     */
    public CustomerDTO toDTO(Customer customer) {
        if (customer == null) {
            return null;
        }

        return CustomerDTO.builder()
                .id(customer.getId())
                .fullName(customer.getFullName())
                .documentType(customer.getDocumentType().name())
                .documentNumber(customer.getDocumentNumber())
                .email(customer.getEmail())
                .phone(customer.getPhone())
                .address(customer.getAddress())
                .birthDate(customer.getBirthDate())
                .driverLicenseNumber(customer.getDriverLicenseNumber())
                .driverLicenseCountry(customer.getDriverLicenseCountry())
                .driverLicenseExpiry(customer.getDriverLicenseExpiry())
                .category(customer.getCategory().name())
                .notes(customer.getNotes())
                .active(customer.getActive())
                .displayName(customer.getDisplayName())
                .age(customer.getAge())
                .isVip(customer.isVip())
                .isActiveCustomer(customer.isActiveCustomer())
                .hasDriverLicense(customer.hasDriverLicense())
                .canRentVehicle(customer.canRentVehicle())
                .cannotRentReason(customer.getCannotRentReason())
                .build();
    }

    /**
     * Convierte una lista de Customer a lista de DTO
     */
    public List<CustomerDTO> toDTOList(List<Customer> customers) {
        if (customers == null) {
            return List.of();
        }
        return customers.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
}
