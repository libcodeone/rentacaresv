package com.rentacaresv.rental.application;

import com.rentacaresv.customer.domain.Customer;
import com.rentacaresv.customer.domain.CustomerCategory;
import com.rentacaresv.customer.domain.DocumentType;
import com.rentacaresv.customer.infrastructure.CustomerRepository;
import com.rentacaresv.rental.domain.Rental;
import com.rentacaresv.rental.domain.RentalPriceCalculator;
import com.rentacaresv.rental.domain.RentalStatus;
import com.rentacaresv.rental.infrastructure.RentalRepository;
import com.rentacaresv.rental.domain.photo.RentalPhoto;
import com.rentacaresv.rental.domain.photo.RentalPhotoType;
import com.rentacaresv.rental.infrastructure.RentalPhotoRepository;
import com.rentacaresv.settings.application.DynamicMailService;
import com.rentacaresv.settings.application.SettingsCache;
import com.rentacaresv.shared.storage.FileStorageService;
import com.rentacaresv.shared.storage.FolderType;
import com.rentacaresv.shared.storage.StorageInitializer;
import com.rentacaresv.vehicle.domain.Vehicle;
import com.rentacaresv.vehicle.infrastructure.VehicleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Servicio para procesar reservas públicas desde la web.
 * Crea el cliente (o lo reutiliza), crea la renta con estado PENDING y origen WEB,
 * y notifica al administrador por email.
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class PublicReservationService {

    private final CustomerRepository customerRepository;
    private final VehicleRepository vehicleRepository;
    private final RentalRepository rentalRepository;
    private final RentalPhotoRepository rentalPhotoRepository;
    private final DynamicMailService mailService;
    private final SettingsCache settingsCache;
    private final FileStorageService fileStorageService;
    private final StorageInitializer storageInitializer;

    private final ReservationConfirmationPdfGenerator confirmationPdfGenerator;

    private final RentalPriceCalculator priceCalculator = new RentalPriceCalculator();

    // Rate limiting simple por IP
    private final Map<String, RateLimitEntry> rateLimits = new ConcurrentHashMap<>();
    private static final int MAX_REQUESTS_PER_HOUR = 5;
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/jpg", "image/png", "image/webp"
    );
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

    /**
     * Procesa una reserva pública.
     *
     * @param dto    Datos del formulario
     * @param ipAddress IP del cliente (para rate limiting y auditoría)
     * @return Número de contrato generado
     */
    public String processReservation(
            PublicReservationDTO dto,
            String ipAddress,
            MultipartFile documentFront,
            MultipartFile documentBack,
            MultipartFile licenseFront,
            MultipartFile licenseBack) {
        log.info("Procesando reserva web desde IP: {}", ipAddress);

        // 1. Honeypot check
        if (dto.getWebsite() != null && !dto.getWebsite().isBlank()) {
            log.warn("Honeypot activado desde IP: {}", ipAddress);
            throw new IllegalArgumentException("Solicitud inválida");
        }

        // 2. Rate limiting
        checkRateLimit(ipAddress);

        // 3. Validar fechas
        validateDates(dto.getStartDate(), dto.getEndDate());

        // 4. Validar vehículo
        Vehicle vehicle = vehicleRepository.findById(dto.getVehicleId())
                .filter(v -> v.getDeletedAt() == null && Boolean.TRUE.equals(v.getPublishedOnWeb()))
                .orElseThrow(() -> new IllegalArgumentException("Vehículo no disponible"));

        // 5. Validar que no haya conflictos de fechas
        if (rentalRepository.hasConflictingRentals(dto.getVehicleId(), dto.getStartDate(), dto.getEndDate())) {
            throw new IllegalStateException(
                    "El vehículo ya tiene una reserva que se cruza con las fechas seleccionadas. " +
                    "Si necesitas fechas no continuas, realiza dos reservas separadas.");
        }

        // 6. Validar licencia no vencida
        if (dto.getDriverLicenseExpiry().isBefore(dto.getEndDate())) {
            throw new IllegalArgumentException("La licencia de conducir vence antes de la fecha de devolución del vehículo.");
        }

        // 7. Validar documentos requeridos antes de crear la renta
        validateDocumentFiles(documentFront, documentBack, licenseFront, licenseBack);

        // 8. Crear o reutilizar cliente
        Customer customer = findOrCreateCustomer(dto);

        // 9. Calcular precio base
        int days = priceCalculator.calculateDays(dto.getStartDate(), dto.getEndDate());
        BigDecimal dailyRate = priceCalculator.selectDailyRate(vehicle, customer, days);
        BigDecimal totalAmount = priceCalculator.calculateTotalPrice(vehicle, customer, dto.getStartDate(), dto.getEndDate());

        // Cargo adicional por salida del país
        BigDecimal cargoSacarPais = BigDecimal.ZERO;
        if (dto.isSacarPais() && dto.getDiasFueraPais() > 0) {
            BigDecimal tarifaSacarPais = settingsCache.getSettings().getTarifaSacarPais();
            if (tarifaSacarPais != null && tarifaSacarPais.compareTo(BigDecimal.ZERO) > 0) {
                cargoSacarPais = tarifaSacarPais.multiply(BigDecimal.valueOf(dto.getDiasFueraPais()));
                totalAmount = totalAmount.add(cargoSacarPais);
            }
        }

        // 10. Generar número de contrato
        String contractNumber = generateContractNumber();

        // 11. Crear renta
        Rental rental = Rental.builder()
                .contractNumber(contractNumber)
                .vehicle(vehicle)
                .customer(customer)
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .dailyRate(dailyRate)
                .totalDays(days)
                .totalAmount(totalAmount)
                .amountPaid(BigDecimal.ZERO)
                .status(RentalStatus.PENDING)
                .source("WEB")
                .flightNumber(dto.getFlightNumber())
                .accommodation(dto.getAccommodation())
                .contactPhone(dto.getContactPhone())
                .sacarPais(dto.isSacarPais())
                .destinosFueraPais(dto.getDestinosFueraPais())
                .diasFueraPais(dto.getDiasFueraPais() > 0 ? dto.getDiasFueraPais() : null)
                .cargoSacarPais(cargoSacarPais.compareTo(BigDecimal.ZERO) > 0 ? cargoSacarPais : null)
                .notes(buildNotesFromReservation(dto))
                .build();

        rental = rentalRepository.save(rental);

        log.info("Reserva web creada: {} - {} - {} días - ${}", contractNumber,
                vehicle.getFullDescription(), days, totalAmount);

        // 12. Subir y guardar documentos anexos solo al confirmar la reserva
        Map<RentalPhotoType, String> documentUrls = processReservationDocuments(
                rental,
                documentFront,
                documentBack,
                licenseFront,
                licenseBack
        );

        // 13. Enviar email de notificación al admin
        sendAdminNotification(rental, dto, documentUrls);

        // 14. Enviar email de confirmación al cliente con PDF adjunto
        sendClientConfirmation(rental);

        return contractNumber;
    }

    private Map<RentalPhotoType, String> processReservationDocuments(
            Rental rental,
            MultipartFile documentFront,
            MultipartFile documentBack,
            MultipartFile licenseFront,
            MultipartFile licenseBack) {
        storageInitializer.initializeIfNeeded();

        if (!fileStorageService.isInitialized()) {
            throw new IllegalStateException("El almacenamiento de documentos no está disponible");
        }

        Map<RentalPhotoType, String> documentUrls = new EnumMap<>(RentalPhotoType.class);
        List<String> uploadedUrls = new ArrayList<>();

        try {
            saveDocument(rental, documentFront, RentalPhotoType.DOCUMENT_ID_FRONT, 0, documentUrls, uploadedUrls);
            saveDocument(rental, documentBack, RentalPhotoType.DOCUMENT_ID_BACK, 1, documentUrls, uploadedUrls);
            saveDocument(rental, licenseFront, RentalPhotoType.DOCUMENT_LICENSE_FRONT, 2, documentUrls, uploadedUrls);
            saveDocument(rental, licenseBack, RentalPhotoType.DOCUMENT_LICENSE_BACK, 3, documentUrls, uploadedUrls);
            return documentUrls;
        } catch (Exception e) {
            cleanupUploadedFiles(uploadedUrls);
            throw e;
        }
    }

    private void validateDocumentFiles(
            MultipartFile documentFront,
            MultipartFile documentBack,
            MultipartFile licenseFront,
            MultipartFile licenseBack) {
        validateDocumentFile(documentFront, "Documento de identidad (frente)");
        validateDocumentFile(documentBack, "Documento de identidad (reverso)");
        validateDocumentFile(licenseFront, "Licencia (frente)");
        validateDocumentFile(licenseBack, "Licencia (reverso)");
    }

    private void validateDocumentFile(MultipartFile file, String label) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException(label + " es obligatorio");
        }

        String contentType = Optional.ofNullable(file.getContentType()).orElse("").toLowerCase();
        if (!ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new IllegalArgumentException(label + " debe ser una imagen JPG, PNG o WebP");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException(label + " no debe exceder 10MB");
        }
    }

    private void saveDocument(
            Rental rental,
            MultipartFile file,
            RentalPhotoType type,
            int displayOrder,
            Map<RentalPhotoType, String> documentUrls,
            List<String> uploadedUrls) {
        try {
            String fileName = Optional.ofNullable(file.getOriginalFilename())
                    .filter(name -> !name.isBlank())
                    .orElse(type.name().toLowerCase() + ".jpg");

            String url = fileStorageService.uploadFile(
                    file.getInputStream(),
                    fileName,
                    file.getContentType(),
                    FolderType.CONTRACT_DOCUMENTS,
                    rental.getContractNumber()
            );

            uploadedUrls.add(url);

            RentalPhoto photo = RentalPhoto.builder()
                    .rental(rental)
                    .photoUrl(url)
                    .photoType(type)
                    .description(type.getLabel())
                    .displayOrder(displayOrder)
                    .build();

            rentalPhotoRepository.save(photo);
            documentUrls.put(type, url);
        } catch (Exception e) {
            log.error("Error guardando documento {} para reserva {}: {}",
                    type, rental.getContractNumber(), e.getMessage(), e);
            throw new RuntimeException("Error guardando documentos de la reserva", e);
        }
    }

    private void cleanupUploadedFiles(List<String> uploadedUrls) {
        for (String uploadedUrl : uploadedUrls) {
            try {
                fileStorageService.deleteFile(uploadedUrl);
            } catch (Exception cleanupError) {
                log.warn("No se pudo revertir el archivo {}: {}", uploadedUrl, cleanupError.getMessage());
            }
        }
    }

    private void validateDates(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Las fechas son obligatorias");
        }
        if (!startDate.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("La fecha de inicio debe ser futura");
        }
        if (!endDate.isAfter(startDate)) {
            throw new IllegalArgumentException("La fecha de fin debe ser posterior a la de inicio");
        }
    }

    private Customer findOrCreateCustomer(PublicReservationDTO dto) {
        // Buscar por número de documento
        return customerRepository.findByDocumentNumber(dto.getDocumentNumber().trim())
                .map(existing -> {
                    // Actualizar datos de contacto si cambiaron
                    existing.setEmail(dto.getEmail().trim());
                    existing.setPhone(dto.getPhone().trim());
                    existing.setDriverLicenseNumber(dto.getDriverLicenseNumber().trim());
                    if (dto.getDriverLicenseCountry() != null) {
                        existing.setDriverLicenseCountry(dto.getDriverLicenseCountry().trim());
                    }
                    existing.setDriverLicenseExpiry(dto.getDriverLicenseExpiry());
                    return customerRepository.save(existing);
                })
                .orElseGet(() -> {
                    Customer newCustomer = Customer.builder()
                            .fullName(sanitize(dto.getFullName()))
                            .documentType(DocumentType.valueOf(dto.getDocumentType()))
                            .documentNumber(sanitize(dto.getDocumentNumber()))
                            .email(sanitize(dto.getEmail()))
                            .phone(sanitize(dto.getPhone()))
                            .driverLicenseNumber(sanitize(dto.getDriverLicenseNumber()))
                            .driverLicenseCountry(dto.getDriverLicenseCountry() != null
                                    ? dto.getDriverLicenseCountry().trim() : "SLV")
                            .driverLicenseExpiry(dto.getDriverLicenseExpiry())
                            .category(CustomerCategory.NORMAL)
                            .active(true)
                            .build();
                    return customerRepository.save(newCustomer);
                });
    }

    private String buildNotesFromReservation(PublicReservationDTO dto) {
        StringBuilder notes = new StringBuilder("=== RESERVA WEB ===\n");

        notes.append("Documentos adjuntos: Sí\n");

        if (dto.getDeliveryLocation() != null && !dto.getDeliveryLocation().isBlank()) {
            notes.append("Lugar de entrega: ").append(dto.getDeliveryLocation()).append("\n");
        }
        if (dto.getPhoneFamily() != null && !dto.getPhoneFamily().isBlank()) {
            notes.append("Tel. emergencia: ").append(dto.getPhoneFamily()).append("\n");
        }
        if (dto.getAdditionalDriverName() != null && !dto.getAdditionalDriverName().isBlank()) {
            notes.append("Conductor adicional: ").append(dto.getAdditionalDriverName());
            if (dto.getAdditionalDriverLicense() != null) {
                notes.append(" | Lic: ").append(dto.getAdditionalDriverLicense());
            }
            if (dto.getAdditionalDriverDui() != null) {
                notes.append(" | DUI: ").append(dto.getAdditionalDriverDui());
            }
            notes.append("\n");
        }

        return notes.toString();
    }

    private String generateContractNumber() {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "WEB-" + date + "-";
        int sequence = 1;
        String contractNumber;
        do {
            contractNumber = prefix + String.format("%05d", sequence);
            sequence++;
        } while (rentalRepository.existsByContractNumber(contractNumber));
        return contractNumber;
    }

    private void sendClientConfirmation(Rental rental) {
        try {
            if (!mailService.isEmailEnabled()) return;

            String clientEmail = rental.getCustomer().getEmail();
            if (clientEmail == null || clientEmail.isBlank()) {
                log.warn("Cliente sin email — no se enviará confirmación para reserva {}", rental.getContractNumber());
                return;
            }

            String companyName = settingsCache.getSettings().getCompanyName();
            String vehicleDesc = rental.getVehicle().getFullDescription();
            String subject = "Confirmación de Reserva: " + vehicleDesc + " — " + rental.getContractNumber();

            String html = """
                <!DOCTYPE html>
                <html>
                <head><meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; color: #333; line-height: 1.6; margin: 0; padding: 0; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background: #003366; color: white; padding: 24px 20px; border-radius: 8px 8px 0 0; text-align: center; }
                    .header h1 { margin: 0 0 4px; font-size: 22px; }
                    .header p  { margin: 0; opacity: 0.85; font-size: 13px; }
                    .badge { display: inline-block; background: #ff9800; color: white; padding: 4px 14px;
                             border-radius: 12px; font-size: 12px; font-weight: bold; margin: 12px 0; }
                    .content { background: #f9f9f9; padding: 24px 20px; border-radius: 0 0 8px 8px; }
                    .section { background: white; border-radius: 6px; padding: 16px; margin-bottom: 14px;
                               border-left: 4px solid #003366; }
                    .section h3 { margin: 0 0 10px; color: #003366; font-size: 14px; text-transform: uppercase;
                                  letter-spacing: 0.5px; }
                    .field { margin: 5px 0; font-size: 13px; }
                    .field b { display: inline-block; min-width: 160px; color: #555; }
                    .steps { background: white; border-radius: 6px; padding: 16px; margin-bottom: 14px;
                             border-left: 4px solid #ff9800; }
                    .steps h3 { margin: 0 0 10px; color: #cc6600; font-size: 14px; text-transform: uppercase; }
                    .step { display: flex; gap: 10px; margin-bottom: 8px; font-size: 13px; }
                    .step-num { background: #003366; color: white; border-radius: 50%%;
                                width: 22px; height: 22px; display: flex; align-items: center;
                                justify-content: center; font-size: 11px; flex-shrink: 0; }
                    .disclaimer { font-size: 11px; color: #888; font-style: italic; margin-top: 6px; }
                    .footer { text-align: center; padding: 16px; font-size: 11px; color: #aaa; }
                </style>
                </head>
                <body>
                <div class="container">
                    <div class="header">
                        <h1>%s</h1>
                        <p>Confirmación de Reserva de Vehículo</p>
                    </div>
                    <div class="content">
                        <div style="text-align:center;">
                            <span class="badge">RESERVA PENDIENTE</span>
                        </div>
                        <p style="font-size:14px;">Hola <b>%s</b>, hemos recibido su reserva. A continuación encontrará el resumen:</p>

                        <div class="section">
                            <h3>Vehículo y Fechas</h3>
                            <div class="field"><b>Vehículo:</b> %s</div>
                            <div class="field"><b>Fecha de entrega:</b> %s</div>
                            <div class="field"><b>Fecha de devolución:</b> %s</div>
                            <div class="field"><b>Días de renta:</b> %d día(s)</div>
                            <div class="field"><b>Tarifa diaria:</b> $%s</div>
                            <div class="field"><b>Total estimado:</b> <strong>$%s</strong></div>
                        </div>

                        <div class="section">
                            <h3>Su Reserva</h3>
                            <div class="field"><b>Número de reserva:</b> %s</div>
                            <div class="field"><b>Estado:</b> Pendiente de entrega</div>
                        </div>

                        <div class="steps">
                            <h3>Próximos Pasos</h3>
                            <div class="step"><span class="step-num">1</span><span>Nos pondremos en contacto con usted para coordinar el lugar y hora de entrega.</span></div>
                            <div class="step"><span class="step-num">2</span><span>Presente su DUI o pasaporte y licencia de conducir originales al momento de la entrega.</span></div>
                            <div class="step"><span class="step-num">3</span><span>Se realizará una inspección del vehículo y firmará el contrato de arrendamiento en ese momento.</span></div>
                            <div class="step"><span class="step-num">4</span><span>Para cancelar o modificar su reserva, contáctenos con anticipación.</span></div>
                        </div>

                        <p class="disclaimer">* El monto final puede variar según accesorios, seguros o cargos adicionales acordados al momento de la entrega.
                        Este documento es una confirmación de reserva y no constituye un contrato de arrendamiento.</p>
                    </div>
                    <div class="footer">%s · Reserva generada automáticamente desde el sitio web</div>
                </div>
                </body>
                </html>
                """.formatted(
                    companyName != null ? companyName : "Nova Rentacar",
                    rental.getCustomer().getFullName(),
                    vehicleDesc,
                    rental.getStartDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                    rental.getEndDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                    rental.getTotalDays(),
                    rental.getDailyRate(),
                    rental.getTotalAmount(),
                    rental.getContractNumber(),
                    companyName != null ? companyName : "Nova Rentacar"
            );

            // Generar PDF de confirmación
            byte[] pdfBytes = confirmationPdfGenerator.generate(rental, companyName);
            String pdfName = "Reserva-" + rental.getContractNumber() + ".pdf";

            mailService.sendEmail(clientEmail, subject, html, pdfName, pdfBytes);
            log.info("Email de confirmación enviado al cliente {} para reserva {}",
                    clientEmail, rental.getContractNumber());

        } catch (Exception e) {
            log.error("Error enviando confirmación al cliente para reserva {}: {}",
                    rental.getContractNumber(), e.getMessage());
            // No lanzar excepción — la reserva ya se creó exitosamente
        }
    }

    private void sendAdminNotification(Rental rental, PublicReservationDTO dto, Map<RentalPhotoType, String> documentUrls) {
        try {
            if (!mailService.isEmailEnabled()) {
                log.warn("Email deshabilitado, no se envió notificación de reserva web");
                return;
            }

            String adminEmail = settingsCache.getSettings().getMailFrom();
            if (adminEmail == null || adminEmail.isBlank()) {
                adminEmail = settingsCache.getSettings().getMailUsername();
            }
            if (adminEmail == null || adminEmail.isBlank()) {
                log.warn("No hay email de admin configurado para notificaciones");
                return;
            }

            String vehicleDesc = rental.getVehicle().getFullDescription();
            String subject = "Nueva Reserva Web: " + vehicleDesc + " - " + rental.getContractNumber();

            String html = """
                <!DOCTYPE html>
                <html>
                <head><meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; color: #333; line-height: 1.6; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background: #003366; color: white; padding: 20px; text-align: center; border-radius: 8px 8px 0 0; }
                    .badge { display: inline-block; background: #ff9800; color: white; padding: 4px 12px; border-radius: 12px; font-size: 12px; font-weight: bold; }
                    .content { padding: 20px; background: #f9f9f9; border-radius: 0 0 8px 8px; }
                    .section { margin-bottom: 15px; }
                    .section h3 { margin: 0 0 8px; color: #003366; font-size: 14px; }
                    .field { margin: 4px 0; font-size: 13px; }
                    .field b { display: inline-block; min-width: 140px; }
                    .photos { margin-top: 10px; }
                    .photos a { display: inline-block; margin-right: 10px; color: #007bff; font-size: 12px; }
                    .footer { text-align: center; padding: 15px; font-size: 11px; color: #999; }
                </style>
                </head>
                <body>
                <div class="container">
                    <div class="header">
                        <h2 style="margin:0;">Nueva Reserva Web</h2>
                        <p style="margin:5px 0 0;opacity:0.8;">%s</p>
                    </div>
                    <div class="content">
                        <p><span class="badge">RESERVA WEB</span> Contrato: <b>%s</b></p>

                        <div class="section">
                            <h3>Cliente</h3>
                            <div class="field"><b>Nombre:</b> %s</div>
                            <div class="field"><b>Documento:</b> %s - %s</div>
                            <div class="field"><b>Email:</b> %s</div>
                            <div class="field"><b>Teléfono:</b> %s</div>
                            <div class="field"><b>Licencia:</b> %s (%s) - Vence: %s</div>
                        </div>

                        <div class="section">
                            <h3>Vehículo y Fechas</h3>
                            <div class="field"><b>Vehículo:</b> %s</div>
                            <div class="field"><b>Período:</b> %s al %s (%d días)</div>
                            <div class="field"><b>Tarifa diaria:</b> $%s</div>
                            <div class="field"><b>Total:</b> $%s</div>
                        </div>

                        <div class="section">
                            <h3>Documentos adjuntos</h3>
                            <div class="photos">
                                <a href="%s" target="_blank">Doc. Frente</a>
                                <a href="%s" target="_blank">Doc. Reverso</a>
                                <a href="%s" target="_blank">Lic. Frente</a>
                                <a href="%s" target="_blank">Lic. Reverso</a>
                            </div>
                        </div>

                        %s
                    </div>
                    <div class="footer">
                        Reserva generada automáticamente desde novarentacarsv.com
                    </div>
                </div>
                </body>
                </html>
                """.formatted(
                    vehicleDesc,
                    rental.getContractNumber(),
                    dto.getFullName(),
                    dto.getDocumentType(), dto.getDocumentNumber(),
                    dto.getEmail(),
                    dto.getPhone(),
                    dto.getDriverLicenseNumber(),
                    dto.getDriverLicenseCountry() != null ? dto.getDriverLicenseCountry() : "SLV",
                    dto.getDriverLicenseExpiry().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                    vehicleDesc,
                    rental.getStartDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                    rental.getEndDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                    rental.getTotalDays(),
                    rental.getDailyRate(),
                    rental.getTotalAmount(),
                    documentUrls.get(RentalPhotoType.DOCUMENT_ID_FRONT),
                    documentUrls.get(RentalPhotoType.DOCUMENT_ID_BACK),
                    documentUrls.get(RentalPhotoType.DOCUMENT_LICENSE_FRONT),
                    documentUrls.get(RentalPhotoType.DOCUMENT_LICENSE_BACK),
                    buildOptionalInfoHtml(dto)
            );

            mailService.sendEmail(adminEmail, subject, html);
            log.info("Email de notificación enviado al admin para reserva {}", rental.getContractNumber());

        } catch (Exception e) {
            log.error("Error enviando email de notificación de reserva: {}", e.getMessage());
            // No lanzar excepción — la reserva ya se creó exitosamente
        }
    }

    private String buildOptionalInfoHtml(PublicReservationDTO dto) {
        StringBuilder sb = new StringBuilder();
        boolean hasOptional = false;

        if ((dto.getFlightNumber() != null && !dto.getFlightNumber().isBlank()) ||
            (dto.getAccommodation() != null && !dto.getAccommodation().isBlank()) ||
            (dto.getDeliveryLocation() != null && !dto.getDeliveryLocation().isBlank()) ||
            (dto.getPhoneFamily() != null && !dto.getPhoneFamily().isBlank()) ||
            (dto.getAdditionalDriverName() != null && !dto.getAdditionalDriverName().isBlank())) {
            hasOptional = true;
        }

        if (!hasOptional) return "";

        sb.append("<div class=\"section\"><h3>Información adicional</h3>");
        if (dto.getDeliveryLocation() != null && !dto.getDeliveryLocation().isBlank())
            sb.append("<div class=\"field\"><b>Lugar de entrega:</b> ").append(dto.getDeliveryLocation()).append("</div>");
        if (dto.getFlightNumber() != null && !dto.getFlightNumber().isBlank())
            sb.append("<div class=\"field\"><b>Vuelo:</b> ").append(dto.getFlightNumber()).append("</div>");
        if (dto.getAccommodation() != null && !dto.getAccommodation().isBlank())
            sb.append("<div class=\"field\"><b>Hospedaje:</b> ").append(dto.getAccommodation()).append("</div>");
        if (dto.getPhoneFamily() != null && !dto.getPhoneFamily().isBlank())
            sb.append("<div class=\"field\"><b>Tel. emergencia:</b> ").append(dto.getPhoneFamily()).append("</div>");
        if (dto.getAdditionalDriverName() != null && !dto.getAdditionalDriverName().isBlank()) {
            sb.append("<div class=\"field\"><b>Conductor adicional:</b> ").append(dto.getAdditionalDriverName());
            if (dto.getAdditionalDriverLicense() != null)
                sb.append(" | Lic: ").append(dto.getAdditionalDriverLicense());
            if (dto.getAdditionalDriverDui() != null)
                sb.append(" | DUI: ").append(dto.getAdditionalDriverDui());
            sb.append("</div>");
        }
        sb.append("</div>");

        return sb.toString();
    }

    private void checkRateLimit(String ip) {
        rateLimits.entrySet().removeIf(entry ->
                System.currentTimeMillis() - entry.getValue().windowStart > 3600_000);

        RateLimitEntry entry = rateLimits.computeIfAbsent(ip,
                k -> new RateLimitEntry(System.currentTimeMillis(), new AtomicInteger(0)));

        if (entry.count.incrementAndGet() > MAX_REQUESTS_PER_HOUR) {
            log.warn("Rate limit excedido para IP: {}", ip);
            throw new IllegalStateException("Demasiadas solicitudes. Intente de nuevo más tarde.");
        }
    }

    private String sanitize(String input) {
        if (input == null) return null;
        return input.trim()
                .replaceAll("<", "&lt;")
                .replaceAll(">", "&gt;")
                .replaceAll("\"", "&quot;");
    }

    private record RateLimitEntry(long windowStart, AtomicInteger count) {}
}
