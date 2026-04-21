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
     * Lista todos los vehículos publicados con su disponibilidad.
     * GET /api/public/vehicles?yearFrom=2020&yearTo=2025&sortBy=price_asc
     *
     * @param yearFrom Año mínimo (opcional)
     * @param yearTo   Año máximo (opcional)
     * @param sortBy   Ordenar por: price_asc, price_desc, year_desc, year_asc, brand_asc (opcional)
     */
    @GetMapping
    public ResponseEntity<List<PublicVehicleDTO>> getAllVehicles(
            @RequestParam(required = false) Integer yearFrom,
            @RequestParam(required = false) Integer yearTo,
            @RequestParam(required = false) String sortBy) {
        List<PublicVehicleDTO> vehicles = publicVehicleService.findAllVehicles(yearFrom, yearTo, sortBy);
        return ResponseEntity.ok(vehicles);
    }

    /**
     * Obtiene el detalle de un vehículo publicado por ID.
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
     * Obtiene vehículos destacados disponibles (para landing page).
     * GET /api/public/vehicles/featured?limit=6
     */
    @GetMapping("/featured")
    public ResponseEntity<List<PublicVehicleDTO>> getFeaturedVehicles(
            @RequestParam(defaultValue = "6") int limit) {
        List<PublicVehicleDTO> vehicles = publicVehicleService.findFeaturedVehicles(limit);
        return ResponseEntity.ok(vehicles);
    }
}
