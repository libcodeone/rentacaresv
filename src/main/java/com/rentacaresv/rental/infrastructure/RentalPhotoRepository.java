package com.rentacaresv.rental.infrastructure;

import com.rentacaresv.rental.domain.photo.RentalPhoto;
import com.rentacaresv.rental.domain.photo.RentalPhotoType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Repositorio para RentalPhoto
 */
public interface RentalPhotoRepository extends JpaRepository<RentalPhoto, Long> {

    /**
     * Encuentra todas las fotos de una renta
     */
    @Query("SELECT rp FROM RentalPhoto rp WHERE rp.rental.id = :rentalId AND rp.deletedAt IS NULL ORDER BY rp.displayOrder ASC")
    List<RentalPhoto> findByRentalId(@Param("rentalId") Long rentalId);

    /**
     * Encuentra fotos por tipo
     */
    @Query("SELECT rp FROM RentalPhoto rp WHERE rp.rental.id = :rentalId AND rp.photoType = :photoType AND rp.deletedAt IS NULL ORDER BY rp.displayOrder ASC")
    List<RentalPhoto> findByRentalIdAndPhotoType(@Param("rentalId") Long rentalId, @Param("photoType") RentalPhotoType photoType);

    /**
     * Encuentra fotos de entrega
     */
    @Query("SELECT rp FROM RentalPhoto rp WHERE rp.rental.id = :rentalId AND " +
           "(rp.photoType = 'DELIVERY_EXTERIOR' OR rp.photoType = 'DELIVERY_INTERIOR' OR rp.photoType = 'DELIVERY_ACCESSORIES') " +
           "AND rp.deletedAt IS NULL ORDER BY rp.displayOrder ASC")
    List<RentalPhoto> findDeliveryPhotosByRentalId(@Param("rentalId") Long rentalId);

    /**
     * Encuentra fotos de devolución
     */
    @Query("SELECT rp FROM RentalPhoto rp WHERE rp.rental.id = :rentalId AND " +
           "(rp.photoType = 'RETURN_EXTERIOR' OR rp.photoType = 'RETURN_INTERIOR' OR rp.photoType = 'RETURN_ACCESSORIES') " +
           "AND rp.deletedAt IS NULL ORDER BY rp.displayOrder ASC")
    List<RentalPhoto> findReturnPhotosByRentalId(@Param("rentalId") Long rentalId);

    /**
     * Cuenta fotos por tipo
     */
    @Query("SELECT COUNT(rp) FROM RentalPhoto rp WHERE rp.rental.id = :rentalId AND rp.photoType = :photoType AND rp.deletedAt IS NULL")
    long countByRentalIdAndPhotoType(@Param("rentalId") Long rentalId, @Param("photoType") RentalPhotoType photoType);

    /**
     * Verifica si hay fotos de entrega
     */
    @Query("SELECT COUNT(rp) > 0 FROM RentalPhoto rp WHERE rp.rental.id = :rentalId AND " +
           "(rp.photoType = 'DELIVERY_EXTERIOR' OR rp.photoType = 'DELIVERY_INTERIOR' OR rp.photoType = 'DELIVERY_ACCESSORIES') " +
           "AND rp.deletedAt IS NULL")
    boolean hasDeliveryPhotos(@Param("rentalId") Long rentalId);

    /**
     * Verifica si hay fotos de devolución
     */
    @Query("SELECT COUNT(rp) > 0 FROM RentalPhoto rp WHERE rp.rental.id = :rentalId AND " +
           "(rp.photoType = 'RETURN_EXTERIOR' OR rp.photoType = 'RETURN_INTERIOR' OR rp.photoType = 'RETURN_ACCESSORIES') " +
           "AND rp.deletedAt IS NULL")
    boolean hasReturnPhotos(@Param("rentalId") Long rentalId);

    /**
     * Elimina todas las fotos de una renta
     */
    @Modifying
    @Query("UPDATE RentalPhoto rp SET rp.deletedAt = CURRENT_TIMESTAMP WHERE rp.rental.id = :rentalId")
    void deleteAllByRentalId(@Param("rentalId") Long rentalId);
}
