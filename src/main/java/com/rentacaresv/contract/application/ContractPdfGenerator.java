package com.rentacaresv.contract.application;

import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;
import com.rentacaresv.contract.domain.*;
import com.rentacaresv.customer.domain.Customer;
import com.rentacaresv.rental.domain.Rental;
import com.rentacaresv.settings.application.SettingsCache;
import com.rentacaresv.vehicle.domain.Vehicle;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Generador de PDF para contratos digitales.
 * Formato completo con todas las secciones.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ContractPdfGenerator {

    private final SettingsCache settingsCache;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    
    // Colores
    private static final DeviceRgb PRIMARY_COLOR = new DeviceRgb(0, 51, 102);
    private static final DeviceRgb LIGHT_GRAY = new DeviceRgb(240, 240, 240);
    private static final DeviceRgb BORDER_COLOR = new DeviceRgb(180, 180, 180);
    private static final DeviceRgb SUCCESS_COLOR = new DeviceRgb(0, 128, 0);
    private static final DeviceRgb ERROR_COLOR = new DeviceRgb(200, 0, 0);
    private static final DeviceRgb WARNING_BG = new DeviceRgb(255, 255, 220);
    
    // Tamaños de fuente
    private static final float FONT_TITLE = 14f;
    private static final float FONT_SUBTITLE = 11f;
    private static final float FONT_SECTION = 10f;
    private static final float FONT_NORMAL = 9f;
    private static final float FONT_SMALL = 8f;

    /**
     * Genera el PDF del contrato en memoria.
     */
    public byte[] generatePdf(Contract contract) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc, PageSize.LETTER);
            document.setMargins(25, 30, 25, 30);

            Rental rental = contract.getRental();
            Customer customer = rental.getCustomer();
            Vehicle vehicle = rental.getVehicle();

            // === CONTENIDO DEL PDF ===
            
            // HEADER con logo y título
            addHeader(document, contract);
            
            // DATOS DEL ARRENDATARIO
            addClientSection(document, contract, customer);
            
            // DATOS DEL VEHÍCULO Y RENTA
            addVehicleSection(document, contract, rental, vehicle);
            
            // CONDUCTOR ADICIONAL (si existe)
            if (contract.getAdditionalDriverName() != null && !contract.getAdditionalDriverName().isEmpty()) {
                addAdditionalDriverSection(document, contract);
            }
            
            // REVISIÓN DE ACCESORIOS (lista completa con checks)
            addAccessoriesSection(document, contract);
            
            // REVISIÓN FÍSICA DEL VEHÍCULO
            addPhysicalReviewSection(document, contract, vehicle);
            
            // DIAGRAMA DE DAÑOS CON IMAGEN
            addDamagesDiagramSection(document, contract, vehicle);
            
            // TÉRMINOS Y CONDICIONES
            addTermsSection(document);
            
            // OBSERVACIONES (si hay)
            if (contract.getObservations() != null && !contract.getObservations().isEmpty()) {
                addObservationsSection(document, contract);
            }
            
            // FIRMAS (con imagen de firma digital)
            addSignatureSection(document, contract, customer);

            document.close();
            
            log.info("PDF generado exitosamente para contrato {}", rental.getContractNumber());
            return baos.toByteArray();

        } catch (Exception e) {
            log.error("Error generando PDF: {}", e.getMessage(), e);
            throw new RuntimeException("Error al generar PDF", e);
        }
    }

    // ========================================
    // HEADER
    // ========================================
    private void addHeader(Document document, Contract contract) {
        Table headerTable = new Table(new float[]{1.5f, 4f, 2f});
        headerTable.setWidth(UnitValue.createPercentValue(100));
        headerTable.setBorder(Border.NO_BORDER);

        // Logo (izquierda)
        Cell logoCell = new Cell().setBorder(Border.NO_BORDER).setVerticalAlignment(VerticalAlignment.MIDDLE);
        String logoUrl = settingsCache.getLogoUrl();
        if (logoUrl != null && !logoUrl.isEmpty()) {
            try {
                ImageData imageData = ImageDataFactory.create(new URL(logoUrl));
                Image logo = new Image(imageData);
                logo.setMaxHeight(55);
                logo.setMaxWidth(110);
                logoCell.add(logo);
            } catch (Exception e) {
                logoCell.add(new Paragraph(settingsCache.getCompanyName()).setBold().setFontSize(FONT_TITLE));
            }
        } else {
            logoCell.add(new Paragraph(settingsCache.getCompanyName()).setBold().setFontSize(FONT_TITLE));
        }
        headerTable.addCell(logoCell);

        // Título (centro)
        Cell titleCell = new Cell().setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.CENTER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE);
        titleCell.add(new Paragraph(settingsCache.getCompanyName().toUpperCase())
            .setBold().setFontSize(FONT_TITLE).setFontColor(PRIMARY_COLOR));
        titleCell.add(new Paragraph("CONTRATO DE ALQUILER DE VEHÍCULOS")
            .setBold().setFontSize(FONT_SUBTITLE));
        headerTable.addCell(titleCell);

        // Info contrato (derecha)
        Cell infoCell = new Cell().setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.RIGHT)
                .setVerticalAlignment(VerticalAlignment.MIDDLE);
        infoCell.add(new Paragraph("Contrato #: " + contract.getRental().getContractNumber())
            .setFontSize(FONT_NORMAL).setBold().setFontColor(PRIMARY_COLOR));
        infoCell.add(new Paragraph("Fecha: " + contract.getCreatedAt().format(DATE_FMT))
            .setFontSize(FONT_SMALL));
        if (contract.getDeliveryLocation() != null && !contract.getDeliveryLocation().isEmpty()) {
            infoCell.add(new Paragraph("Lugar: " + contract.getDeliveryLocation())
                .setFontSize(FONT_SMALL));
        }
        headerTable.addCell(infoCell);

        document.add(headerTable);
        document.add(new Paragraph().setMarginBottom(10));
    }

    // ========================================
    // DATOS DEL CLIENTE
    // ========================================
    private void addClientSection(Document document, Contract contract, Customer customer) {
        document.add(createSectionTitle("DATOS DEL ARRENDATARIO"));

        Table table = new Table(new float[]{1, 1, 1, 1});
        table.setWidth(UnitValue.createPercentValue(100));

        // Fila 1: Nombre completo (2 cols), DUI, Licencia
        addCell(table, "Nombre:", customer.getFullName(), 2);
        String docNumber = contract.getDocumentNumber() != null ? contract.getDocumentNumber() : 
                          (customer.getDocumentNumber() != null ? customer.getDocumentNumber() : "-");
        addCell(table, "DUI:", docNumber, 1);
        addCell(table, "Licencia:", customer.getDriverLicenseNumber() != null ? customer.getDriverLicenseNumber() : "-", 1);

        // Fila 2: Dirección SV (2 cols), Tel, Tel USA
        String addressSV = contract.getAddressElSalvador() != null ? contract.getAddressElSalvador() : 
                          (customer.getAddress() != null ? customer.getAddress() : "-");
        addCell(table, "Dir. El Salvador:", addressSV, 2);
        String phoneFamily = contract.getPhoneFamily() != null ? contract.getPhoneFamily() : 
                            (customer.getPhone() != null ? customer.getPhone() : "-");
        addCell(table, "Teléfono:", phoneFamily, 1);
        String phoneUSA = contract.getPhoneUsa() != null ? contract.getPhoneUsa() : "-";
        addCell(table, "Tel. USA:", phoneUSA, 1);

        // Fila 3: Dirección extranjero (si existe)
        if (contract.getAddressForeign() != null && !contract.getAddressForeign().isEmpty()) {
            addCell(table, "Dir. Extranjero:", contract.getAddressForeign(), 4);
        }

        document.add(table);
        document.add(new Paragraph().setMarginBottom(8));
    }

    // ========================================
    // DATOS DEL VEHÍCULO
    // ========================================
    private void addVehicleSection(Document document, Contract contract, Rental rental, Vehicle vehicle) {
        document.add(createSectionTitle("DATOS DEL VEHÍCULO Y RENTA"));

        Table table = new Table(new float[]{1, 1, 1, 1, 1, 1});
        table.setWidth(UnitValue.createPercentValue(100));

        // Fila 1
        addCell(table, "Vehículo:", vehicle.getBrand() + " " + vehicle.getModel() + " " + vehicle.getYear(), 2);
        addCell(table, "Placa:", vehicle.getLicensePlate(), 1);
        addCell(table, "Color:", vehicle.getColor() != null ? vehicle.getColor() : "-", 1);
        addCell(table, "Días:", String.valueOf(rental.getTotalDays()), 1);
        addCell(table, "Total:", "$" + rental.getTotalAmount(), 1);

        // Fila 2
        addCell(table, "Fecha Salida:", rental.getStartDate().format(DATE_FMT), 1);
        addCell(table, "Fecha Entrada:", rental.getEndDate().format(DATE_FMT), 1);
        String paymentMethod = contract.getPaymentMethod() != null ? contract.getPaymentMethod().getLabel() : "-";
        addCell(table, "Forma Pago:", paymentMethod, 1);
        String deposit = contract.getDepositAmount() != null ? "$" + contract.getDepositAmount() : "-";
        addCell(table, "Depósito:", deposit, 1);
        String accDeductible = contract.getAccidentDeductible() != null ? "$" + contract.getAccidentDeductible() : "-";
        addCell(table, "Ded. Accidente:", accDeductible, 1);
        // Deducible por robo como porcentaje del valor del vehículo
        String theftDeductible = vehicle.getTheftDeductiblePercentage() + "% del valor";
        addCell(table, "Ded. Robo:", theftDeductible, 1);

        document.add(table);
        document.add(new Paragraph().setMarginBottom(8));
    }

    // ========================================
    // CONDUCTOR ADICIONAL
    // ========================================
    private void addAdditionalDriverSection(Document document, Contract contract) {
        Table table = new Table(new float[]{2, 1, 1});
        table.setWidth(UnitValue.createPercentValue(100));

        addCell(table, "Conductor Adicional:", contract.getAdditionalDriverName(), 1);
        addCell(table, "Licencia:", contract.getAdditionalDriverLicense() != null ? contract.getAdditionalDriverLicense() : "-", 1);
        addCell(table, "DUI:", contract.getAdditionalDriverDui() != null ? contract.getAdditionalDriverDui() : "-", 1);

        document.add(table);
        document.add(new Paragraph().setMarginBottom(8));
    }

    // ========================================
    // REVISIÓN DE ACCESORIOS
    // ========================================
    private void addAccessoriesSection(Document document, Contract contract) {
        document.add(createSectionTitle("REVISIÓN DE ACCESORIOS"));

        Set<ContractAccessory> accessoriesSet = contract.getAccessories();
        if (accessoriesSet == null || accessoriesSet.isEmpty()) {
            document.add(new Paragraph("Sin accesorios registrados").setFontSize(FONT_SMALL).setItalic());
            document.add(new Paragraph().setMarginBottom(8));
            return;
        }

        // Convertir a lista ordenada
        List<ContractAccessory> accessories = accessoriesSet.stream()
            .sorted(Comparator.comparingInt(a -> a.getDisplayOrder() != null ? a.getDisplayOrder() : 0))
            .collect(Collectors.toList());

        // Crear tabla con 4 columnas (2 pares de accesorio + check)
        Table table = new Table(new float[]{3f, 0.7f, 3f, 0.7f});
        table.setWidth(UnitValue.createPercentValue(100));

        // Header
        table.addHeaderCell(createHeaderCell("Accesorio"));
        table.addHeaderCell(createHeaderCell("✓"));
        table.addHeaderCell(createHeaderCell("Accesorio"));
        table.addHeaderCell(createHeaderCell("✓"));

        // Dividir accesorios en 2 columnas
        int half = (accessories.size() + 1) / 2;
        for (int i = 0; i < half; i++) {
            // Columna 1
            ContractAccessory acc1 = accessories.get(i);
            table.addCell(createAccessoryCell(acc1.getAccessoryName()));
            table.addCell(createCheckCell(acc1.getIsPresent()));

            // Columna 2
            int idx2 = i + half;
            if (idx2 < accessories.size()) {
                ContractAccessory acc2 = accessories.get(idx2);
                table.addCell(createAccessoryCell(acc2.getAccessoryName()));
                table.addCell(createCheckCell(acc2.getIsPresent()));
            } else {
                table.addCell(createAccessoryCell(""));
                table.addCell(createAccessoryCell(""));
            }
        }

        document.add(table);
        document.add(new Paragraph().setMarginBottom(8));
    }

    // ========================================
    // REVISIÓN FÍSICA DEL VEHÍCULO
    // ========================================
    private void addPhysicalReviewSection(Document document, Contract contract, Vehicle vehicle) {
        document.add(createSectionTitle("REVISIÓN FÍSICA DEL VEHÍCULO"));

        Table table = new Table(new float[]{1, 1, 1, 1, 2});
        table.setWidth(UnitValue.createPercentValue(100));

        String mileageOut = contract.getMileageOut() != null ? contract.getMileageOut().toString() + " km" : "-";
        String fuelOut = contract.getFuelLevelOut() != null ? contract.getFuelLevelOut() + "%" : "-";
        String fuelType = vehicle.getFuelType() != null ? vehicle.getFuelType().name() : "-";
        String transmission = vehicle.getTransmissionType() != null ? vehicle.getTransmissionType().name() : "-";

        addCell(table, "Kilometraje:", mileageOut, 1);
        addCell(table, "Combustible:", fuelOut, 1);
        addCell(table, "Tipo Comb.:", fuelType, 1);
        addCell(table, "Transmisión:", transmission, 1);
        
        // Nota importante con fondo amarillo
        Cell notaCell = new Cell().setBorder(new SolidBorder(BORDER_COLOR, 0.5f)).setPadding(5)
                .setBackgroundColor(WARNING_BG);
        notaCell.add(new Paragraph("NOTA IMPORTANTE:")
                .setFontSize(FONT_SMALL).setBold());
        notaCell.add(new Paragraph("• Lavado: $5.00  • Manchas tapicería: $20.00")
                .setFontSize(FONT_SMALL));
        table.addCell(notaCell);

        document.add(table);
        document.add(new Paragraph().setMarginBottom(8));
    }

    // ========================================
    // VIDEO DEL ESTADO DEL VEHÍCULO
    // ========================================
    private void addDamagesDiagramSection(Document document, Contract contract, Vehicle vehicle) {
        document.add(createSectionTitle("ESTADO DEL VEHÍCULO - VIDEO"));

        Table videoTable = new Table(1);
        videoTable.setWidth(UnitValue.createPercentValue(100));
        
        Cell videoCell = new Cell()
                .setBorder(new SolidBorder(BORDER_COLOR, 1))
                .setPadding(15)
                .setTextAlignment(TextAlignment.CENTER);

        // Verificar si hay video del vehículo
        String videoUrl = contract.getVehicleVideoUrl();
        
        if (videoUrl != null && !videoUrl.isEmpty()) {
            // Hay video - mostrar icono y enlace
            videoCell.add(new Paragraph("🎥")
                    .setFontSize(24)
                    .setTextAlignment(TextAlignment.CENTER));
            
            videoCell.add(new Paragraph("VIDEO DEL ESTADO DEL VEHÍCULO")
                    .setBold()
                    .setFontSize(FONT_SUBTITLE)
                    .setFontColor(PRIMARY_COLOR)
                    .setTextAlignment(TextAlignment.CENTER));
            
            videoCell.add(new Paragraph("Se grabó un video mostrando el estado del vehículo al momento de la entrega.")
                    .setFontSize(FONT_SMALL)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginTop(5));
            
            // Link al video
            videoCell.add(new Paragraph()
                    .setMarginTop(10)
                    .add(new Text("Ver video: ").setFontSize(FONT_SMALL))
                    .add(new Text(videoUrl)
                            .setFontSize(FONT_SMALL)
                            .setFontColor(new DeviceRgb(0, 102, 204))
                            .setUnderline()));
            
            videoCell.add(new Paragraph("(Copie y pegue el enlace en su navegador para ver el video)")
                    .setFontSize(7)
                    .setItalic()
                    .setFontColor(new DeviceRgb(120, 120, 120))
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginTop(3));
            
        } else {
            // No hay video
            videoCell.add(new Paragraph("📹")
                    .setFontSize(20)
                    .setFontColor(new DeviceRgb(150, 150, 150))
                    .setTextAlignment(TextAlignment.CENTER));
            
            videoCell.add(new Paragraph("VIDEO NO DISPONIBLE")
                    .setFontSize(FONT_NORMAL)
                    .setItalic()
                    .setFontColor(new DeviceRgb(150, 150, 150))
                    .setTextAlignment(TextAlignment.CENTER));
            
            videoCell.add(new Paragraph("No se grabó video del estado del vehículo para este contrato.")
                    .setFontSize(FONT_SMALL)
                    .setFontColor(new DeviceRgb(150, 150, 150))
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginTop(5));
        }

        videoTable.addCell(videoCell);
        document.add(videoTable);
        
        // Nota informativa
        Table noteTable = new Table(1);
        noteTable.setWidth(UnitValue.createPercentValue(100));
        noteTable.setMarginTop(8);
        
        Cell noteCell = new Cell()
                .setBackgroundColor(WARNING_BG)
                .setBorder(new SolidBorder(BORDER_COLOR, 0.5f))
                .setPadding(8);
        noteCell.add(new Paragraph("IMPORTANTE:")
                .setBold()
                .setFontSize(FONT_SMALL));
        noteCell.add(new Paragraph("El video adjunto constituye evidencia del estado del vehículo al momento de la entrega. " +
                "Cualquier daño no visible en el video y encontrado durante la devolución será responsabilidad del arrendatario.")
                .setFontSize(FONT_SMALL));
        noteTable.addCell(noteCell);
        document.add(noteTable);
        
        document.add(new Paragraph().setMarginBottom(8));
    }

    // ========================================
    // TÉRMINOS Y CONDICIONES
    // ========================================
    private void addTermsSection(Document document) {
        document.add(createSectionTitle("TÉRMINOS Y CONDICIONES"));

        List<String> terms = List.of(
            "El arrendatario se compromete a devolver el vehículo en las mismas condiciones en que lo recibió.",
            "Entregar lavado el vehículo, de lo contrario se cobrará entre $5 a $15 dependiendo de cómo se entregue de sucio.",
            "Manchas o derrame de líquido en tapicería: $20.00.",
            "El arrendatario es responsable de cualquier daño o pérdida del vehículo durante el período de renta.",
            "En caso de accidente, el arrendatario participará con el deducible establecido en este contrato.",
            "En caso de robo, aplicará el deducible por robo acordado (porcentaje del valor del vehículo).",
            "El vehículo no puede salir del país sin autorización previa por escrito.",
            "Está prohibido fumar dentro del vehículo."
        );

        Table termsTable = new Table(1);
        termsTable.setWidth(UnitValue.createPercentValue(100));
        
        Cell termsCell = new Cell().setPadding(8).setBorder(new SolidBorder(BORDER_COLOR, 0.5f));
        for (int i = 0; i < terms.size(); i++) {
            termsCell.add(new Paragraph((i + 1) + ". " + terms.get(i)).setFontSize(FONT_SMALL));
        }
        termsTable.addCell(termsCell);
        
        document.add(termsTable);
        document.add(new Paragraph().setMarginBottom(8));
    }

    // ========================================
    // OBSERVACIONES
    // ========================================
    private void addObservationsSection(Document document, Contract contract) {
        Table table = new Table(1);
        table.setWidth(UnitValue.createPercentValue(100));
        
        Cell cell = new Cell().setPadding(8).setBorder(new SolidBorder(BORDER_COLOR, 0.5f));
        cell.add(new Paragraph("OBSERVACIONES:").setBold().setFontSize(FONT_NORMAL));
        cell.add(new Paragraph(contract.getObservations()).setFontSize(FONT_SMALL));
        table.addCell(cell);
        
        document.add(table);
        document.add(new Paragraph().setMarginBottom(8));
    }

    // ========================================
    // FIRMAS
    // ========================================
    private void addSignatureSection(Document document, Contract contract, Customer customer) {
        document.add(new Paragraph().setMarginTop(15));
        
        Table table = new Table(new float[]{1, 1});
        table.setWidth(UnitValue.createPercentValue(100));
        table.setBorder(Border.NO_BORDER);

        // ===== FIRMA CLIENTE =====
        Cell clientCell = new Cell().setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.CENTER)
                .setPaddingLeft(30).setPaddingRight(30);
        
        // Agregar imagen de firma si existe
        if (contract.getSignatureUrl() != null && !contract.getSignatureUrl().isEmpty()) {
            try {
                ImageData signatureData = ImageDataFactory.create(new URL(contract.getSignatureUrl()));
                Image signatureImg = new Image(signatureData);
                signatureImg.setMaxHeight(70);
                signatureImg.setMaxWidth(180);
                signatureImg.setHorizontalAlignment(HorizontalAlignment.CENTER);
                clientCell.add(signatureImg);
            } catch (Exception e) {
                log.warn("No se pudo cargar la firma del cliente: {}", e.getMessage());
                clientCell.add(new Paragraph().setMarginTop(50));
            }
        } else {
            clientCell.add(new Paragraph().setMarginTop(50));
        }
        
        clientCell.add(new Paragraph("_______________________________").setFontSize(FONT_SMALL));
        clientCell.add(new Paragraph("CLIENTE").setBold().setFontSize(FONT_SMALL));
        clientCell.add(new Paragraph(customer.getFullName()).setFontSize(FONT_SMALL));
        
        if (contract.getSignedAt() != null) {
            clientCell.add(new Paragraph("Firmado: " + contract.getSignedAt().format(DATETIME_FMT))
                    .setFontSize(7).setItalic().setFontColor(new DeviceRgb(100, 100, 100)));
        }
        table.addCell(clientCell);

        // ===== FIRMA EMPLEADO =====
        Cell employeeCell = new Cell().setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.CENTER)
                .setPaddingLeft(30).setPaddingRight(30);
        
        // Agregar imagen de firma del empleado si existe
        if (contract.getEmployeeSignatureUrl() != null && !contract.getEmployeeSignatureUrl().isEmpty()) {
            try {
                ImageData employeeSignatureData = ImageDataFactory.create(new URL(contract.getEmployeeSignatureUrl()));
                Image employeeSignatureImg = new Image(employeeSignatureData);
                employeeSignatureImg.setMaxHeight(70);
                employeeSignatureImg.setMaxWidth(180);
                employeeSignatureImg.setHorizontalAlignment(HorizontalAlignment.CENTER);
                employeeCell.add(employeeSignatureImg);
            } catch (Exception e) {
                log.warn("No se pudo cargar la firma del empleado: {}", e.getMessage());
                employeeCell.add(new Paragraph().setMarginTop(50));
            }
        } else {
            employeeCell.add(new Paragraph().setMarginTop(50));
        }
        
        employeeCell.add(new Paragraph("_______________________________").setFontSize(FONT_SMALL));
        employeeCell.add(new Paragraph("EMPLEADO").setBold().setFontSize(FONT_SMALL));
        
        // Nombre del empleado si existe, sino nombre de la empresa
        String employeeName = contract.getEmployeeName() != null && !contract.getEmployeeName().isEmpty() 
                ? contract.getEmployeeName() : settingsCache.getCompanyName();
        employeeCell.add(new Paragraph(employeeName).setFontSize(FONT_SMALL));
        table.addCell(employeeCell);

        document.add(table);
    }

    // ========================================
    // MÉTODOS AUXILIARES
    // ========================================
    
    private Paragraph createSectionTitle(String title) {
        return new Paragraph(title)
            .setBold()
            .setFontSize(FONT_SECTION)
            .setFontColor(PRIMARY_COLOR)
            .setBackgroundColor(LIGHT_GRAY)
            .setPadding(6)
            .setMarginTop(5)
            .setMarginBottom(5);
    }

    private void addCell(Table table, String label, String value, int colspan) {
        Cell cell = new Cell(1, colspan);
        cell.setBorder(new SolidBorder(BORDER_COLOR, 0.5f));
        cell.setPadding(5);
        cell.add(new Paragraph()
            .add(new Text(label).setBold().setFontSize(FONT_SMALL))
            .add(new Text(" " + (value != null ? value : "-")).setFontSize(FONT_SMALL)));
        table.addCell(cell);
    }

    private Cell createHeaderCell(String text) {
        Cell cell = new Cell()
            .setBackgroundColor(new DeviceRgb(220, 220, 220))
            .setPadding(4)
            .setBorder(new SolidBorder(BORDER_COLOR, 0.5f));
        cell.add(new Paragraph(text).setBold().setFontSize(FONT_SMALL).setTextAlignment(TextAlignment.CENTER));
        return cell;
    }

    private Cell createAccessoryCell(String text) {
        return new Cell()
            .add(new Paragraph(text != null ? text : "").setFontSize(FONT_SMALL))
            .setPadding(4)
            .setBorder(new SolidBorder(BORDER_COLOR, 0.5f));
    }

    private Cell createCheckCell(Boolean checked) {
        String symbol = Boolean.TRUE.equals(checked) ? "✓" : "✗";
        DeviceRgb color = Boolean.TRUE.equals(checked) ? SUCCESS_COLOR : ERROR_COLOR;
        
        return new Cell()
            .add(new Paragraph(symbol)
                    .setFontSize(FONT_NORMAL)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontColor(color)
                    .setBold())
            .setPadding(3)
            .setBorder(new SolidBorder(BORDER_COLOR, 0.5f));
    }
}
