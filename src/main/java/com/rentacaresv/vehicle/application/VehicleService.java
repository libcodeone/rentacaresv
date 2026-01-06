package com.rentacaresv.vehicle.application;

import com.rentacaresv.vehicle.domain.FuelType;
import com.rentacaresv.vehicle.domain.TransmissionType;
import com.rentacaresv.vehicle.domain.Vehicle;
import com.rentacaresv.vehicle.domain.VehicleStatus;
import com.rentacaresv.vehicle.domain.VehicleType;
import com.rentacaresv.vehicle.infrastructure.VehicleMapper;
import com.rentacaresv.vehicle.infrastructure.VehicleRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;

/**
 * Servicio de aplicación para Vehicle
 * Orquesta los casos de uso relacionados con vehículos
 */
@Service
@Transactional
@Validated
@RequiredArgsConstructor
@Slf4j
public class VehicleService {

    private final VehicleRepository vehicleRepository;
    private final VehicleMapper vehicleMapper;

    /**
     * Crea un nuevo vehículo
     * 
     * @return ID del vehículo creado
     */
    public Long createVehicle(@Valid CreateVehicleCommand command) {
        log.info("Creando vehículo con placa: {}", command.getLicensePlate());

        // Validar que no exista la placa
        if (vehicleRepository.existsByLicensePlate(command.getLicensePlate())) {
            throw new IllegalArgumentException(
                    "Ya existe un vehículo con la placa: " + command.getLicensePlate());
        }

        // Crear entidad de dominio
        Vehicle vehicle = Vehicle.builder()
                .licensePlate(command.getLicensePlate().toUpperCase())
                .brand(command.getBrand())
                .model(command.getModel())
                .year(command.getYear())
                .color(command.getColor())
                .transmissionType(TransmissionType.valueOf(command.getTransmissionType().toUpperCase()))
                .fuelType(FuelType.valueOf(command.getFuelType().toUpperCase()))
                .vehicleType(command.getVehicleType() != null ? VehicleType.valueOf(command.getVehicleType()) : null)
                .passengerCapacity(command.getPassengerCapacity())
                .mileage(command.getMileage() != null ? command.getMileage() : 0)
                .priceNormal(command.getPriceNormal())
                .priceVip(command.getPriceVip())
                .priceMoreThan15Days(command.getPriceMoreThan15Days())
                .priceMonthly(command.getPriceMonthly())
                .status(VehicleStatus.AVAILABLE)
                .notes(command.getNotes())
                .build();

        // Persistir
        vehicle = vehicleRepository.save(vehicle);

        log.info("Vehículo creado exitosamente: {} con ID: {}", vehicle.getFullDescription(), vehicle.getId());

        return vehicle.getId();
    }

    /**
     * Actualiza un vehículo existente
     */
    public void updateVehicle(Long id, @Valid CreateVehicleCommand command) {
        log.info("Actualizando vehículo ID: {}", id);

        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Vehículo no encontrado"));

        // Validar si la placa cambió y si ya existe otra placa igual
        if (!vehicle.getLicensePlate().equalsIgnoreCase(command.getLicensePlate()) &&
                vehicleRepository.existsByLicensePlate(command.getLicensePlate())) {
            throw new IllegalArgumentException("Ya existe otro vehículo con la placa: " + command.getLicensePlate());
        }

        // Actualizar campos
        vehicle.setBrand(command.getBrand());
        vehicle.setModel(command.getModel());
        vehicle.setYear(command.getYear());
        vehicle.setColor(command.getColor());
        vehicle.setTransmissionType(TransmissionType.valueOf(command.getTransmissionType()));
        vehicle.setFuelType(FuelType.valueOf(command.getFuelType()));
        vehicle.setVehicleType(command.getVehicleType() != null ? VehicleType.valueOf(command.getVehicleType()) : null);
        vehicle.setPassengerCapacity(command.getPassengerCapacity());
        vehicle.setMileage(command.getMileage());
        vehicle.setPriceNormal(command.getPriceNormal());
        vehicle.setPriceVip(command.getPriceVip());
        vehicle.setPriceMoreThan15Days(command.getPriceMoreThan15Days());
        vehicle.setPriceMonthly(command.getPriceMonthly());
        vehicle.setNotes(command.getNotes());

        // El estado y otros campos de negocio no se cambian aquí directamente

        vehicleRepository.save(vehicle);
        log.info("Vehículo actualizado exitosamente: {}", id);
    }

