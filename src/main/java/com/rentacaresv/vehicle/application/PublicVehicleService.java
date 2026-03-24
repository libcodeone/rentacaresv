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
import java.util.List;

/**
 * Servicio para la API pública de vehículos.
 * Solo expone vehículos no eliminados con información pública e información de disponibilidad.
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PublicVehicleService {

    private final VehicleRepository vehicleRepository;
    private final VehiclePhotoRepository vehiclePhotoRepository;
    private final RentalRepository rentalRepository;

    /**
     * Obtiene todos los vehículos activos (no eliminados) con su disponibilidad.
     */
    public List<PublicVehicleDTO> findAllVehicles() {
        List<Vehicle> vehicles = vehicleRepository.findAllActive();
        return vehicles.stream().map(this::toPublicDTO).toList();
    }

    /**
     * Obtiene un vehículo por ID (para la página de detalle).
     */
    public PublicVehicleDTO findById(Long id) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .filter(v -> v.getDeletedAt() == null)
                .orElseThrow(() -> new IllegalArgumentException("Vehículo no encontrado"));
        return toPublicDTO(vehicle);
    }

    /**
     * Obtiene los primeros N vehículos disponibles (para el hero/landing).
     */
    public List<PublicVehicleDTO> findFeaturedVehicles(int limit) {
        List<Vehicle> vehicles = vehicleRepository.findAvailableVehicles();
        return vehicles.stream()
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

        // Disponibilidad - rentas activas o pendientes
        List<Rental> activeRentals = rentalRepository.findActiveRentalsByVehicleId(vehicle.getId());
        LocalDate today = LocalDate.now();

        // Verificar si hay una renta que cubra hoy
        boolean availableNow = vehicle.isAvailable() && activeRentals.stream()
                .noneMatch(r -> !r.getStartDate().isAfter(today) && !r.getEndDate().isBefore(today));

        // Si no está disponible ahora, calcular cuándo estará
        LocalDate availableFrom = null;
        if (!availableNow && !activeRentals.isEmpty()) {
            // Buscar la renta actual (que cubra hoy) y su fecha de fin
            availableFrom = activeRentals.stream()
                    .filter(r -> !r.getStartDate().isAfter(today) && !r.getEndDate().isBefore(today))
                    .map(r -> r.getEndDate().plusDays(1))
                    .findFirst()
                    .orElse(null);

            // Si el vehículo está en mantenimiento o fuera de servicio sin renta activa hoy,
            // no podemos saber cuándo estará disponible
        }

        // Períodos reservados (solo rentas vigentes o futuras, excluir las ya pasadas)
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
