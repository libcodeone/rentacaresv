package com.rentacaresv.vehicle.infrastructure;

import com.rentacaresv.vehicle.application.VehicleDTO;
import com.rentacaresv.vehicle.domain.Vehicle;
import com.rentacaresv.vehicle.domain.VehicleType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper para convertir entre Vehicle y VehicleDTO
 * (Infrastructure Layer)
 */
@Component
public class VehicleMapper {

    /**
     * Convierte una entidad Vehicle a DTO
     */
    public VehicleDTO toDTO(Vehicle vehicle) {
        if (vehicle == null) {
            return null;
        }

        return VehicleDTO.builder()
                .id(vehicle.getId())
                .licensePlate(vehicle.getLicensePlate())
                .brand(vehicle.getBrand())
                .model(vehicle.getModel())
                .year(vehicle.getYear())
                .color(vehicle.getColor())
                .transmissionType(vehicle.getTransmissionType().name())
                .fuelType(vehicle.getFuelType().name())
                .vehicleType(vehicle.getVehicleType() != null ? vehicle.getVehicleType().name() : null)
                .vehicleTypeLabel(vehicle.getVehicleType() != null ? vehicle.getVehicleType().getLabel() : null)
                .passengerCapacity(vehicle.getPassengerCapacity())
                .mileage(vehicle.getMileage())
                .priceNormal(vehicle.getPriceNormal())
                .priceVip(vehicle.getPriceVip())
                .priceMoreThan15Days(vehicle.getPriceMoreThan15Days())
                .priceMonthly(vehicle.getPriceMonthly())
                .status(vehicle.getStatus().name())
                .notes(vehicle.getNotes())
                .fullDescription(vehicle.getFullDescription())
                .isAvailable(vehicle.isAvailable())
                .build();
    }

    /**
     * Convierte una lista de Vehicle a lista de DTO
     */
    public List<VehicleDTO> toDTOList(List<Vehicle> vehicles) {
        if (vehicles == null) {
            return List.of();
        }
        return vehicles.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
}
