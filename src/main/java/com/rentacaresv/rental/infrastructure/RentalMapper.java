package com.rentacaresv.rental.infrastructure;

import com.rentacaresv.rental.application.RentalDTO;
import com.rentacaresv.rental.domain.Rental;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper para convertir entre Rental y RentalDTO
 * (Infrastructure Layer)
 */
@Component
public class RentalMapper {

    /**
     * Convierte una entidad Rental a DTO
     */
    public RentalDTO toDTO(Rental rental) {
        if (rental == null) {
            return null;
        }

        return RentalDTO.builder()
                .id(rental.getId())
                .contractNumber(rental.getContractNumber())
                // Vehículo
                .vehicleId(rental.getVehicle().getId())
                .vehicleLicensePlate(rental.getVehicle().getLicensePlate())
                .vehicleDescription(rental.getVehicle().getFullDescription())
                // Cliente
                .customerId(rental.getCustomer().getId())
                .customerName(rental.getCustomer().getFullName())
                .customerDocument(rental.getCustomer().getDocumentNumber())
                .customerIsVip(rental.getCustomer().isVip())
                // Fechas
                .startDate(rental.getStartDate())
                .endDate(rental.getEndDate())
                .actualDeliveryDate(rental.getActualDeliveryDate())
                .actualReturnDate(rental.getActualReturnDate())
                // Montos
                .dailyRate(rental.getDailyRate())
                .totalDays(rental.getTotalDays())
                .totalAmount(rental.getTotalAmount())
                .amountPaid(rental.getAmountPaid())
                .balance(rental.getBalance())
                // Estado
                .status(rental.getStatus().name())
                .statusLabel(rental.getStatus().getLabel())
                .notes(rental.getNotes())
                // Información de viaje
                .flightNumber(rental.getFlightNumber())
                .travelItinerary(rental.getTravelItinerary())
                .accommodation(rental.getAccommodation())
                .contactPhone(rental.getContactPhone())
                // Campos calculados
                .actualDays(rental.getActualDays())
                .isDelayed(rental.isDelayed())
                .delayDays(rental.getDelayDays())
                .delayPenalty(rental.calculateDelayPenalty())
                .isFullyPaid(rental.isFullyPaid())
                .isActive(rental.isActive())
                .canBeModified(rental.canBeModified())
                .isTouristRental(rental.isTouristRental())
                .build();
    }

    /**
     * Convierte una lista de Rental a lista de DTO
     */
    public List<RentalDTO> toDTOList(List<Rental> rentals) {
        if (rentals == null) {
            return List.of();
        }
        return rentals.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
}
