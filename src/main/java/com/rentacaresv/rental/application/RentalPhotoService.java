package com.rentacaresv.rental.application;

import com.rentacaresv.rental.domain.Rental;
import com.rentacaresv.rental.domain.photo.RentalPhoto;
import com.rentacaresv.rental.domain.photo.RentalPhotoType;
import com.rentacaresv.rental.infrastructure.RentalPhotoRepository;
import com.rentacaresv.rental.infrastructure.RentalRepository;
import com.rentacaresv.shared.storage.FileStorageService;
import com.rentacaresv.shared.storage.FolderType;
import com.rentacaresv.shared.storage.StorageInitializer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.List;

/**
 * Servicio para gestión de fotos de rentas (entrega/devolución)
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class RentalPhotoService {

    private final RentalPhotoRepository rentalPhotoRepository;
    private final RentalRepository rentalRepository;
    private final FileStorageService fileStorageService;
    private final StorageInitializer storageInitializer;

    // Límites de fotos
    private static final int MAX_EXTERIOR_PHOTOS = 10;
    private static final int MAX_INTERIOR_PHOTOS = 10;
    private static final int MAX_ACCESSORIES_PHOTOS = 10;

    /**
     * Sube una foto de renta (entrega o devolución)
     */
    public RentalPhoto uploadPhoto(Long rentalId, InputStream inputStream, String fileName,
                                    String contentType, RentalPhotoType photoType, String description) {

        // Inicializar storage si no está inicializado (lazy initialization)
        storageInitializer.initializeIfNeeded();

        Rental rental = rentalRepository.findById(rentalId)
                .orElseThrow(() -> new IllegalArgumentException("Renta no encontrada"));

        // Verificar límites
        long currentCount = rentalPhotoRepository.countByRentalIdAndPhotoType(rentalId, photoType);
        int maxPhotos = getMaxPhotosForType(photoType);

        if (currentCount >= maxPhotos) {
            log.warn("Se alcanzó el límite de {} fotos de tipo {} para renta {}",
                    maxPhotos, photoType, rentalId);
        }

        // Determinar subcarpeta (delivery o return)
        String subfolder = photoType.isDelivery() ?
                "rental-" + rentalId + "-delivery" :
                "rental-" + rentalId + "-return";

        // Subir archivo a DO Spaces
        String photoUrl = fileStorageService.uploadFile(inputStream, fileName, contentType,
                FolderType.CAR_DETAILS, subfolder);

        // Obtener el siguiente orden
        List<RentalPhoto> existingPhotos = rentalPhotoRepository.findByRentalIdAndPhotoType(rentalId, photoType);
        int nextOrder = existingPhotos.size() + 1;

        // Crear registro en BD
        RentalPhoto rentalPhoto = RentalPhoto.builder()
                .rental(rental)
                .photoUrl(photoUrl)
                .photoType(photoType)
                .description(description)
                .displayOrder(nextOrder)
                .build();

        rentalPhoto = rentalPhotoRepository.save(rentalPhoto);
        log.info("Foto subida: {} para renta {}", photoUrl, rentalId);

        return rentalPhoto;
    }

    /**
     * Elimina una foto
     */
    public void deletePhoto(Long photoId) {
        RentalPhoto photo = rentalPhotoRepository.findById(photoId)
                .orElseThrow(() -> new IllegalArgumentException("Foto no encontrada"));

        // Eliminar de DO Spaces
        try {
            fileStorageService.deleteFile(photo.getPhotoUrl());
        } catch (Exception e) {
            log.error("Error eliminando archivo de DO Spaces: {}", e.getMessage());
        }

        // Soft delete en BD
        photo.delete();
        rentalPhotoRepository.save(photo);

        log.info("Foto {} eliminada", photoId);
    }

    /**
     * Obtiene todas las fotos de una renta
     */
    @Transactional(readOnly = true)
    public List<RentalPhoto> getRentalPhotos(Long rentalId) {
        return rentalPhotoRepository.findByRentalId(rentalId);
    }

    /**
     * Obtiene fotos de entrega
     */
    @Transactional(readOnly = true)
    public List<RentalPhoto> getDeliveryPhotos(Long rentalId) {
        return rentalPhotoRepository.findDeliveryPhotosByRentalId(rentalId);
    }

    /**
     * Obtiene fotos de devolución
     */
    @Transactional(readOnly = true)
    public List<RentalPhoto> getReturnPhotos(Long rentalId) {
        return rentalPhotoRepository.findReturnPhotosByRentalId(rentalId);
    }

    /**
     * Obtiene fotos por tipo específico
     */
    @Transactional(readOnly = true)
    public List<RentalPhoto> getPhotosByType(Long rentalId, RentalPhotoType photoType) {
        return rentalPhotoRepository.findByRentalIdAndPhotoType(rentalId, photoType);
    }

    /**
     * Verifica si hay fotos de entrega
     */
    @Transactional(readOnly = true)
    public boolean hasDeliveryPhotos(Long rentalId) {
        return rentalPhotoRepository.hasDeliveryPhotos(rentalId);
    }

    /**
     * Verifica si hay fotos de devolución
     */
    @Transactional(readOnly = true)
    public boolean hasReturnPhotos(Long rentalId) {
        return rentalPhotoRepository.hasReturnPhotos(rentalId);
    }

    /**
     * Verifica si se alcanzó el límite de fotos
     */
    @Transactional(readOnly = true)
    public boolean hasReachedLimit(Long rentalId, RentalPhotoType photoType) {
        long count = rentalPhotoRepository.countByRentalIdAndPhotoType(rentalId, photoType);
        return count >= getMaxPhotosForType(photoType);
    }

    /**
     * Obtiene el conteo de fotos por tipo
     */
    @Transactional(readOnly = true)
    public long getPhotoCount(Long rentalId, RentalPhotoType photoType) {
        return rentalPhotoRepository.countByRentalIdAndPhotoType(rentalId, photoType);
    }

    private int getMaxPhotosForType(RentalPhotoType photoType) {
        return switch (photoType) {
            case DELIVERY_EXTERIOR, RETURN_EXTERIOR -> MAX_EXTERIOR_PHOTOS;
            case DELIVERY_INTERIOR, RETURN_INTERIOR -> MAX_INTERIOR_PHOTOS;
            case DELIVERY_ACCESSORIES, RETURN_ACCESSORIES -> MAX_ACCESSORIES_PHOTOS;
        };
    }
}
