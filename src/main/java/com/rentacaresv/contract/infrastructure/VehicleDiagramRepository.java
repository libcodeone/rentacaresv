package com.rentacaresv.contract.infrastructure;

import com.rentacaresv.contract.domain.VehicleDiagram;
import com.rentacaresv.vehicle.domain.VehicleType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio de VehicleDiagram
 */
public interface VehicleDiagramRepository extends JpaRepository<VehicleDiagram, Long> {

    /**
     * Busca el diagrama para un tipo de vehículo
     */
    Optional<VehicleDiagram> findByVehicleTypeAndIsActiveTrue(VehicleType vehicleType);

    /**
     * Busca el diagrama por tipo de vehículo (incluyendo inactivos)
     */
    Optional<VehicleDiagram> findByVehicleType(VehicleType vehicleType);

    /**
     * Lista todos los diagramas activos
     */
    List<VehicleDiagram> findByIsActiveTrueOrderByName();

    /**
     * Verifica si existe diagrama para un tipo
     */
    boolean existsByVehicleType(VehicleType vehicleType);
}
