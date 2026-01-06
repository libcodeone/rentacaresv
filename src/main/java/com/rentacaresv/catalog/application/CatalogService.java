package com.rentacaresv.catalog.application;

import com.rentacaresv.catalog.domain.VehicleBrand;
import com.rentacaresv.catalog.domain.VehicleModel;
import com.rentacaresv.catalog.infrastructure.VehicleBrandRepository;
import com.rentacaresv.catalog.infrastructure.VehicleModelRepository;
import com.rentacaresv.vehicle.domain.VehicleType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio para gestión de catálogos de marcas y modelos
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class CatalogService {

    private final VehicleBrandRepository brandRepository;
    private final VehicleModelRepository modelRepository;

    // ========================================
    // MARCAS
    // ========================================

    public Long createBrand(String name, String logoUrl) {
        log.info("Creando marca: {}", name);

        if (brandRepository.existsByNameIgnoreCase(name)) {
            throw new IllegalArgumentException("Ya existe una marca con el nombre: " + name);
        }

        VehicleBrand brand = VehicleBrand.builder()
                .name(name.trim())
                .logoUrl(logoUrl)
                .build();

        brand = brandRepository.save(brand);
        log.info("Marca creada: {} con ID: {}", name, brand.getId());

        return brand.getId();
    }

    /**
     * Método de compatibilidad - ignora el parámetro country
     */
    public Long createBrand(String name, String ignoredCountry, String logoUrl) {
        return createBrand(name, logoUrl);
    }

    public void updateBrand(Long id, String name, String logoUrl) {
        log.info("Actualizando marca ID: {}", id);

        VehicleBrand brand = brandRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Marca no encontrada"));

        // Validar nombre único
        if (!brand.getName().equalsIgnoreCase(name) && brandRepository.existsByNameIgnoreCase(name)) {
            throw new IllegalArgumentException("Ya existe otra marca con el nombre: " + name);
        }

        brand.setName(name.trim());
        brand.setLogoUrl(logoUrl);

        brandRepository.save(brand);
        log.info("Marca actualizada: {}", id);
    }

    public void deleteBrand(Long id) {
        log.info("Eliminando marca ID: {}", id);

        VehicleBrand brand = brandRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Marca no encontrada"));

        // Verificar si tiene modelos
        long modelCount = modelRepository.countByBrandId(id);
        if (modelCount > 0) {
            throw new IllegalArgumentException("No se puede eliminar la marca porque tiene " + modelCount + " modelos asociados");
        }

        brandRepository.delete(brand);
        log.info("Marca eliminada: {}", id);
    }

    public void toggleBrandActive(Long id) {
        VehicleBrand brand = brandRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Marca no encontrada"));

        if (brand.getActive()) {
            brand.deactivate();
        } else {
            brand.activate();
        }

        brandRepository.save(brand);
        log.info("Marca {} {}", id, brand.getActive() ? "activada" : "desactivada");
    }

    @Transactional(readOnly = true)
    public List<VehicleBrandDTO> findAllBrands() {
        return brandRepository.findAllOrderByName().stream()
                .map(this::toBrandDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<VehicleBrandDTO> findActiveBrands() {
        return brandRepository.findAllActive().stream()
                .map(this::toBrandDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public VehicleBrandDTO findBrandById(Long id) {
        VehicleBrand brand = brandRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Marca no encontrada"));
        return toBrandDTO(brand);
    }

    // ========================================
    // MODELOS
    // ========================================

    public Long createModel(Long brandId, String name, VehicleType vehicleType, Integer yearStart, Integer yearEnd) {
        log.info("Creando modelo: {} para marca ID: {}", name, brandId);

        VehicleBrand brand = brandRepository.findById(brandId)
                .orElseThrow(() -> new IllegalArgumentException("Marca no encontrada"));

        if (modelRepository.existsByBrandIdAndNameIgnoreCase(brandId, name)) {
            throw new IllegalArgumentException("Ya existe un modelo con el nombre: " + name + " para esta marca");
        }

        VehicleModel model = VehicleModel.builder()
                .brand(brand)
                .name(name.trim())
                .vehicleType(vehicleType)
                .yearStart(yearStart)
                .yearEnd(yearEnd)
                .build();

        model = modelRepository.save(model);
        log.info("Modelo creado: {} con ID: {}", name, model.getId());

        return model.getId();
    }

    public void updateModel(Long id, Long brandId, String name, VehicleType vehicleType, Integer yearStart, Integer yearEnd) {
        log.info("Actualizando modelo ID: {}", id);

        VehicleModel model = modelRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Modelo no encontrado"));

        VehicleBrand brand = brandRepository.findById(brandId)
                .orElseThrow(() -> new IllegalArgumentException("Marca no encontrada"));

        // Validar nombre único para la marca
        if (!model.getName().equalsIgnoreCase(name) || !model.getBrand().getId().equals(brandId)) {
            if (modelRepository.existsByBrandIdAndNameIgnoreCase(brandId, name)) {
                throw new IllegalArgumentException("Ya existe un modelo con el nombre: " + name + " para esta marca");
            }
        }

        model.setBrand(brand);
        model.setName(name.trim());
        model.setVehicleType(vehicleType);
        model.setYearStart(yearStart);
        model.setYearEnd(yearEnd);

        modelRepository.save(model);
        log.info("Modelo actualizado: {}", id);
    }

    public void deleteModel(Long id) {
        log.info("Eliminando modelo ID: {}", id);

        VehicleModel model = modelRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Modelo no encontrado"));

        // TODO: Verificar si hay vehículos usando este modelo

        modelRepository.delete(model);
        log.info("Modelo eliminado: {}", id);
    }

    public void toggleModelActive(Long id) {
        VehicleModel model = modelRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Modelo no encontrado"));

        if (model.getActive()) {
            model.deactivate();
        } else {
            model.activate();
        }

        modelRepository.save(model);
        log.info("Modelo {} {}", id, model.getActive() ? "activado" : "desactivado");
    }

    @Transactional(readOnly = true)
    public List<VehicleModelDTO> findAllModels() {
        return modelRepository.findAllOrderByBrandAndName().stream()
                .map(this::toModelDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<VehicleModelDTO> findActiveModels() {
        return modelRepository.findAllActive().stream()
                .map(this::toModelDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<VehicleModelDTO> findModelsByBrand(Long brandId) {
        return modelRepository.findByBrandIdActive(brandId).stream()
                .map(this::toModelDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<VehicleModelDTO> findModelsByType(VehicleType type) {
        return modelRepository.findByVehicleType(type).stream()
                .map(this::toModelDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public VehicleModelDTO findModelById(Long id) {
        VehicleModel model = modelRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Modelo no encontrado"));
        return toModelDTO(model);
    }

    // ========================================
    // CONTADORES
    // ========================================

    @Transactional(readOnly = true)
    public long countActiveBrands() {
        return brandRepository.countActive();
    }

    @Transactional(readOnly = true)
    public long countActiveModels() {
        return modelRepository.countActive();
    }

    // ========================================
    // MAPPERS
    // ========================================

    private VehicleBrandDTO toBrandDTO(VehicleBrand brand) {
        return VehicleBrandDTO.builder()
                .id(brand.getId())
                .name(brand.getName())
                .logoUrl(brand.getLogoUrl())
                .active(brand.getActive())
                .modelCount(brand.getModels() != null ? brand.getModels().size() : 0)
                .build();
    }

    private VehicleModelDTO toModelDTO(VehicleModel model) {
        return VehicleModelDTO.builder()
                .id(model.getId())
                .brandId(model.getBrand().getId())
                .brandName(model.getBrand().getName())
                .name(model.getName())
                .fullName(model.getFullName())
                .vehicleType(model.getVehicleType().name())
                .vehicleTypeLabel(model.getVehicleType().getLabel())
                .yearStart(model.getYearStart())
                .yearEnd(model.getYearEnd())
                .active(model.getActive())
                .build();
    }
}
