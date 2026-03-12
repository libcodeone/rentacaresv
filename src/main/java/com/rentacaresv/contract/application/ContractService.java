package com.rentacaresv.contract.application;

import com.rentacaresv.contract.domain.*;
import com.rentacaresv.contract.infrastructure.*;
import com.rentacaresv.rental.domain.Rental;
import com.rentacaresv.rental.infrastructure.RentalRepository;
import com.rentacaresv.shared.storage.FileStorageService;
import com.rentacaresv.shared.storage.FolderType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

/**
 * Servicio de aplicación para gestión de Contratos Digitales
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ContractService {

    private final ContractRepository contractRepository;
    private final ContractAccessoryRepository contractAccessoryRepository;
    private final ContractDamageMarkRepository contractDamageMarkRepository;
    private final AccessoryCatalogRepository accessoryCatalogRepository;
    private final VehicleDiagramRepository vehicleDiagramRepository;
    private final RentalRepository rentalRepository;
    private final FileStorageService fileStorageService;
    private final ContractPdfGenerator pdfGenerator;
    private final ContractEmailService emailService;

    // ========================================
    // Operaciones de lectura
    // ========================================

    /**
     * Busca un contrato por su token (para acceso público)
     * Carga todas las relaciones necesarias en múltiples consultas
     * para evitar MultipleBagFetchException
     */
    @Transactional(readOnly = true)
    public Optional<Contract> findByToken(String token) {
        // Primero cargar el contrato básico con rental, vehicle, customer
        Optional<Contract> contractOpt = contractRepository.findByTokenBasic(token);
        
        if (contractOpt.isEmpty()) {
            return Optional.empty();
        }
        
        Contract contract = contractOpt.get();
        
        // Cargar accesorios (Hibernate los inicializa en la misma sesión)
        contractRepository.findByIdWithAccessories(contract.getId());
        
        // Cargar marcas de daño
        contractRepository.findByIdWithDamageMarks(contract.getId());
        
        return Optional.of(contract);
    }

    /**
     * Busca un contrato por ID con todas sus relaciones
     */
    @Transactional(readOnly = true)
    public Optional<Contract> findByIdWithRelations(Long id) {
        Optional<Contract> contractOpt = contractRepository.findByIdBasic(id);
        
        if (contractOpt.isEmpty()) {
            return Optional.empty();
        }
        
        Contract contract = contractOpt.get();
        
        // Cargar accesorios
        contractRepository.findByIdWithAccessories(contract.getId());
        
        // Cargar marcas de daño
        contractRepository.findByIdWithDamageMarks(contract.getId());
        
        return Optional.of(contract);
    }

    /**
     * Busca un contrato por ID
     */
    public Optional<Contract> findById(Long id) {
        return contractRepository.findById(id);
    }

    /**
     * Busca el contrato de una renta
     */
    public Optional<Contract> findByRentalId(Long rentalId) {
        return contractRepository.findByRentalId(rentalId);
    }

    /**
     * Verifica si una renta ya tiene contrato
     */
    public boolean existsForRental(Long rentalId) {
        return contractRepository.existsByRentalId(rentalId);
    }

    /**
     * Obtiene todos los accesorios del catálogo activos
     */
    public List<AccessoryCatalog> getActiveAccessories() {
        return accessoryCatalogRepository.findAllActiveOrdered();
    }

    /**
     * Obtiene el diagrama para un tipo de vehículo
     */
    public Optional<VehicleDiagram> getDiagramForVehicleType(com.rentacaresv.vehicle.domain.VehicleType type) {
        return vehicleDiagramRepository.findByVehicleTypeAndIsActiveTrue(type);
    }

    /**
     * Lista contratos por estado
     */
    public List<Contract> findByStatus(ContractStatus status) {
        return contractRepository.findByStatus(status);
    }

    // ========================================
    // Crear contrato
    // ========================================

    /**
     * Crea un nuevo contrato para una renta
     */
    @Transactional
    public Contract createContract(Long rentalId) {
        log.info("Creando contrato para renta ID: {}", rentalId);

        // Verificar que la renta existe
        Rental rental = rentalRepository.findById(rentalId)
                .orElseThrow(() -> new IllegalArgumentException("Renta no encontrada: " + rentalId));

        // Verificar que no exista ya un contrato
        if (contractRepository.existsByRentalId(rentalId)) {
            throw new IllegalStateException("Ya existe un contrato para esta renta");
        }

        // Crear contrato
        Contract contract = Contract.builder()
                .rental(rental)
                .status(ContractStatus.PENDING)
                .mileageOut(rental.getVehicle().getMileage())
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();

        contract = contractRepository.save(contract);

        // Agregar accesorios del catálogo
        List<AccessoryCatalog> accessories = accessoryCatalogRepository.findAllActiveOrdered();
        for (AccessoryCatalog catalogItem : accessories) {
            ContractAccessory accessory = ContractAccessory.builder()
                    .contract(contract)
                    .accessoryCatalogId(catalogItem.getId())
                    .accessoryName(catalogItem.getName())
                    .isPresent(true) // Por defecto todos presentes
                    .displayOrder(catalogItem.getDisplayOrder())
                    .build();
            contract.addAccessory(accessory);
        }

        contract = contractRepository.save(contract);

        log.info("✅ Contrato creado con token: {}", contract.getToken());
        return contract;
    }

    /**
     * Genera la URL pública del contrato
     */
    public String getPublicUrl(Contract contract, String baseUrl) {
        return baseUrl + "/public/contract/" + contract.getToken();
    }

    // ========================================
    // Actualizar contrato (desde vista pública)
    // ========================================

    /**
     * Actualiza la información del documento de identidad
     */
    @Transactional
    public Contract updateDocumentInfo(String token, DocumentType documentType, 
                                        String documentNumber, String frontPhotoBase64, 
                                        String backPhotoBase64) {
        Contract contract = findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Contrato no encontrado"));

        if (!contract.canBeAccessed()) {
            throw new IllegalStateException("El contrato no puede ser modificado");
        }

        String frontUrl = null;
        String backUrl = null;

        // Subir foto frontal
        if (frontPhotoBase64 != null && !frontPhotoBase64.isEmpty()) {
            frontUrl = uploadBase64Image(frontPhotoBase64, "doc_front_" + contract.getId(), 
                    FolderType.CONTRACT_DOCUMENTS);
        }

        // Subir foto trasera (opcional)
        if (backPhotoBase64 != null && !backPhotoBase64.isEmpty()) {
            backUrl = uploadBase64Image(backPhotoBase64, "doc_back_" + contract.getId(), 
                    FolderType.CONTRACT_DOCUMENTS);
        }

        contract.updateDocumentInfo(documentType, documentNumber, frontUrl, backUrl);
        return contractRepository.save(contract);
    }

    /**
     * Actualiza el checklist de accesorios
     */
    @Transactional
    public Contract updateAccessories(String token, List<ContractAccessoryDTO> accessoriesDTO) {
        Contract contract = findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Contrato no encontrado"));

        if (!contract.canBeAccessed()) {
            throw new IllegalStateException("El contrato no puede ser modificado");
        }

        // Actualizar cada accesorio
        for (ContractAccessoryDTO dto : accessoriesDTO) {
            contract.getAccessories().stream()
                    .filter(a -> a.getId().equals(dto.getId()))
                    .findFirst()
                    .ifPresent(accessory -> {
                        accessory.setIsPresent(dto.getIsPresent());
                        accessory.setObservations(dto.getObservations());
                    });
        }

        // Agregar accesorios personalizados ("Otros")
        if (accessoriesDTO.stream().anyMatch(dto -> dto.getId() == null && dto.getAccessoryName() != null)) {
            accessoriesDTO.stream()
                    .filter(dto -> dto.getId() == null && dto.getAccessoryName() != null)
                    .forEach(dto -> {
                        ContractAccessory custom = ContractAccessory.builder()
                                .contract(contract)
                                .accessoryName(dto.getAccessoryName())
                                .isPresent(dto.getIsPresent())
                                .observations(dto.getObservations())
                                .displayOrder(999)
                                .build();
                        contract.addAccessory(custom);
                    });
        }

        return contractRepository.save(contract);
    }

    /**
     * Actualiza las marcas de daño en el diagrama
     */
    @Transactional
    public Contract updateDamageMarks(String token, List<ContractDamageMarkDTO> damageMarksDTO) {
        Contract contract = findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Contrato no encontrado"));

        if (!contract.canBeAccessed()) {
            throw new IllegalStateException("El contrato no puede ser modificado");
        }

        // Limpiar marcas existentes y agregar nuevas
        contract.getDamageMarks().clear();

        for (ContractDamageMarkDTO dto : damageMarksDTO) {
            ContractDamageMark mark = ContractDamageMark.builder()
                    .contract(contract)
                    .positionX(dto.getPositionX())
                    .positionY(dto.getPositionY())
                    .damageType(dto.getDamageType())
                    .description(dto.getDescription())
                    .isPreExisting(true)
                    .severity(dto.getSeverity() != null ? dto.getSeverity() : 1)
                    .build();
            contract.addDamageMark(mark);
        }

        return contractRepository.save(contract);
    }

    /**
     * Actualiza información del vehículo (kilometraje, combustible)
     */
    @Transactional
    public Contract updateVehicleInfo(String token, Integer mileageOut, Integer fuelLevelOut) {
        Contract contract = findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Contrato no encontrado"));

        if (!contract.canBeAccessed()) {
            throw new IllegalStateException("El contrato no puede ser modificado");
        }

        contract.updateVehicleInfo(mileageOut, fuelLevelOut);
        return contractRepository.save(contract);
    }

    /**
     * Actualiza información adicional del contrato (nuevo formato papel)
     */
    @Transactional
    public Contract updateAdditionalInfo(String token,
                                          String deliveryLocation,
                                          String addressElSalvador,
                                          String addressForeign,
                                          String phoneUsa,
                                          String phoneFamily,
                                          PaymentMethod paymentMethod,
                                          java.math.BigDecimal depositAmount,
                                          java.math.BigDecimal accidentDeductible,
                                          java.math.BigDecimal theftDeductible,
                                          String additionalDriverName,
                                          String additionalDriverLicense,
                                          String additionalDriverDui,
                                          String observations) {
        Contract contract = findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Contrato no encontrado"));

        if (!contract.canBeAccessed()) {
            throw new IllegalStateException("El contrato no puede ser modificado");
        }

        contract.setDeliveryLocation(deliveryLocation);
        contract.setAddressElSalvador(addressElSalvador);
        contract.setAddressForeign(addressForeign);
        contract.setPhoneUsa(phoneUsa);
        contract.setPhoneFamily(phoneFamily);
        contract.setPaymentMethod(paymentMethod);
        contract.setDepositAmount(depositAmount);
        contract.setAccidentDeductible(accidentDeductible);
        contract.setTheftDeductible(theftDeductible);
        contract.setAdditionalDriverName(additionalDriverName);
        contract.setAdditionalDriverLicense(additionalDriverLicense);
        contract.setAdditionalDriverDui(additionalDriverDui);
        contract.setObservations(observations);

        return contractRepository.save(contract);
    }

    // ========================================
    // Firmar contrato
    // ========================================

    /**
     * Firma el contrato digitalmente
     */
    @Transactional
    public Contract signContract(String token, String signatureBase64, String ipAddress, String userAgent) {
        log.info("Firmando contrato con token: {}", token);

        Contract contract = findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Contrato no encontrado"));

        if (!contract.canBeSigned()) {
            throw new IllegalStateException("El contrato no puede ser firmado. Estado: " + contract.getStatus());
        }

        // Subir firma
        String signatureUrl = uploadBase64Image(signatureBase64, "signature_" + contract.getId(), 
                FolderType.CONTRACT_SIGNATURES);

        // Firmar
        contract.sign(signatureUrl, ipAddress, userAgent);
        contract = contractRepository.save(contract);

        log.info("✅ Contrato firmado exitosamente. ID: {}", contract.getId());

        // Generar PDF y guardarlo
        try {
            contract = generateAndSavePdf(contract);
            
            // Enviar contrato firmado por email (asíncrono)
            emailService.sendSignedContractEmail(contract);
            
        } catch (Exception e) {
            log.error("Error generando PDF o enviando email: {}", e.getMessage(), e);
            // No lanzamos excepción para no bloquear la firma
        }

        return contract;
    }

    /**
     * Genera el PDF del contrato y lo sube a Digital Ocean Spaces
     */
    @Transactional
    public Contract generateAndSavePdf(Contract contract) {
        try {
            log.info("Generando PDF para contrato ID: {}", contract.getId());
            log.info("URL firma cliente: {}", contract.getSignatureUrl());
            log.info("URL firma empleado: {}", contract.getEmployeeSignatureUrl());

            // Forzar flush para asegurar que los datos están en BD
            contractRepository.flush();

            // Recargar el contrato con todas las relaciones para evitar LazyInitializationException
            Contract fullContract = findByIdWithRelations(contract.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Contrato no encontrado"));
            
            log.info("Después de recargar - URL firma cliente: {}", fullContract.getSignatureUrl());
            log.info("Después de recargar - URL firma empleado: {}", fullContract.getEmployeeSignatureUrl());

            // Generar PDF
            byte[] pdfBytes = pdfGenerator.generatePdf(fullContract);

            // Subir a Digital Ocean Spaces
            String fileName = "contrato_" + fullContract.getRental().getContractNumber() + "_" + fullContract.getId() + ".pdf";
            String pdfUrl = fileStorageService.uploadFile(
                    new ByteArrayInputStream(pdfBytes),
                    fileName,
                    "application/pdf",
                    FolderType.CONTRACT_DOCUMENTS,
                    null
            );

            // Actualizar contrato con URL del PDF
            fullContract.setPdfUrl(pdfUrl);
            fullContract = contractRepository.save(fullContract);

            log.info("✅ PDF generado y guardado: {}", pdfUrl);
            return fullContract;

        } catch (Exception e) {
            log.error("Error generando/guardando PDF: {}", e.getMessage(), e);
            throw new RuntimeException("Error al generar PDF: " + e.getMessage(), e);
        }
    }

    /**
     * Regenera el PDF de un contrato existente
     */
    @Transactional
    public Contract regeneratePdf(Long contractId) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new IllegalArgumentException("Contrato no encontrado"));
        
        return generateAndSavePdf(contract);
    }

    // ========================================
    // Cancelar / Expirar contratos
    // ========================================

    /**
     * Cancela un contrato pendiente
     */
    @Transactional
    public Contract cancelContract(Long contractId) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new IllegalArgumentException("Contrato no encontrado"));

        contract.cancel();
        return contractRepository.save(contract);
    }

    /**
     * Marca contratos expirados (ejecutar periódicamente)
     */
    @Transactional
    public int markExpiredContracts() {
        List<Contract> expired = contractRepository.findExpiredContracts(LocalDateTime.now());
        int count = 0;

        for (Contract contract : expired) {
            contract.markAsExpired();
            contractRepository.save(contract);
            count++;
        }

        if (count > 0) {
            log.info("Marcados {} contratos como expirados", count);
        }

        return count;
    }

    // ========================================
    // Métodos auxiliares
    // ========================================

    /**
     * Sube una imagen en Base64 a Digital Ocean Spaces
     */
    private String uploadBase64Image(String base64Data, String fileName, FolderType folderType) {
        try {
            // Remover prefijo data:image/xxx;base64, si existe
            String base64Clean = base64Data;
            String mimeType = "image/png";

            if (base64Data.contains(",")) {
                String[] parts = base64Data.split(",");
                if (parts[0].contains("image/jpeg")) {
                    mimeType = "image/jpeg";
                } else if (parts[0].contains("image/png")) {
                    mimeType = "image/png";
                }
                base64Clean = parts[1];
            }

            byte[] imageBytes = Base64.getDecoder().decode(base64Clean);
            InputStream inputStream = new ByteArrayInputStream(imageBytes);

            String extension = mimeType.equals("image/jpeg") ? ".jpg" : ".png";

            return fileStorageService.uploadFile(
                    inputStream,
                    fileName + extension,
                    mimeType,
                    folderType,
                    null
            );
        } catch (Exception e) {
            log.error("Error subiendo imagen base64: {}", e.getMessage(), e);
            throw new RuntimeException("Error al subir imagen: " + e.getMessage(), e);
        }
    }

    // ========================================
    // DTOs internos
    // ========================================

    /**
     * Firma el contrato con firma del cliente Y del empleado
     */
    @Transactional
    public Contract signContractWithEmployeeSignature(String token, 
                                                       String clientSignatureBase64, 
                                                       String employeeSignatureBase64,
                                                       String employeeName,
                                                       String ipAddress, 
                                                       String userAgent) {
        log.info("Firmando contrato con ambas firmas. Token: {}", token);

        Contract contract = findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Contrato no encontrado"));

        if (!contract.canBeSigned()) {
            throw new IllegalStateException("El contrato no puede ser firmado. Estado: " + contract.getStatus());
        }

        // Subir firma del cliente
        String clientSignatureUrl = uploadBase64Image(clientSignatureBase64, "signature_client_" + contract.getId(), 
                FolderType.CONTRACT_SIGNATURES);

        // Subir firma del empleado
        String employeeSignatureUrl = uploadBase64Image(employeeSignatureBase64, "signature_employee_" + contract.getId(), 
                FolderType.CONTRACT_SIGNATURES);

        // Firmar contrato (cliente)
        contract.sign(clientSignatureUrl, ipAddress, userAgent);
        
        // Guardar firma y nombre del empleado
        contract.setEmployeeSignatureUrl(employeeSignatureUrl);
        contract.setEmployeeName(employeeName);
        
        contract = contractRepository.save(contract);

        log.info("✅ Contrato firmado con ambas firmas. ID: {}", contract.getId());

        // Generar PDF y guardarlo
        try {
            contract = generateAndSavePdf(contract);
            
            // Enviar contrato firmado por email (asíncrono)
            emailService.sendSignedContractEmail(contract);
            
        } catch (Exception e) {
            log.error("Error generando PDF o enviando email: {}", e.getMessage(), e);
            // No lanzamos excepción para no bloquear la firma
        }

        return contract;
    }

    /**
     * Actualiza información del documento con fotos de licencia y documento de identidad
     */
    @Transactional
    public Contract updateDocumentInfoWithPhotos(String token,
                                                  DocumentType documentType,
                                                  String documentNumber,
                                                  String documentFrontBase64,
                                                  String documentBackBase64,
                                                  String licenseFrontBase64,
                                                  String licenseBackBase64) {
        Contract contract = findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Contrato no encontrado"));

        if (!contract.canBeAccessed()) {
            throw new IllegalStateException("El contrato no puede ser modificado");
        }

        // Subir foto documento frente
        String docFrontUrl = null;
        if (documentFrontBase64 != null && !documentFrontBase64.isEmpty()) {
            docFrontUrl = uploadBase64Image(documentFrontBase64, "doc_front_" + contract.getId(), 
                    FolderType.CONTRACT_DOCUMENTS);
        }

        // Subir foto documento reverso
        String docBackUrl = null;
        if (documentBackBase64 != null && !documentBackBase64.isEmpty()) {
            docBackUrl = uploadBase64Image(documentBackBase64, "doc_back_" + contract.getId(), 
                    FolderType.CONTRACT_DOCUMENTS);
        }

        // Subir foto licencia frente
        String licenseFrontUrl = null;
        if (licenseFrontBase64 != null && !licenseFrontBase64.isEmpty()) {
            licenseFrontUrl = uploadBase64Image(licenseFrontBase64, "license_front_" + contract.getId(), 
                    FolderType.CONTRACT_DOCUMENTS);
        }

        // Subir foto licencia reverso
        String licenseBackUrl = null;
        if (licenseBackBase64 != null && !licenseBackBase64.isEmpty()) {
            licenseBackUrl = uploadBase64Image(licenseBackBase64, "license_back_" + contract.getId(), 
                    FolderType.CONTRACT_DOCUMENTS);
        }

        // Actualizar contrato
        contract.setDocumentType(documentType);
        contract.setDocumentNumber(documentNumber);
        contract.setDocumentFrontUrl(docFrontUrl);
        contract.setDocumentBackUrl(docBackUrl);
        contract.setLicenseFrontUrl(licenseFrontUrl);
        contract.setLicenseBackUrl(licenseBackUrl);

        return contractRepository.save(contract);
    }

    // ========================================
    // Subir video del vehículo
    // ========================================

    /**
     * Sube el video del estado del vehículo (entrega)
     */
    @Transactional
    public Contract uploadVehicleVideo(String token, InputStream videoStream, String fileName, String contentType) {
        Contract contract = findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Contrato no encontrado"));

        if (!contract.canBeAccessed()) {
            throw new IllegalStateException("El contrato no puede ser modificado");
        }

        log.info("📹 Subiendo video del vehículo para contrato ID: {}", contract.getId());

        try {
            String videoUrl = fileStorageService.uploadFile(
                    videoStream,
                    "video_entrega_" + contract.getId() + "_" + fileName,
                    contentType,
                    FolderType.CONTRACT_VIDEOS,
                    null
            );

            contract.setVehicleVideoUrl(videoUrl);
            contract = contractRepository.save(contract);

            log.info("✅ Video subido exitosamente: {}", videoUrl);
            return contract;

        } catch (Exception e) {
            log.error("❌ Error subiendo video: {}", e.getMessage(), e);
            throw new RuntimeException("Error al subir video: " + e.getMessage(), e);
        }
    }

    /**
     * Sube el video del estado del vehículo (devolución)
     */
    @Transactional
    public Contract uploadReturnVideo(Long contractId, InputStream videoStream, String fileName, String contentType) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new IllegalArgumentException("Contrato no encontrado"));

        log.info("📹 Subiendo video de devolución para contrato ID: {}", contract.getId());

        try {
            String videoUrl = fileStorageService.uploadFile(
                    videoStream,
                    "video_devolucion_" + contract.getId() + "_" + fileName,
                    contentType,
                    FolderType.CONTRACT_VIDEOS,
                    null
            );

            contract.setVehicleReturnVideoUrl(videoUrl);
            contract = contractRepository.save(contract);

            log.info("✅ Video de devolución subido exitosamente: {}", videoUrl);
            return contract;

        } catch (Exception e) {
            log.error("❌ Error subiendo video de devolución: {}", e.getMessage(), e);
            throw new RuntimeException("Error al subir video: " + e.getMessage(), e);
        }
    }

    // ========================================
    // DTOs internos
    // ========================================

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ContractAccessoryDTO {
        private Long id;
        private String accessoryName;
        private Boolean isPresent;
        private String observations;
    }

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ContractDamageMarkDTO {
        private java.math.BigDecimal positionX;
        private java.math.BigDecimal positionY;
        private DamageType damageType;
        private String description;
        private Integer severity;
    }
}
