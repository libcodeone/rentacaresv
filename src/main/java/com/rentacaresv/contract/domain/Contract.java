package com.rentacaresv.contract.domain;

import com.rentacaresv.rental.domain.Rental;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Entidad Contract (Domain Layer)
 * Representa el contrato digital de alquiler de vehículo.
 * 
 * Este contrato se envía al cliente vía link público para que:
 * - Suba foto de su documento de identidad
 * - Revise y confirme el checklist de accesorios
 * - Marque daños existentes en el diagrama del vehículo
 * - Firme digitalmente el contrato
 */
@Entity
@Table(name = "contract")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"rental", "accessories", "damageMarks"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Contract {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    /**
     * Token único para acceso público al contrato (URL pública)
     */
    @Column(name = "token", unique = true, nullable = false, length = 36)
    private String token;

    /**
     * Relación con la renta asociada
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rental_id", nullable = false, unique = true)
    private Rental rental;

    /**
     * Estado del contrato
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private ContractStatus status = ContractStatus.PENDING;

    // ========================================
    // Información del documento de identidad
    // ========================================

    /**
     * Tipo de documento presentado
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "document_type")
    private DocumentType documentType;

    /**
     * Número del documento
     */
    @Column(name = "document_number", length = 50)
    private String documentNumber;

    /**
     * URL de la foto del documento de identidad (frente)
     */
    @Column(name = "document_front_url", length = 500)
    private String documentFrontUrl;

    /**
     * URL de la foto del documento de identidad (reverso)
     */
    @Column(name = "document_back_url", length = 500)
    private String documentBackUrl;

    /**
     * URL de la foto de licencia de conducir (frente)
     */
    @Column(name = "license_front_url", length = 500)
    private String licenseFrontUrl;

    /**
     * URL de la foto de licencia de conducir (reverso)
     */
    @Column(name = "license_back_url", length = 500)
    private String licenseBackUrl;

    // ========================================
    // Información de la firma
    // ========================================

    /**
     * URL de la imagen de la firma digital del cliente
     */
    @Column(name = "signature_url", length = 500)
    private String signatureUrl;

    /**
     * URL de la imagen de la firma del empleado que entrega
     */
    @Column(name = "employee_signature_url", length = 500)
    private String employeeSignatureUrl;

    /**
     * Nombre del empleado que entrega el vehículo
     */
    @Column(name = "employee_name", length = 200)
    private String employeeName;

    /**
     * Fecha y hora de la firma
     */
    @Column(name = "signed_at")
    private LocalDateTime signedAt;

    /**
     * IP desde donde se firmó
     */
    @Column(name = "signed_from_ip", length = 45)
    private String signedFromIp;

    /**
     * User-Agent del navegador usado para firmar
     */
    @Column(name = "signed_user_agent", length = 500)
    private String signedUserAgent;

    // ========================================
    // Diagrama del vehículo
    // ========================================

    /**
     * URL del diagrama del vehículo con las marcas de daños (LEGACY - ya no se usa)
     */
    @Column(name = "vehicle_diagram_url", length = 500)
    private String vehicleDiagramUrl;

    /**
     * URL del video del EXTERIOR del vehículo al momento de la entrega
     */
    @Column(name = "vehicle_exterior_video_url", length = 500)
    private String vehicleExteriorVideoUrl;

    /**
     * URL del video del INTERIOR del vehículo al momento de la entrega
     */
    @Column(name = "vehicle_interior_video_url", length = 500)
    private String vehicleInteriorVideoUrl;

    /**
     * URL del video de OTROS DETALLES del vehículo al momento de la entrega
     */
    @Column(name = "vehicle_details_video_url", length = 500)
    private String vehicleDetailsVideoUrl;

    /**
     * URL del video del estado del vehículo al momento de la devolución
     */
    @Column(name = "vehicle_return_video_url", length = 500)
    private String vehicleReturnVideoUrl;

    // LEGACY - campo antiguo, mantener para compatibilidad
    @Column(name = "vehicle_video_url", length = 500)
    private String vehicleVideoUrl;

    // ========================================
    // Información del vehículo al momento del contrato
    // ========================================

    /**
     * Kilometraje de salida
     */
    @Column(name = "mileage_out")
    private Integer mileageOut;

    /**
     * Kilometraje de entrada (al devolver)
     */
    @Column(name = "mileage_in")
    private Integer mileageIn;

    /**
     * Nivel de combustible de salida (0-100)
     */
    @Column(name = "fuel_level_out")
    private Integer fuelLevelOut;

    /**
     * Nivel de combustible de entrada (0-100)
     */
    @Column(name = "fuel_level_in")
    private Integer fuelLevelIn;

    // ========================================
    // Montos y cargos adicionales
    // ========================================

    /**
     * Depósito de garantía
     */
    @Column(name = "deposit_amount", precision = 10, scale = 2)
    private BigDecimal depositAmount;

    /**
     * Deducible por accidente
     */
    @Column(name = "accident_deductible", precision = 10, scale = 2)
    private BigDecimal accidentDeductible;

    /**
     * Deducible por robo
     */
    @Column(name = "theft_deductible", precision = 10, scale = 2)
    private BigDecimal theftDeductible;

    // ========================================
    // Información adicional del contrato (formato papel)
    // ========================================

    /**
     * Lugar de entrega (ej: Aeropuerto)
     */
    @Column(name = "delivery_location", length = 100)
    private String deliveryLocation;

    /**
     * Dirección en El Salvador
     */
    @Column(name = "address_el_salvador", length = 255)
    private String addressElSalvador;

    /**
     * Dirección en el extranjero (USA u otro)
     */
    @Column(name = "address_foreign", length = 255)
    private String addressForeign;

    /**
     * Teléfono en USA
     */
    @Column(name = "phone_usa", length = 20)
    private String phoneUsa;

    /**
     * Teléfono familiar/emergencia
     */
    @Column(name = "phone_family", length = 20)
    private String phoneFamily;

    /**
     * Forma de pago seleccionada
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method")
    private PaymentMethod paymentMethod;

    /**
     * Nombre del conductor adicional
     */
    @Column(name = "additional_driver_name", length = 200)
    private String additionalDriverName;

    /**
     * Licencia del conductor adicional
     */
    @Column(name = "additional_driver_license", length = 50)
    private String additionalDriverLicense;

    /**
     * DUI del conductor adicional
     */
    @Column(name = "additional_driver_dui", length = 20)
    private String additionalDriverDui;

    /**
     * Hora de salida del vehículo
     */
    @Column(name = "departure_time")
    private LocalDateTime departureTime;

    /**
     * Hora de entrada del vehículo (devolución)
     */
    @Column(name = "return_time")
    private LocalDateTime returnTime;

    /**
     * Tipo de combustible marcado en contrato
     */
    @Column(name = "fuel_type_contract", length = 20)
    private String fuelTypeContract;

    // ========================================
    // PDF generado
    // ========================================

    /**
     * URL del PDF del contrato firmado
     */
    @Column(name = "pdf_url", length = 500)
    private String pdfUrl;

    // ========================================
    // Observaciones
    // ========================================

    /**
     * Observaciones generales del contrato
     */
    @Column(name = "observations", columnDefinition = "TEXT")
    private String observations;

    // ========================================
    // Relaciones con accesorios y daños
    // ========================================

    /**
     * Lista de accesorios revisados en el contrato
     */
    @OneToMany(mappedBy = "contract", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<ContractAccessory> accessories = new HashSet<>();

    /**
     * Lista de marcas de daños en el vehículo
     */
    @OneToMany(mappedBy = "contract", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<ContractDamageMark> damageMarks = new HashSet<>();

    // ========================================
    // Auditoría
    // ========================================

    /**
     * Fecha de expiración del link
     */
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ========================================
    // Métodos de Negocio (Domain Logic)
    // ========================================

    /**
     * Genera un nuevo token único para el contrato
     */
    @PrePersist
    public void generateToken() {
        if (this.token == null) {
            this.token = UUID.randomUUID().toString();
        }
        if (this.expiresAt == null) {
            // Por defecto expira en 7 días
            this.expiresAt = LocalDateTime.now().plusDays(7);
        }
    }

    /**
     * Verifica si el contrato ha expirado
     */
    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    /**
     * Verifica si el contrato puede ser accedido
     */
    public boolean canBeAccessed() {
        return status == ContractStatus.PENDING && !isExpired();
    }

    /**
     * Verifica si el contrato puede ser firmado
     */
    public boolean canBeSigned() {
        return status.canBeSigned() && !isExpired();
    }

    /**
     * Firma el contrato
     */
    public void sign(String signatureUrl, String ipAddress, String userAgent) {
        if (!canBeSigned()) {
            throw new IllegalStateException(
                "El contrato no puede ser firmado. Estado: " + status + ", Expirado: " + isExpired()
            );
        }

        this.signatureUrl = signatureUrl;
        this.signedAt = LocalDateTime.now();
        this.signedFromIp = ipAddress;
        this.signedUserAgent = userAgent;
        this.status = ContractStatus.SIGNED;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Cancela el contrato
     */
    public void cancel() {
        if (!status.canBeCancelled()) {
            throw new IllegalStateException(
                "El contrato no puede ser cancelado. Estado actual: " + status
            );
        }
        this.status = ContractStatus.CANCELLED;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Marca el contrato como expirado
     */
    public void markAsExpired() {
        if (status == ContractStatus.PENDING) {
            this.status = ContractStatus.EXPIRED;
            this.updatedAt = LocalDateTime.now();
        }
    }

    /**
     * Agrega un accesorio al contrato
     */
    public void addAccessory(ContractAccessory accessory) {
        accessories.add(accessory);
        accessory.setContract(this);
    }

    /**
     * Agrega una marca de daño al contrato
     */
    public void addDamageMark(ContractDamageMark damageMark) {
        damageMarks.add(damageMark);
        damageMark.setContract(this);
    }

    /**
     * Actualiza la información del documento
     */
    public void updateDocumentInfo(DocumentType type, String number, String frontUrl, String backUrl) {
        this.documentType = type;
        this.documentNumber = number;
        this.documentFrontUrl = frontUrl;
        this.documentBackUrl = backUrl;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Actualiza la información del vehículo (kilometraje y combustible)
     */
    public void updateVehicleInfo(Integer mileageOut, Integer fuelLevelOut) {
        this.mileageOut = mileageOut;
        this.fuelLevelOut = fuelLevelOut;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Registra la devolución del vehículo
     */
    public void registerReturn(Integer mileageIn, Integer fuelLevelIn) {
        this.mileageIn = mileageIn;
        this.fuelLevelIn = fuelLevelIn;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Hook de JPA para actualizar updatedAt
     */
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
