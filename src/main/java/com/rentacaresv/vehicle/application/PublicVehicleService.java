package com.rentacaresv.vehicle.application;

import com.rentacaresv.rental.domain.Rental;
import com.rentacaresv.rental.infrastructure.RentalRepository;
import com.rentacaresv.vehicle.domain.Vehicle;
import com.rentacaresv.vehicle.domain.photo.VehiclePhoto;
import com.rentacaresv.vehicle.infrastructure.VehiclePhotoRepository;
import com.rentacaresv.vehicle.infrastructure.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

/**
 * Servicio para la API pública de vehículos.
 * Solo expone vehículos publicados (publishedOnWeb = true) con información pública.
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PublicVehicleService {

    private final VehicleRepository vehicleRepository;
    private final VehiclePhotoRepository vehiclePhotoRepository;
    private final RentalRepository rentalRepository;

    /**
     * Obtiene todos los vehículos publicados con su disponibilidad.
     * Soporta filtro por rango de años y ordenamiento.
     *
     * @param yearFrom  Año mínimo (null = sin límite)
     * @param yearTo    Año máximo (null = sin límite)
     * @param sortBy    Campo de ordenamiento: price_asc, price_desc, year_desc, year_asc, brand_asc
     */
    public List<PublicVehicleDTO> findAllVehicles(Integer yearFrom, Integer yearTo, String sortBy) {
        List<Vehicle> vehicles = vehicleRepository.findPublishedOnWeb();

        // Filtro por rango de años
        if (yearFrom != null) {
            vehicles = vehicles.stream().filter(v -> v.getYear() >= yearFrom).toList();
        }
        if (yearTo != null) {
            vehicles = vehicles.stream().filter(v -> v.getYear() <= yearTo).toList();
        }

        // Mapear a DTO
        List<PublicVehicleDTO> dtos = vehicles.stream().map(this::toPublicDTO).toList();

        // Ordenamiento
        if (sortBy != null) {
            Comparator<PublicVehicleDTO> comparator = switch (sortBy) {
                case "price_asc" -> Comparator.comparing(PublicVehicleDTO::getPricePerDay);
                case "price_desc" -> Comparator.comparing(PublicVehicleDTO::getPricePerDay).reversed();
                case "year_asc" -> Comparator.comparing(PublicVehicleDTO::getYear);
                case "year_desc" -> Comparator.comparing(PublicVehicleDTO::getYear).reversed();
                case "brand_asc" -> Comparator.comparing(PublicVehicleDTO::getBrand);
                default -> null;
            };
            if (comparator != null) {
                dtos = dtos.stream().sorted(comparator).toList();
            }
        }

        return dtos;
    }

    /**
     * Obtiene un vehículo publicado por ID.
     */
    public PublicVehicleDTO findById(Long id) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .filter(v -> v.getDeletedAt() == null && Boolean.TRUE.equals(v.getPublishedOnWeb()))
                .orElseThrow(() -> new IllegalArgumentException("Vehículo no encontrado"));
        return toPublicDTO(vehicle);
    }

    /**
     * Obtiene los primeros N vehículos disponibles y publicados.
     */
    public List<PublicVehicleDTO> findFeaturedVehicles(int limit) {
        List<Vehicle> vehicles = vehicleRepository.findPublishedOnWeb();
        return vehicles.stream()
                .filter(Vehicle::isAvailable)
                .limit(limit)
                .map(this::toPublicDTO)
                .toList();
    }

    private PublicVehicleDTO toPublicDTO(Vehicle vehicle) {
        // Fotos
        List<VehiclePhoto> photos = vehiclePhotoRepository.findByVehicleId(vehicle.getId());

        String primaryUrl = photos.stream()
                .filter(p -> Boolean.TRUE.equals(p.getIsPrimary()))
                .findFirst()
                .map(VehiclePhoto::getPhotoUrl)
                .orElse(photos.isEmpty() ? null : photos.get(0).getPhotoUrl());

        List<PublicVehicleDTO.VehiclePhotoDTO> photoDTOs = photos.stream()
                .map(p -> PublicVehicleDTO.VehiclePhotoDTO.builder()
                        .id(p.getId())
                        .photoUrl(p.getPhotoUrl())
                        .photoType(p.getPhotoType().name())
                        .isPrimary(p.getIsPrimary())
                        .displayOrder(p.getDisplayOrder())
                        .build())
                .toList();

        // Disponibilidad
        List<Rental> activeRentals = rentalRepository.findActiveRentalsByVehicleId(vehicle.getId());
        LocalDate today = LocalDate.now();

        boolean availableNow = vehicle.isAvailable() && activeRentals.stream()
                .noneMatch(r -> !r.getStartDate().isAfter(today) && !r.getEndDate().isBefore(today));

        LocalDate availableFrom = null;
        if (!availableNow && !activeRentals.isEmpty()) {
            availableFrom = activeRentals.stream()
                    .filter(r -> !r.getStartDate().isAfter(today) && !r.getEndDate().isBefore(today))
                    .map(r -> r.getEndDate().plusDays(1))
                    .findFirst()
                    .orElse(null);
        }

        List<PublicVehicleDTO.ReservedPeriodDTO> reservedPeriods = activeRentals.stream()
                .filter(r -> !r.getEndDate().isBefore(today))
                .map(r -> PublicVehicleDTO.ReservedPeriodDTO.builder()
                        .startDate(r.getStartDate())
                        .endDate(r.getEndDate())
                        .build())
                .toList();

        return PublicVehicleDTO.builder()
                .id(vehicle.getId())
                .brand(vehicle.getBrand())
                .model(vehicle.getModel())
                .year(vehicle.getYear())
                .color(vehicle.getColor())
                .transmissionType(vehicle.getTransmissionType().name())
                .fuelType(vehicle.getFuelType().name())
                .vehicleType(vehicle.getVehicleType() != null ? vehicle.getVehicleType().name() : null)
                .vehicleTypeLabel(vehicle.getVehicleType() != null ? vehicle.getVehicleType().getLabel() : null)
                .passengerCapacity(vehicle.getPassengerCapacity())
                .pricePerDay(vehicle.getPriceNormal())
                .primaryPhotoUrl(primaryUrl)
                .photos(photoDTOs)
                .fullDescription(vehicle.getFullDescription())
                .availableNow(availableNow)
                .availableFrom(availableFrom)
                .reservedPeriods(reservedPeriods)
                .build();
    }
}
