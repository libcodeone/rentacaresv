package com.rentacaresv.contract.application;

import com.rentacaresv.contract.domain.Contract;
import com.rentacaresv.contract.infrastructure.ContractRepository;
import com.rentacaresv.settings.application.DynamicMailService;
import com.rentacaresv.settings.application.SettingsCache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;

/**
 * Servicio para envío de correos electrónicos relacionados con contratos.
 * Usa DynamicMailService para configuración desde base de datos.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ContractEmailService {

    private final DynamicMailService mailService;
    private final SettingsCache settingsCache;
    private final ContractPdfGenerator pdfGenerator;
    private final ContractRepository contractRepository;

    @Value("${app.base-url:http://localhost:8091}")
    private String baseUrl;

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /**
     * Envía el contrato firmado por correo al cliente.
     * Se ejecuta de forma asíncrona.
     */
    @Async
    @Transactional(readOnly = true)
    public void sendSignedContractEmail(Contract contract) {
        if (!mailService.isEmailEnabled()) {
            log.info("Envío de email deshabilitado. No se enviará contrato ID: {}", contract.getId());
            return;
        }

        try {
            // Recargar el contrato con todas las relaciones en esta nueva transacción
            Contract fullContract = loadContractWithRelations(contract.getId());

            if (fullContract == null) {
                log.error("No se pudo cargar el contrato ID: {}", contract.getId());
                return;
            }

            var customer = fullContract.getRental().getCustomer();

            if (customer.getEmail() == null || customer.getEmail().isEmpty()) {
                log.warn("Cliente sin email registrado. No se puede enviar contrato ID: {}", fullContract.getId());
                return;
            }

            log.info("Enviando contrato firmado por email a: {}", customer.getEmail());

            // Cuerpo del email en HTML
             String htmlContent = buildEmailHtml(fullContract);
             String subject = "Contrato de Alquiler - " + fullContract.getRental().getContractNumber();
 
             // Enviar sin adjunto para ahorrar memoria (Metaspace limitado)
             mailService.sendEmail(customer.getEmail(), subject, htmlContent);
 
             log.info("✅ Email de contrato enviado exitosamente a: {}", customer.getEmail());

        } catch (Exception e) {
            log.error("Error enviando contrato por email: {}", e.getMessage(), e);
        }
    }

    /**
     * Carga el contrato con todas sus relaciones para evitar
     * LazyInitializationException
     */
    private Contract loadContractWithRelations(Long contractId) {
        // Cargar contrato básico
        var contractOpt = contractRepository.findByIdBasic(contractId);
        if (contractOpt.isEmpty()) {
            return null;
        }

        Contract contract = contractOpt.get();

        // Cargar accesorios
        contractRepository.findByIdWithAccessories(contract.getId());

        return contract;
    }

    /**
     * Envía el link del contrato para firma.
     */
    @Async
    public void sendContractLinkEmail(Contract contract, String contractUrl) {
        if (!mailService.isEmailEnabled()) {
            log.info("Envío de email deshabilitado");
            return;
        }

        var customer = contract.getRental().getCustomer();

        if (customer.getEmail() == null || customer.getEmail().isEmpty()) {
            log.warn("Cliente sin email registrado");
            return;
        }

        log.info("Enviando link de contrato por email a: {}", customer.getEmail());

        try {
            String htmlContent = buildLinkEmailHtml(contract, contractUrl);
            String subject = "Firme su Contrato de Alquiler - " + settingsCache.getCompanyName();

            mailService.sendEmail(customer.getEmail(), subject, htmlContent);

            log.info("✅ Email con link enviado exitosamente a: {}", customer.getEmail());

        } catch (Exception e) {
            log.error("Error enviando email con link: {}", e.getMessage(), e);
        }
    }

    private String buildEmailHtml(Contract contract) {
        var rental = contract.getRental();
        var vehicle = rental.getVehicle();
        var customer = rental.getCustomer();
        String companyName = settingsCache.getCompanyName();

        // Usar la URL directa de Spaces si está disponible, si no el API
        String pdfUrl = (contract.getPdfUrl() != null && !contract.getPdfUrl().isBlank())
                ? contract.getPdfUrl()
                : baseUrl + "/api/contracts/" + contract.getId() + "/pdf";

        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <style>
                        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; margin: 0; padding: 0; }
                        .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                        .header { background: #003366; color: white; padding: 24px 20px; text-align: center; border-radius: 8px 8px 0 0; }
                        .header h1 { margin: 0; font-size: 22px; letter-spacing: 1px; }
                        .header p { margin: 6px 0 0; font-size: 14px; opacity: 0.85; }
                        .content { background: #f4f6f9; padding: 24px 20px; }
                        .info-box { background: white; padding: 16px; margin: 12px 0; border-radius: 8px; border-left: 4px solid #003366; }
                        .info-box.success { background: #f0faf4; border-left-color: #2e7d52; }
                        .info-box h2 { margin: 0 0 8px; font-size: 16px; color: #003366; }
                        .info-box.success h2 { color: #2e7d52; }
                        .info-box p { margin: 4px 0; font-size: 14px; }
                        .pdf-box { background: white; border-radius: 8px; padding: 28px 20px; text-align: center; margin: 12px 0; border: 1px solid #dde2ea; }
                        .pdf-icon-big { font-size: 52px; display: block; margin-bottom: 10px; }
                        .contract-num { font-size: 15px; font-weight: bold; color: #003366; margin: 0 0 18px; }
                        .btn-download {
                            display: inline-block;
                            background: #003366;
                            color: #ffffff !important;
                            text-decoration: none;
                            padding: 13px 32px;
                            border-radius: 6px;
                            font-size: 15px;
                            font-weight: bold;
                            letter-spacing: 0.5px;
                        }
                        .btn-download:hover { background: #00255a; }
                        .hint { font-size: 12px; color: #888; margin: 12px 0 0; }
                        .footer { text-align: center; padding: 20px; font-size: 12px; color: #999; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>%s</h1>
                            <p>Contrato de Alquiler de Vehículo</p>
                        </div>

                        <div class="content">
                            <div class="info-box success">
                                <h2>✅ ¡Contrato Firmado!</h2>
                                <p>Estimado/a <strong>%s</strong>, su contrato ha sido firmado correctamente. Puede descargarlo usando el botón a continuación.</p>
                            </div>

                            <div class="pdf-box">
                                <span class="pdf-icon-big">📄</span>
                                <p class="contract-num">Contrato %s</p>
                                <a href="%s" class="btn-download">⬇&nbsp;&nbsp;Descargar contrato PDF</a>
                                <p class="hint">Si el botón no funciona, copie y pegue el enlace en su navegador.</p>
                            </div>

                            <div class="info-box">
                                <h2>📋 Detalles</h2>
                                <p><strong>Vehículo:</strong> %s &nbsp;|&nbsp; <strong>Placa:</strong> %s</p>
                                <p><strong>Período:</strong> %s al %s</p>
                                <p><strong>Total:</strong> $%s</p>
                            </div>

                            <div class="info-box">
                                <h2>📞 Contacto</h2>
                                <p>Si tiene alguna pregunta, no dude en contactarnos.</p>
                            </div>
                        </div>

                        <div class="footer">
                            <p>Correo automático generado por %s · Por favor no responda a este mensaje.</p>
                        </div>
                    </div>
                </body>
                </html>
                """
                .formatted(
                        companyName,
                        customer.getFullName(),
                        rental.getContractNumber(),
                        pdfUrl,
                        vehicle.getBrand() + " " + vehicle.getModel() + " " + vehicle.getYear(),
                        vehicle.getLicensePlate(),
                        rental.getStartDate().format(DATE_FORMAT),
                        rental.getEndDate().format(DATE_FORMAT),
                        rental.getTotalAmount().toString(),
                        companyName);
    }

    private String buildLinkEmailHtml(Contract contract, String contractUrl) {
        var rental = contract.getRental();
        var vehicle = rental.getVehicle();
        var customer = rental.getCustomer();
        String companyName = settingsCache.getCompanyName();

        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <style>
                        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                        .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                        .header { background: #003366; color: white; padding: 20px; text-align: center; }
                        .content { padding: 20px; background: #f9f9f9; }
                        .info-box { background: white; padding: 15px; margin: 10px 0; border-radius: 8px; border-left: 4px solid #003366; }
                        .button { display: inline-block; background: #003366; color: white; padding: 15px 30px; text-decoration: none; border-radius: 5px; margin: 20px 0; }
                        .footer { text-align: center; padding: 20px; font-size: 12px; color: #666; }
                        h1 { margin: 0; font-size: 24px; }
                        h2 { color: #003366; font-size: 18px; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>%s</h1>
                            <p>Contrato de Alquiler de Vehículo</p>
                        </div>

                        <div class="content">
                            <p>Estimado/a <strong>%s</strong>,</p>

                            <p>Su contrato de alquiler está listo para ser firmado.</p>

                            <div class="info-box">
                                <h2>📋 Detalles de la Reserva</h2>
                                <p><strong>Vehículo:</strong> %s</p>
                                <p><strong>Período:</strong> %s al %s</p>
                                <p><strong>Total:</strong> $%s</p>
                            </div>

                            <div style="text-align: center;">
                                <a href="%s" class="button">Ver y Firmar Contrato</a>
                            </div>

                            <p style="font-size: 12px; color: #666;">
                                Si el botón no funciona, copie y pegue este enlace en su navegador:<br>
                                <a href="%s">%s</a>
                            </p>
                        </div>

                        <div class="footer">
                            <p>Este es un correo automático generado por el sistema de %s</p>
                        </div>
                    </div>
                </body>
                </html>
                """
                .formatted(
                        companyName,
                        customer.getFullName(),
                        vehicle.getBrand() + " " + vehicle.getModel() + " " + vehicle.getYear(),
                        rental.getStartDate().format(DATE_FORMAT),
                        rental.getEndDate().format(DATE_FORMAT),
                        rental.getTotalAmount().toString(),
                        contractUrl,
                        contractUrl,
                        contractUrl,
                        companyName);
    }
}
