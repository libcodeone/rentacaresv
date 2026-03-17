package com.rentacaresv.contract.application;

import com.rentacaresv.contract.domain.Contract;
import com.rentacaresv.contract.infrastructure.ContractRepository;
import com.rentacaresv.settings.application.DynamicMailService;
import com.rentacaresv.settings.application.SettingsCache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

            // Generar PDF
            byte[] pdfBytes = pdfGenerator.generatePdf(fullContract);

            // Cuerpo del email en HTML
            String htmlContent = buildEmailHtml(fullContract);
            String subject = "Contrato de Alquiler - " + fullContract.getRental().getContractNumber();
            String filename = "Contrato_" + fullContract.getRental().getContractNumber() + ".pdf";

            // Enviar con adjunto
            mailService.sendEmail(customer.getEmail(), subject, htmlContent, filename, pdfBytes);

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
                        .success { background: #d4edda; border-left-color: #28a745; }
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
                            <div class="info-box success">
                                <h2>✅ ¡Contrato Firmado Exitosamente!</h2>
                                <p>Estimado/a <strong>%s</strong>,</p>
                                <p>Su contrato de alquiler ha sido firmado correctamente. Adjunto encontrará una copia del contrato en formato PDF.</p>
                            </div>

                            <div class="info-box">
                                <h2>📋 Detalles del Contrato</h2>
                                <p><strong>Número de Contrato:</strong> %s</p>
                                <p><strong>Vehículo:</strong> %s</p>
                                <p><strong>Placa:</strong> %s</p>
                                <p><strong>Período:</strong> %s al %s</p>
                                <p><strong>Total:</strong> $%s</p>
                            </div>

                            <div class="info-box">
                                <h2>📞 Contacto</h2>
                                <p>Si tiene alguna pregunta o necesita asistencia, no dude en contactarnos.</p>
                            </div>
                        </div>

                        <div class="footer">
                            <p>Este es un correo automático generado por el sistema de %s</p>
                            <p>Por favor no responda a este correo.</p>
                        </div>
                    </div>
                </body>
                </html>
                """
                .formatted(
                        companyName,
                        customer.getFullName(),
                        rental.getContractNumber(),
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
