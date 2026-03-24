package com.rentacaresv.vehicle.infrastructure.api;

import com.rentacaresv.vehicle.application.PublicVehicleDTO;
import com.rentacaresv.vehicle.application.PublicVehicleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * API REST pública para exponer vehículos con disponibilidad.
 * No requiere autenticación. Consumida por la página web pública (novarentacarsv.com).
 */
@RestController
@RequestMapping("/api/public/vehicles")
@RequiredArgsConstructor
public class PublicVehicleController {

    private final PublicVehicleService publicVehicleService;

    /**
     * Lista todos los vehículos activos con su disponibilidad.
     * GET /api/public/vehicles
     */
    @GetMapping
    public ResponseEntity<List<PublicVehicleDTO>> getAllVehicles() {
        List<PublicVehicleDTO> vehicles = publicVehicleService.findAllVehicles();
        return ResponseEntity.ok(vehicles);
    }

    /**
     * Obtiene el detalle de un vehículo por ID (incluye períodos reservados).
     * GET /api/public/vehicles/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<PublicVehicleDTO> getVehicleById(@PathVariable Long id) {
        try {
            PublicVehicleDTO vehicle = publicVehicleService.findById(id);
            return ResponseEntity.ok(vehicle);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Obtiene vehículos destacados disponibles ahora (para el hero/landing page).
     * GET /api/public/vehicles/featured?limit=6
     */
    @GetMapping("/featured")
    public ResponseEntity<List<PublicVehicleDTO>> getFeaturedVehicles(
            @RequestParam(defaultValue = "6") int limit) {
        List<PublicVehicleDTO> vehicles = publicVehicleService.findFeaturedVehicles(limit);
        return ResponseEntity.ok(vehicles);
    }
}
