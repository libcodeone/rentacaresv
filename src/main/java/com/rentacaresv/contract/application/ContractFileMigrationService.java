package com.rentacaresv.contract.application;

import com.rentacaresv.contract.domain.Contract;
import com.rentacaresv.contract.infrastructure.ContractRepository;
import com.rentacaresv.rental.domain.photo.RentalPhoto;
import com.rentacaresv.rental.domain.photo.RentalPhotoType;
import com.rentacaresv.rental.infrastructure.RentalPhotoRepository;
import com.rentacaresv.shared.storage.FileStorageService;
import com.rentacaresv.shared.storage.FolderType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Servicio de migración one-time para mover archivos existentes en DO Spaces
 * a la nueva estructura de subcarpetas por número de contrato.
 *
 * Antes: contracts/documents/{uuid}.jpg
 * Ahora: contracts/documents/ADM-20260410-00001/{uuid}.jpg
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ContractFileMigrationService {

    private final ContractRepository contractRepository;
    private final RentalPhotoRepository rentalPhotoRepository;
    private final FileStorageService fileStorageService;

    // Tipos de foto de documentos del cliente en RentalPhoto
    private static final Set<RentalPhotoType> DOCUMENT_PHOTO_TYPES = Set.of(
            RentalPhotoType.DOCUMENT_ID_FRONT,
            RentalPhotoType.DOCUMENT_ID_BACK,
            RentalPhotoType.DOCUMENT_LICENSE_FRONT,
            RentalPhotoType.DOCUMENT_LICENSE_BACK
    );

    @Transactional
    public MigrationResult migrateAll() {
        List<String> logs = new ArrayList<>();
        int moved = 0;
        int skipped = 0;
        int errors = 0;

        log.info("=== Iniciando migración de archivos de contratos ===");
        List<Contract> contracts = contractRepository.findAll();
        log.info("Contratos encontrados: {}", contracts.size());

        for (Contract contract : contracts) {
            String contractNumber = contract.getRental().getContractNumber();
            logs.add("── Contrato: " + contractNumber + " [" + contract.getStatus() + "]");

            // 1. Documentos del contrato (fotos de DUI y licencia)
            var docResult = migrateContractDocuments(contract, contractNumber);
            moved   += docResult.moved;
            skipped += docResult.skipped;
            errors  += docResult.errors;
            logs.addAll(docResult.logs);

            // 2. Firmas
            var sigResult = migrateSignatures(contract, contractNumber);
            moved   += sigResult.moved;
            skipped += sigResult.skipped;
            errors  += sigResult.errors;
            logs.addAll(sigResult.logs);

            // 3. Videos
            var vidResult = migrateVideos(contract, contractNumber);
            moved   += vidResult.moved;
            skipped += vidResult.skipped;
            errors  += vidResult.errors;
            logs.addAll(vidResult.logs);

            // 4. PDF
            var pdfResult = migratePdf(contract, contractNumber);
            moved   += pdfResult.moved;
            skipped += pdfResult.skipped;
            errors  += pdfResult.errors;
            logs.addAll(pdfResult.logs);

            // 5. RentalPhoto (documentos subidos en reserva WEB)
            var rentalPhotoResult = migrateRentalPhotos(contract, contractNumber);
            moved   += rentalPhotoResult.moved;
            skipped += rentalPhotoResult.skipped;
            errors  += rentalPhotoResult.errors;
            logs.addAll(rentalPhotoResult.logs);

            contractRepository.save(contract);
        }

        log.info("=== Migración completada: {} movidos, {} omitidos, {} errores ===", moved, skipped, errors);
        return new MigrationResult(moved, skipped, errors, logs);
    }

    // ── Documentos (DUI / Licencia) ──────────────────────────────────────────

    private PartialResult migrateContractDocuments(Contract contract, String contractNumber) {
        PartialResult r = new PartialResult();

        String newFront = moveFile(contract.getDocumentFrontUrl(), FolderType.CONTRACT_DOCUMENTS, contractNumber, "doc_front", r);
        String newBack  = moveFile(contract.getDocumentBackUrl(),  FolderType.CONTRACT_DOCUMENTS, contractNumber, "doc_back",  r);
        String newLicF  = moveFile(contract.getLicenseFrontUrl(),  FolderType.CONTRACT_DOCUMENTS, contractNumber, "lic_front", r);
        String newLicB  = moveFile(contract.getLicenseBackUrl(),   FolderType.CONTRACT_DOCUMENTS, contractNumber, "lic_back",  r);

        if (newFront != null) contract.setDocumentFrontUrl(newFront);
        if (newBack  != null) contract.setDocumentBackUrl(newBack);
        if (newLicF  != null) contract.setLicenseFrontUrl(newLicF);
        if (newLicB  != null) contract.setLicenseBackUrl(newLicB);

        return r;
    }

    // ── Firmas ───────────────────────────────────────────────────────────────

    private PartialResult migrateSignatures(Contract contract, String contractNumber) {
        PartialResult r = new PartialResult();

        String newClientSig   = moveFile(contract.getSignatureUrl(),         FolderType.CONTRACT_SIGNATURES, contractNumber, "sig_cliente",  r);
        String newEmployeeSig = moveFile(contract.getEmployeeSignatureUrl(), FolderType.CONTRACT_SIGNATURES, contractNumber, "sig_empleado", r);

        if (newClientSig   != null) contract.setSignatureUrl(newClientSig);
        if (newEmployeeSig != null) contract.setEmployeeSignatureUrl(newEmployeeSig);

        return r;
    }

    // ── Videos ───────────────────────────────────────────────────────────────

    private PartialResult migrateVideos(Contract contract, String contractNumber) {
        PartialResult r = new PartialResult();

        String newExt    = moveFile(contract.getVehicleExteriorVideoUrl(), FolderType.CONTRACT_VIDEOS, contractNumber, "video_exterior", r);
        String newInt    = moveFile(contract.getVehicleInteriorVideoUrl(), FolderType.CONTRACT_VIDEOS, contractNumber, "video_interior", r);
        String newDet    = moveFile(contract.getVehicleDetailsVideoUrl(),  FolderType.CONTRACT_VIDEOS, contractNumber, "video_detalles", r);
        String newReturn = moveFile(contract.getVehicleReturnVideoUrl(),   FolderType.CONTRACT_VIDEOS, contractNumber, "video_devolucion", r);
        String newLegacy = moveFile(contract.getVehicleVideoUrl(),         FolderType.CONTRACT_VIDEOS, contractNumber, "video_legacy", r);

        if (newExt    != null) contract.setVehicleExteriorVideoUrl(newExt);
        if (newInt    != null) contract.setVehicleInteriorVideoUrl(newInt);
        if (newDet    != null) contract.setVehicleDetailsVideoUrl(newDet);
        if (newReturn != null) contract.setVehicleReturnVideoUrl(newReturn);
        if (newLegacy != null) contract.setVehicleVideoUrl(newLegacy);

        return r;
    }

    // ── PDF ──────────────────────────────────────────────────────────────────

    private PartialResult migratePdf(Contract contract, String contractNumber) {
        PartialResult r = new PartialResult();

        String newPdf = moveFile(contract.getPdfUrl(), FolderType.CONTRACT_DOCUMENTS, contractNumber, "contrato_pdf", r);
        if (newPdf != null) contract.setPdfUrl(newPdf);

        return r;
    }

    // ── RentalPhoto (reservas WEB) ────────────────────────────────────────────

    private PartialResult migrateRentalPhotos(Contract contract, String contractNumber) {
        PartialResult r = new PartialResult();

        List<RentalPhoto> photos = rentalPhotoRepository.findByRentalId(contract.getRental().getId());

        for (RentalPhoto photo : photos) {
            if (!DOCUMENT_PHOTO_TYPES.contains(photo.getPhotoType())) continue;

            String newUrl = moveFile(photo.getPhotoUrl(), FolderType.CONTRACT_DOCUMENTS, contractNumber,
                    photo.getPhotoType().name().toLowerCase(), r);

            if (newUrl != null) {
                photo.setPhotoUrl(newUrl);
                rentalPhotoRepository.save(photo);
            }
        }

        return r;
    }

    // ── Lógica central de movimiento ─────────────────────────────────────────

    /**
     * Mueve un archivo a la subcarpeta correcta si no está ya en ella.
     * Devuelve la nueva URL si se movió, null si se omitió o hubo error.
     */
    private String moveFile(String currentUrl, FolderType folderType, String contractNumber,
                            String label, PartialResult result) {
        if (currentUrl == null || currentUrl.isBlank()) {
            return null;
        }

        // Ya está en la subcarpeta correcta → omitir
        if (currentUrl.contains("/" + contractNumber + "/")) {
            result.skipped++;
            result.logs.add("   ⏭ [OMITIDO] " + label + " ya está en subcarpeta");
            return null;
        }

        try {
            String newUrl = fileStorageService.moveFile(currentUrl, folderType, contractNumber);
            result.moved++;
            result.logs.add("   ✅ [MOVIDO] " + label);
            log.info("Movido [{}] → subcarpeta {}", label, contractNumber);
            return newUrl;
        } catch (Exception e) {
            result.errors++;
            result.logs.add("   ❌ [ERROR] " + label + ": " + e.getMessage());
            log.error("Error moviendo [{}]: {}", label, e.getMessage());
            return null;
        }
    }

    // ── DTOs de resultado ─────────────────────────────────────────────────────

    private static class PartialResult {
        int moved = 0;
        int skipped = 0;
        int errors = 0;
        List<String> logs = new ArrayList<>();
    }

    public record MigrationResult(int moved, int skipped, int errors, List<String> logs) {
        public boolean hasErrors() { return errors > 0; }
    }
}
