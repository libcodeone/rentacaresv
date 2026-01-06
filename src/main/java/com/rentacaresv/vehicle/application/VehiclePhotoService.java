package com.rentacaresv.vehicle.application;

import com.rentacaresv.shared.storage.FileStorageService;
import com.rentacaresv.shared.storage.FolderType;
import com.rentacaresv.shared.storage.StorageInitializer;
import com.rentacaresv.vehicle.domain.Vehicle;
import com.rentacaresv.vehicle.domain.photo.PhotoType;
import com.rentacaresv.vehicle.domain.photo.VehiclePhoto;
import com.rentacaresv.vehicle.infrastructure.VehiclePhotoRepository;
import com.rentacaresv.vehicle.infrastructure.VehicleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.List;

/**
 * Servicio para gestión de fotos de vehículos
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class VehiclePhotoService {

    private final VehiclePhotoRepository vehiclePhotoRepository;
    private final VehicleRepository vehicleRepository;
    private final FileStorageService fileStorageService;
    private final StorageInitializer storageInitializer;

    // Límites de fotos
    private static final int MAX_EXTERIOR_PHOTOS = 10;
    private static final int MAX_INTERIOR_PHOTOS = 10;

    /**
     * Sube una foto de vehículo
     */
    public VehiclePhoto uploadPhoto(Long vehicleId, InputStream inputStream, String fileName,
                                     String contentType, PhotoType photoType) {

        // Inicializar storage si no está inicializado (lazy initialization)
        storageInitializer.initializeIfNeeded();

        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new IllegalArgumentException("Vehículo no encontrado"));

        // Verificar límites
        long currentCount = vehiclePhotoRepository.countByVehicleIdAndPhotoType(vehicleId, photoType);
        int maxPhotos = photoType == PhotoType.EXTERIOR ? MAX_EXTERIOR_PHOTOS : MAX_INTERIOR_PHOTOS;

        if (currentCount >= maxPhotos) {
            log.warn("Se alcanzó el límite de {} fotos de tipo {} para vehículo {}", 
                maxPhotos, photoType, vehicleId);
        }

        // Subir archivo a DO Spaces
        String subFolder = "vehicle-" + vehicleId;
        String photoUrl = fileStorageService.uploadFile(inputStream, fileName, contentType,
                FolderType.CARS, subFolder);

        // Obtener el siguiente orden
        List<VehiclePhoto> existingPhotos = vehiclePhotoRepository.findByVehicleIdAndPhotoType(vehicleId, photoType);
        int nextOrder = existingPhotos.size() + 1;

        // Crear registro en BD
        VehiclePhoto vehiclePhoto = VehiclePhoto.builder()
                .vehicle(vehicle)
                .photoUrl(photoUrl)
                .photoType(photoType)
                .isPrimary(false)
                .displayOrder(nextOrder)
                .build();

        vehiclePhoto = vehiclePhotoRepository.save(vehiclePhoto);
        log.info("Foto subida: {} para vehículo {}", photoUrl, vehicleId);

        return vehiclePhoto;
    }

    /**
     * Marca una foto como principal
     */
    public void markAsPrimary(Long photoId) {
        VehiclePhoto photo = vehiclePhotoRepository.findById(photoId)
                .orElseThrow(() -> new IllegalArgumentException("Foto no encontrada"));

        // Desmarcar todas las demás
        vehiclePhotoRepository.unmarkAllAsPrimary(photo.getVehicle().getId());

        // Marcar esta como principal
        photo.markAsPrimary();
        vehiclePhotoRepository.save(photo);

        log.info("Foto {} marcada como principal", photoId);
    }

    /**
     * Elimina una foto
     */
    public void deletePhoto(Long photoId) {
        VehiclePhoto photo = vehiclePhotoRepository.findById(photoId)
                .orElseThrow(() -> new IllegalArgumentException("Foto no encontrada"));

        // Eliminar de DO Spaces
        try {
            fileStorageService.deleteFile(photo.getPhotoUrl());
        } catch (Exception e) {
            log.error("Error eliminando archivo de DO Spaces: {}", e.getMessage());
        }

        // Soft delete en BD
        photo.delete();
        vehiclePhotoRepository.save(photo);

        log.info("Foto {} eliminada", photoId);
    }

    /**
     * Obtiene todas las fotos de un vehículo
     */
    @Transactional(readOnly = true)
    public List<VehiclePhoto> getVehiclePhotos(Long vehicleId) {
        return vehiclePhotoRepository.findByVehicleId(vehicleId);
    }

    /**
     * Obtiene la foto principal de un vehículo
     */
    @Transactional(readOnly = true)
    public String getPrimaryPhotoUrl(Long vehicleId) {
        return vehiclePhotoRepository.findPrimaryPhotoByVehicleId(vehicleId)
                .map(VehiclePhoto::getPhotoUrl)
                .orElse(null);
    }

    /**
     * Obtiene fotos por tipo
     */
    @Transactional(readOnly = true)
    public List<VehiclePhoto> getPhotosByType(Long vehicleId, PhotoType photoType) {
        return vehiclePhotoRepository.findByVehicleIdAndPhotoType(vehicleId, photoType);
    }

    /**
     * Verifica si se alcanzó el límite de fotos
     */
    @Transactional(readOnly = true)
    public boolean hasReachedLimit(Long vehicleId, PhotoType photoType) {
        long count = vehiclePhotoRepository.countByVehicleIdAndPhotoType(vehicleId, photoType);
        int maxPhotos = photoType == PhotoType.EXTERIOR ? MAX_EXTERIOR_PHOTOS : MAX_INTERIOR_PHOTOS;
        return count >= maxPhotos;
    }

    /**
     * Obtiene el conteo de fotos por tipo
     */
    @Transactional(readOnly = true)
    public long getPhotoCount(Long vehicleId, PhotoType photoType) {
        return vehiclePhotoRepository.countByVehicleIdAndPhotoType(vehicleId, photoType);
    }
}