    /**
     * Obtiene un vehículo por ID
     */
    @Transactional(readOnly = true)
    public VehicleDTO findById(Long id) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Vehículo no encontrado con ID: " + id));
        return vehicleMapper.toDTO(vehicle);
    }

    /**
     * Obtiene un vehículo por placa
     */
    @Transactional(readOnly = true)
    public VehicleDTO findByLicensePlate(String licensePlate) {
        Vehicle vehicle = vehicleRepository.findByLicensePlate(licensePlate)
                .orElseThrow(() -> new IllegalArgumentException("Vehículo no encontrado con placa: " + licensePlate));
        return vehicleMapper.toDTO(vehicle);
    }

    /**
     * Obtiene todos los vehículos activos
     */
    @Transactional(readOnly = true)
    public List<VehicleDTO> findAll() {
        List<Vehicle> vehicles = vehicleRepository.findAllActive();
        return vehicleMapper.toDTOList(vehicles);
    }

    /**
     * Obtiene vehículos disponibles para rentar
     */
    @Transactional(readOnly = true)
    public List<VehicleDTO> findAvailableVehicles() {
        List<Vehicle> vehicles = vehicleRepository.findAvailableVehicles();
        return vehicleMapper.toDTOList(vehicles);
    }

    /**
     * Obtiene vehículos por estado
     */
    @Transactional(readOnly = true)
    public List<VehicleDTO> findByStatus(VehicleStatus status) {
        List<Vehicle> vehicles = vehicleRepository.findByStatus(status);
        return vehicleMapper.toDTOList(vehicles);
    }

    /**
     * Marca un vehículo como rentado
     */
    public void markAsRented(Long vehicleId) {
        log.info("Marcando vehículo {} como rentado", vehicleId);
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new IllegalArgumentException("Vehículo no encontrado"));

        vehicle.markAsRented(); // Lógica de dominio
        vehicleRepository.save(vehicle);
    }

    /**
     * Marca un vehículo como disponible
     */
    public void markAsAvailable(Long vehicleId) {
        log.info("Marcando vehículo {} como disponible", vehicleId);
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new IllegalArgumentException("Vehículo no encontrado"));

        vehicle.markAsAvailable(); // Lógica de dominio
        vehicleRepository.save(vehicle);
    }

    /**
     * Envía un vehículo a mantenimiento
     */
    public void sendToMaintenance(Long vehicleId) {
        log.info("Enviando vehículo {} a mantenimiento", vehicleId);
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new IllegalArgumentException("Vehículo no encontrado"));

        vehicle.sendToMaintenance(); // Lógica de dominio
        vehicleRepository.save(vehicle);
    }

    /**
     * Actualiza el estado de un vehículo
     * No permite cambiar a RENTED (eso se hace automáticamente al crear una renta)
     */
    public void updateVehicleStatus(Long vehicleId, VehicleStatus newStatus) {
        log.info("Actualizando estado del vehículo {} a {}", vehicleId, newStatus);
        
        if (newStatus == VehicleStatus.RENTED) {
            throw new IllegalArgumentException("El estado 'Rentado' solo se puede asignar al crear una renta");
        }
        
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new IllegalArgumentException("Vehículo no encontrado"));
        
        // Si está rentado, no se puede cambiar el estado manualmente
        if (vehicle.getStatus() == VehicleStatus.RENTED) {
            throw new IllegalArgumentException("No se puede cambiar el estado de un vehículo rentado. Debe completar o cancelar la renta primero.");
        }
        
        switch (newStatus) {
            case AVAILABLE -> vehicle.markAsAvailable();
            case MAINTENANCE -> vehicle.sendToMaintenance();
            case OUT_OF_SERVICE -> vehicle.setStatus(VehicleStatus.OUT_OF_SERVICE);
            default -> throw new IllegalArgumentException("Estado no válido");
        }
        
        vehicleRepository.save(vehicle);
        log.info("Estado del vehículo {} actualizado a {}", vehicleId, newStatus);
    }

    /**
     * Actualiza el kilometraje de un vehículo
     */
    public void updateMileage(Long vehicleId, Integer newMileage) {
        log.info("Actualizando kilometraje del vehículo {} a {}", vehicleId, newMileage);
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new IllegalArgumentException("Vehículo no encontrado"));

        vehicle.updateMileage(newMileage); // Lógica de dominio
        vehicleRepository.save(vehicle);
    }

    /**
     * Elimina un vehículo (soft delete)
     */
    public void deleteVehicle(Long vehicleId) {
        log.info("Eliminando vehículo {}", vehicleId);
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new IllegalArgumentException("Vehículo no encontrado"));

        vehicle.delete(); // Lógica de dominio
        vehicleRepository.save(vehicle);
    }

    /**
     * Cuenta vehículos disponibles
     */
    @Transactional(readOnly = true)
    public long countAvailableVehicles() {
        return vehicleRepository.countAvailableVehicles();
    }
}
