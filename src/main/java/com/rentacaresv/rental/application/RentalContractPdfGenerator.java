package com.rentacaresv.rental.application;

import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.kernel.pdf.canvas.draw.SolidLine;
import com.rentacaresv.customer.domain.Customer;
import com.rentacaresv.rental.domain.Rental;
import com.rentacaresv.vehicle.domain.Vehicle;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Generador de contratos de renta en PDF con lenguaje jurídico profesional
 */
@Service
@Slf4j
public class RentalContractPdfGenerator {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DeviceRgb PRIMARY_COLOR = new DeviceRgb(41, 98, 255);
    private static final DeviceRgb GRAY_COLOR = new DeviceRgb(100, 100, 100);

    /**
     * Genera el PDF del contrato de renta con lenguaje legal
     */
    public byte[] generateContract(Rental rental, String deliveryNotes, List<String> photoUrls) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc, PageSize.LETTER);
            document.setMargins(40, 40, 40, 40);

            // Header con logo y título
            addHeader(document, rental);

            // Separador visual
            addSeparator(document);

            // Sección 1: Identificación de las partes
            addPartiesSection(document, rental);

            // Sección 2: Objeto del contrato
            addVehicleSection(document, rental);

            // Sección 3: Condiciones económicas
            addFinancialSection(document, rental);

            // Sección 4: Estado del vehículo (si hay notas o fotos)
            if (deliveryNotes != null || (photoUrls != null && !photoUrls.isEmpty())) {
                addVehicleConditionSection(document, deliveryNotes, photoUrls);
            }

            // Sección 5: Términos y condiciones generales
            addTermsAndConditions(document);

            // Sección 6: Responsabilidades del arrendatario
            addTenantResponsibilities(document);

            // Sección 7: Causales de rescisión
            addTerminationClauses(document);

            // Sección 8: Firmas
            addSignatureSection(document, rental);

            document.close();

            log.info("Contrato PDF generado exitosamente para renta {}", rental.getContractNumber());
            return baos.toByteArray();

        } catch (Exception e) {
            log.error("Error generando PDF del contrato: {}", e.getMessage(), e);
            throw new RuntimeException("Error al generar contrato PDF", e);
        }
    }

    private void addHeader(Document document, Rental rental) {
        // Logo/Marca
        Paragraph logo = new Paragraph("🚗 RENTACAR ESV")
                .setFontSize(20)
                .setBold()
                .setFontColor(PRIMARY_COLOR)
                .setTextAlignment(TextAlignment.CENTER);
        document.add(logo);

        // Título principal
        Paragraph title = new Paragraph("CONTRATO DE ARRENDAMIENTO DE VEHÍCULO AUTOMOTOR")
                .setFontSize(16)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(10);
        document.add(title);

        // Info del contrato
        Paragraph contractInfo = new Paragraph()
                .setFontSize(10)
                .setTextAlignment(TextAlignment.CENTER)
                .setFontColor(GRAY_COLOR)
                .add("Contrato No. " + rental.getContractNumber() + " | ")
                .add("Fecha de emisión: " + LocalDate.now().format(DATE_FORMATTER));
        document.add(contractInfo);
    }

    private void addSeparator(Document document) {
        SolidLine line = new SolidLine(2);
        line.setColor(PRIMARY_COLOR);
        document.add(new LineSeparator(line)
                .setMarginTop(15)
                .setMarginBottom(15));
    }

    private void addPartiesSection(Document document, Rental rental) {

        Customer customer = rental.getCustomer();

        Paragraph sectionTitle = new Paragraph("I. IDENTIFICACIÓN DE LAS PARTES")
                .setFontSize(12)
                .setBold()
                .setFontColor(PRIMARY_COLOR)
                .setMarginTop(10);
        document.add(sectionTitle);

        // El Arrendador
        Paragraph landlord = new Paragraph()
                .setFontSize(10)
                .add(new Text("EL ARRENDADOR: ").setBold())
                .add("RentaCar ESV, sociedad debidamente constituida conforme a las leyes de la República de El Salvador, "
                        +
                        "con domicilio en [DIRECCIÓN COMPLETA], representada en este acto por [NOMBRE DEL REPRESENTANTE LEGAL], "
                        +
                        "mayor de edad, [PROFESIÓN], del domicilio de [CIUDAD], portador del Documento Único de Identidad número "
                        +
                        "[DUI], quien actúa en calidad de Representante Legal, según consta en escritura pública de fecha [FECHA].");
        document.add(landlord);

        // El Arrendatario
        String licenseInfo = customer.getDriverLicenseNumber() != null 
            ? customer.getDriverLicenseNumber() 
            : "[LICENCIA]";
        
        String licenseCountry = customer.getDriverLicenseCountry() != null 
            ? " (" + customer.getDriverLicenseCountry() + ")" 
            : "";
        
        Paragraph tenant = new Paragraph()
                .setFontSize(10)
                .setMarginTop(10)
                .add(new Text("EL ARRENDATARIO: ").setBold())
                .add(customer.getFullName() + ", ")
                .add("mayor de edad, ")
                .add("del domicilio de " + (customer.getAddress() != null ? customer.getAddress() : "[CIUDAD]") + ", ")
                .add("portador del Documento Único de Identidad número " + customer.getDocumentNumber() + ", ")
                .add("con Licencia de Conducir número " + licenseInfo + licenseCountry + ", ")
                .add("domiciliado en " + (customer.getAddress() != null ? customer.getAddress() : "[DIRECCIÓN]") + ", ")
                .add("teléfono " + (customer.getPhone() != null ? customer.getPhone() : "[TELÉFONO]") + ", ")
                .add("correo electrónico " + (customer.getEmail() != null ? customer.getEmail() : "[EMAIL]") + ".");
        document.add(tenant);

        // Declaración
        Paragraph declaration = new Paragraph()
                .setFontSize(10)
                .setMarginTop(10)
                .add("Ambas partes, actuando en el ejercicio de sus propios derechos y bajo su propia representación, "
                        +
                        "declaran tener la capacidad legal necesaria para celebrar el presente contrato y ");
        document.add(declaration);

        Paragraph exponen = new Paragraph("EXPONEN:")
                .setFontSize(10)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(10);
        document.add(exponen);
    }

    private void addVehicleSection(Document document, Rental rental) {
        Vehicle vehicle = rental.getVehicle();

        Paragraph sectionTitle = new Paragraph("II. OBJETO DEL CONTRATO")
                .setFontSize(12)
                .setBold()
                .setFontColor(PRIMARY_COLOR)
                .setMarginTop(15);
        document.add(sectionTitle);

        Paragraph intro = new Paragraph()
                .setFontSize(10)
                .add("Que por medio del presente instrumento, EL ARRENDADOR concede en arrendamiento a " +
                        "EL ARRENDATARIO, y éste recibe a su entera satisfacción, el vehículo automotor que a " +
                        "continuación se describe:");
        document.add(intro);

        // Tabla del vehículo
        Table vehicleTable = new Table(new float[] { 1, 2 });
        vehicleTable.setWidth(UnitValue.createPercentValue(100));
        vehicleTable.setMarginTop(10);

        addTableRow(vehicleTable, "Marca:", vehicle.getBrand());
        addTableRow(vehicleTable, "Modelo:", vehicle.getModel());
        addTableRow(vehicleTable, "Año:", String.valueOf(vehicle.getYear()));
        addTableRow(vehicleTable, "Placa:", vehicle.getLicensePlate());
        addTableRow(vehicleTable, "Color:", vehicle.getColor());
        addTableRow(vehicleTable, "Transmisión:", vehicle.getTransmissionType().name());
        addTableRow(vehicleTable, "Combustible:", vehicle.getFuelType().name());
        addTableRow(vehicleTable, "Capacidad:", vehicle.getPassengerCapacity() + " pasajeros");

        document.add(vehicleTable);

        Paragraph clause = new Paragraph()
                .setFontSize(10)
                .setMarginTop(10)
                .add("El vehículo descrito se entrega en perfecto estado de funcionamiento mecánico y estético, " +
                        "comprometiéndose EL ARRENDATARIO a devolverlo en las mismas condiciones en que lo recibe, " +
                        "salvo el desgaste natural propio del uso ordinario.");
        document.add(clause);
    }

    private void addFinancialSection(Document document, Rental rental) {
        Paragraph sectionTitle = new Paragraph("III. CONDICIONES ECONÓMICAS Y PLAZO")
                .setFontSize(12)
                .setBold()
                .setFontColor(PRIMARY_COLOR)
                .setMarginTop(15);
        document.add(sectionTitle);

        // Período
        Paragraph period = new Paragraph()
                .setFontSize(10)
                .add(new Text("PERÍODO DE ARRENDAMIENTO: ").setBold())
                .add("El presente contrato tendrá vigencia desde el día " + rental.getStartDate().format(DATE_FORMATTER)
                        + " ")
                .add("hasta el día " + rental.getEndDate().format(DATE_FORMATTER) + ", ")
                .add("comprendiendo un total de " + rental.getTotalDays() + " días calendario. ")
                .add("La no devolución del vehículo en la fecha pactada será considerada como mora y se aplicarán " +
                        "las penalidades establecidas en el presente contrato.");
        document.add(period);

        // Tabla de costos
        Table costsTable = new Table(new float[] { 2, 1 });
        costsTable.setWidth(UnitValue.createPercentValue(100));
        costsTable.setMarginTop(15);
        costsTable.setMarginBottom(10);

        // Header
        costsTable.addHeaderCell(new Cell()
                .add(new Paragraph("CONCEPTO").setBold())
                .setBackgroundColor(new DeviceRgb(240, 240, 240))
                .setTextAlignment(TextAlignment.CENTER)
                .setPadding(8));
        costsTable.addHeaderCell(new Cell()
                .add(new Paragraph("MONTO").setBold())
                .setBackgroundColor(new DeviceRgb(240, 240, 240))
                .setTextAlignment(TextAlignment.CENTER)
                .setPadding(8));

        // Rows
        addCostRow(costsTable, "Tarifa diaria", "$" + rental.getDailyRate());
        addCostRow(costsTable, "Días de arrendamiento", String.valueOf(rental.getTotalDays()));
        addCostRow(costsTable, "TOTAL A PAGAR", "$" + rental.getTotalAmount());
        addCostRow(costsTable, "Anticipo/Pagado", "$" + rental.getAmountPaid());
        addCostRow(costsTable, "SALDO PENDIENTE", "$" + rental.getBalance());

        document.add(costsTable);

        // Forma de pago
        Paragraph payment = new Paragraph()
                .setFontSize(10)
                .add(new Text("FORMA DE PAGO: ").setBold())
                .add("EL ARRENDATARIO se obliga a pagar la cantidad total establecida en el plazo acordado. " +
                        "El saldo pendiente deberá ser cancelado previo a la devolución del vehículo. " +
                        "Todo pago deberá ser realizado en efectivo o mediante transferencia bancaria a las cuentas " +
                        "designadas por EL ARRENDADOR.");
        document.add(payment);

        // Depósito
        Paragraph deposit = new Paragraph()
                .setFontSize(10)
                .setMarginTop(10)
                .add(new Text("DEPÓSITO DE GARANTÍA: ").setBold())
                .add("EL ARRENDATARIO se compromete a dejar un depósito de garantía equivalente al 20% del monto total, "
                        +
                        "el cual será reembolsado al término del contrato, previo a la verificación del estado del vehículo "
                        +
                        "y el cumplimiento de todas las obligaciones contractuales.");
        document.add(deposit);
    }

    private void addVehicleConditionSection(Document document, String deliveryNotes, List<String> photoUrls) {
        Paragraph sectionTitle = new Paragraph("IV. ESTADO DEL VEHÍCULO AL MOMENTO DE ENTREGA")
                .setFontSize(12)
                .setBold()
                .setFontColor(PRIMARY_COLOR)
                .setMarginTop(15);
        document.add(sectionTitle);

        Paragraph intro = new Paragraph()
                .setFontSize(10)
                .add("Ambas partes dejan constancia del estado en que se encuentra el vehículo al momento de su entrega:");
        document.add(intro);

        if (deliveryNotes != null && !deliveryNotes.isBlank()) {
            Paragraph notes = new Paragraph()
                    .setFontSize(9)
                    .setItalic()
                    .setMarginTop(10)
                    .setBackgroundColor(new DeviceRgb(250, 250, 250))
                    .setPadding(10)
                    .setBorder(new SolidBorder(GRAY_COLOR, 1))
                    .add(deliveryNotes);
            document.add(notes);
        }

        if (photoUrls != null && !photoUrls.isEmpty()) {
            Paragraph photoNote = new Paragraph()
                    .setFontSize(9)
                    .setMarginTop(10)
                    .add(new Text("NOTA: ").setBold())
                    .add("Se adjuntan " + photoUrls.size()
                            + " fotografías digitales que documentan el estado del vehículo, " +
                            "las cuales forman parte integral del presente contrato.");
            document.add(photoNote);
        }
    }

    private void addTermsAndConditions(Document document) {
        Paragraph sectionTitle = new Paragraph("V. TÉRMINOS Y CONDICIONES GENERALES")
                .setFontSize(12)
                .setBold()
                .setFontColor(PRIMARY_COLOR)
                .setMarginTop(15);
        document.add(sectionTitle);

        addClause(document, "1. USO DEL VEHÍCULO:",
                "EL ARRENDATARIO se compromete a utilizar el vehículo única y exclusivamente para fines personales " +
                        "y dentro del territorio nacional de la República de El Salvador, quedando expresamente prohibido el "
                        +
                        "uso comercial, competencias deportivas, remolque de otros vehículos, o cualquier actividad que "
                        +
                        "comprometa la integridad del vehículo.");

        addClause(document, "2. MANTENIMIENTO Y CUIDADO:",
                "EL ARRENDATARIO se obliga a mantener el vehículo en buen estado de conservación y limpieza, " +
                        "verificando periódicamente los niveles de aceite, agua y presión de llantas. Cualquier anomalía "
                        +
                        "mecánica deberá ser reportada inmediatamente a EL ARRENDADOR.");

        addClause(document, "3. SEGUROS:",
                "El vehículo cuenta con seguro contra daños a terceros y robo total. EL ARRENDATARIO será responsable "
                        +
                        "por el deducible del seguro en caso de siniestro, cuyo monto será comunicado al momento de la entrega.");

        addClause(document, "4. COMBUSTIBLE:",
                "EL ARRENDADOR entrega el vehículo con tanque lleno de combustible. EL ARRENDATARIO se obliga a " +
                        "devolverlo en las mismas condiciones. De no ser así, se cobrará el combustible faltante al precio "
                        +
                        "vigente en el mercado, más un cargo administrativo del 20%.");

        addClause(document, "5. INFRACCIONES DE TRÁNSITO:",
                "Toda multa, infracción, o sanción derivada del uso del vehículo durante el período de arrendamiento " +
                        "será responsabilidad exclusiva de EL ARRENDATARIO, quien se obliga a pagarlas directamente o a "
                        +
                        "reembolsar a EL ARRENDADOR cualquier cantidad que éste haya tenido que erogar por dichos conceptos.");
    }

    private void addTenantResponsibilities(Document document) {
        Paragraph sectionTitle = new Paragraph("VI. OBLIGACIONES DEL ARRENDATARIO")
                .setFontSize(12)
                .setBold()
                .setFontColor(PRIMARY_COLOR)
                .setMarginTop(15);
        document.add(sectionTitle);

        addClause(document, "1.", "Portar en todo momento su licencia de conducir vigente y el presente contrato.");
        addClause(document, "2.", "No ceder, subarrendar, o permitir el uso del vehículo a terceras personas sin " +
                "autorización expresa y por escrito de EL ARRENDADOR.");
        addClause(document, "3.",
                "No conducir el vehículo bajo los efectos del alcohol, drogas o sustancias estupefacientes.");
        addClause(document, "4.",
                "Mantener cerrado el vehículo cuando no esté en uso y estacionarlo en lugares seguros.");
        addClause(document, "5.",
                "Devolver el vehículo en la fecha, hora y lugar acordados, con todos sus accesorios y documentos.");
        addClause(document, "6.", "Reportar inmediatamente cualquier accidente, robo, o daño que sufra el vehículo.");
    }

    private void addTerminationClauses(Document document) {
        Paragraph sectionTitle = new Paragraph("VII. CAUSALES DE RESCISIÓN")
                .setFontSize(12)
                .setBold()
                .setFontColor(PRIMARY_COLOR)
                .setMarginTop(15);
        document.add(sectionTitle);

        Paragraph intro = new Paragraph()
                .setFontSize(10)
                .add("EL ARRENDADOR podrá dar por terminado el presente contrato de forma inmediata y sin " +
                        "responsabilidad alguna en los siguientes casos:");
        document.add(intro);

        addClause(document, "a)",
                "Incumplimiento de cualquiera de las obligaciones establecidas en el presente contrato.");
        addClause(document, "b)", "Uso indebido del vehículo o destinarlo a fines distintos a los pactados.");
        addClause(document, "c)", "Mora en el pago de las cantidades adeudadas por más de 24 horas.");
        addClause(document, "d)", "Negativa a devolver el vehículo en la fecha pactada.");
        addClause(document, "e)", "Falsedad en los datos proporcionados por EL ARRENDATARIO.");

        Paragraph penalty = new Paragraph()
                .setFontSize(10)
                .setMarginTop(10)
                .add(new Text("PENALIZACIÓN POR MORA: ").setBold())
                .add("En caso de mora en la devolución del vehículo, EL ARRENDATARIO pagará el doble de la tarifa " +
                        "diaria por cada día de retraso, sin perjuicio de las acciones legales que EL ARRENDADOR pueda ejercer.");
        document.add(penalty);
    }

    private void addSignatureSection(Document document, Rental rental) {
        document.add(new AreaBreak());

        Paragraph sectionTitle = new Paragraph("VIII. ACEPTACIÓN Y FIRMAS")
                .setFontSize(12)
                .setBold()
                .setFontColor(PRIMARY_COLOR)
                .setMarginTop(15);
        document.add(sectionTitle);

        Paragraph acceptance = new Paragraph()
                .setFontSize(10)
                .add("Ambas partes manifiestan su conformidad con todas las cláusulas del presente contrato, " +
                        "el cual aceptan y firman en la ciudad de San Salvador, a los " +
                        LocalDate.now().getDayOfMonth() + " días del mes de " +
                        obtenerNombreMes(LocalDate.now().getMonthValue()) + " del año " +
                        LocalDate.now().getYear() + ".");
        document.add(acceptance);

        // Tabla de firmas
        Table sigTable = new Table(new float[] { 1, 1 });
        sigTable.setWidth(UnitValue.createPercentValue(100));
        sigTable.setMarginTop(40);

        // Arrendador
        Cell landlordCell = new Cell()
                .setBorder(Border.NO_BORDER)
                .setTextAlignment(TextAlignment.CENTER);
        landlordCell.add(new Paragraph("_____________________________")
                .setMarginBottom(5));
        landlordCell.add(new Paragraph("EL ARRENDADOR")
                .setBold()
                .setFontSize(10));
        landlordCell.add(new Paragraph("RentaCar ESV")
                .setFontSize(9));
        landlordCell.add(new Paragraph("Representante Legal")
                .setFontSize(9)
                .setItalic());

        // Arrendatario
        Cell tenantCell = new Cell()
                .setBorder(Border.NO_BORDER)
                .setTextAlignment(TextAlignment.CENTER);
        tenantCell.add(new Paragraph("_____________________________")
                .setMarginBottom(5));
        tenantCell.add(new Paragraph("EL ARRENDATARIO")
                .setBold()
                .setFontSize(10));
        tenantCell.add(new Paragraph(rental.getCustomer().getFullName())
                .setFontSize(9));
        tenantCell.add(new Paragraph("DUI: " + rental.getCustomer().getDocumentNumber())
                .setFontSize(9)
                .setItalic());

        sigTable.addCell(landlordCell);
        sigTable.addCell(tenantCell);

        document.add(sigTable);

        // Footer
        Paragraph footer = new Paragraph()
                .setFontSize(8)
                .setMarginTop(30)
                .setTextAlignment(TextAlignment.CENTER)
                .setFontColor(GRAY_COLOR)
                .add("Este documento ha sido generado electrónicamente y tiene plena validez legal conforme a las leyes de la República de El Salvador.\n")
                .add("Para cualquier consulta o aclaración, comunicarse al [TELÉFONO] o enviar correo a [EMAIL]");
        document.add(footer);
    }

    // Métodos auxiliares
    private void addTableRow(Table table, String label, String value) {
        table.addCell(new Cell()
                .add(new Paragraph(label).setBold().setFontSize(9))
                .setPadding(5)
                .setBackgroundColor(new DeviceRgb(250, 250, 250)));
        table.addCell(new Cell()
                .add(new Paragraph(value).setFontSize(9))
                .setPadding(5));
    }

    private void addCostRow(Table table, String concept, String amount) {
        table.addCell(new Cell()
                .add(new Paragraph(concept).setFontSize(9))
                .setPadding(8));
        table.addCell(new Cell()
                .add(new Paragraph(amount).setFontSize(9).setBold())
                .setPadding(8)
                .setTextAlignment(TextAlignment.RIGHT));
    }

    private void addClause(Document document, String title, String content) {
        Paragraph clause = new Paragraph()
                .setFontSize(10)
                .setMarginTop(5)
                .add(new Text(title + " ").setBold())
                .add(content);
        document.add(clause);
    }

    private String obtenerNombreMes(int mes) {
        String[] meses = { "enero", "febrero", "marzo", "abril", "mayo", "junio",
                "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre" };
        return meses[mes - 1];
    }
}
