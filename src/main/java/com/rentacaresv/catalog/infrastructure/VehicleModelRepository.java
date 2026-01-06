package com.rentacaresv.catalog.infrastructure;

import com.rentacaresv.catalog.domain.VehicleModel;
import com.rentacaresv.vehicle.domain.VehicleType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VehicleModelRepository extends JpaRepository<VehicleModel, Long> {

    @Query("SELECT m FROM VehicleModel m WHERE m.brand.id = :brandId AND m.active = true ORDER BY m.name")
    List<VehicleModel> findByBrandIdActive(@Param("brandId") Long brandId);

    @Query("SELECT m FROM VehicleModel m WHERE m.brand.id = :brandId ORDER BY m.name")
    List<VehicleModel> findByBrandId(@Param("brandId") Long brandId);

    @Query("SELECT m FROM VehicleModel m WHERE m.active = true ORDER BY m.brand.name, m.name")
    List<VehicleModel> findAllActive();

    @Query("SELECT m FROM VehicleModel m ORDER BY m.brand.name, m.name")
    List<VehicleModel> findAllOrderByBrandAndName();

    @Query("SELECT m FROM VehicleModel m WHERE m.vehicleType = :type AND m.active = true ORDER BY m.brand.name, m.name")
    List<VehicleModel> findByVehicleType(@Param("type") VehicleType type);

    boolean existsByBrandIdAndNameIgnoreCase(Long brandId, String name);

    Optional<VehicleModel> findByBrandIdAndNameIgnoreCase(Long brandId, String name);

    @Query("SELECT COUNT(m) FROM VehicleModel m WHERE m.active = true")
    long countActive();

    @Query("SELECT COUNT(m) FROM VehicleModel m WHERE m.brand.id = :brandId AND m.active = true")
    long countByBrandId(@Param("brandId") Long brandId);
}
