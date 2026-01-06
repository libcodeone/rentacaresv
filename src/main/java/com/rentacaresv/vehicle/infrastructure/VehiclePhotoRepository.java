package com.rentacaresv.vehicle.infrastructure;

import com.rentacaresv.vehicle.domain.photo.PhotoType;
import com.rentacaresv.vehicle.domain.photo.VehiclePhoto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio para VehiclePhoto
 */
public interface VehiclePhotoRepository extends JpaRepository<VehiclePhoto, Long> {

    /**
     * Encuentra todas las fotos de un vehículo (no eliminadas)
     */
    @Query("SELECT vp FROM VehiclePhoto vp WHERE vp.vehicle.id = :vehicleId AND vp.deletedAt IS NULL ORDER BY vp.displayOrder ASC")
    List<VehiclePhoto> findByVehicleId(@Param("vehicleId") Long vehicleId);

    /**
     * Encuentra la foto principal de un vehículo
     */
    @Query("SELECT vp FROM VehiclePhoto vp WHERE vp.vehicle.id = :vehicleId AND vp.isPrimary = true AND vp.deletedAt IS NULL")
    Optional<VehiclePhoto> findPrimaryPhotoByVehicleId(@Param("vehicleId") Long vehicleId);

    /**
     * Encuentra fotos por tipo
     */
    @Query("SELECT vp FROM VehiclePhoto vp WHERE vp.vehicle.id = :vehicleId AND vp.photoType = :photoType AND vp.deletedAt IS NULL ORDER BY vp.displayOrder ASC")
    List<VehiclePhoto> findByVehicleIdAndPhotoType(@Param("vehicleId") Long vehicleId, @Param("photoType") PhotoType photoType);

    /**
     * Cuenta fotos por tipo
     */
    @Query("SELECT COUNT(vp) FROM VehiclePhoto vp WHERE vp.vehicle.id = :vehicleId AND vp.photoType = :photoType AND vp.deletedAt IS NULL")
    long countByVehicleIdAndPhotoType(@Param("vehicleId") Long vehicleId, @Param("photoType") PhotoType photoType);

    /**
     * Desmarca todas las fotos como no principales de un vehículo
     */
    @Modifying
    @Query("UPDATE VehiclePhoto vp SET vp.isPrimary = false WHERE vp.vehicle.id = :vehicleId")
    void unmarkAllAsPrimary(@Param("vehicleId") Long vehicleId);

    /**
     * Elimina todas las fotos de un vehículo
     */
    @Modifying
    @Query("UPDATE VehiclePhoto vp SET vp.deletedAt = CURRENT_TIMESTAMP WHERE vp.vehicle.id = :vehicleId")
    void deleteAllByVehicleId(@Param("vehicleId") Long vehicleId);
}
