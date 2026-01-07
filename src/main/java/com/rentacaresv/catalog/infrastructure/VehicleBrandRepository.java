package com.rentacaresv.catalog.infrastructure;

import com.rentacaresv.catalog.domain.VehicleBrand;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VehicleBrandRepository extends JpaRepository<VehicleBrand, Long> {

    Optional<VehicleBrand> findByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCase(String name);

    @Query("SELECT b FROM VehicleBrand b WHERE b.active = true ORDER BY b.name")
    List<VehicleBrand> findAllActive();

    @Query("SELECT b FROM VehicleBrand b ORDER BY b.name")
    List<VehicleBrand> findAllOrderByName();

    @Query("SELECT COUNT(b) FROM VehicleBrand b WHERE b.active = true")
    long countActive();
}
