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
import com.rentacaresv.rental.domain.Rental;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

/**
 * Genera el PDF de confirmación de reserva web para enviar al cliente.
 */
@Service
@Slf4j
public class ReservationConfirmationPdfGenerator {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DeviceRgb DARK_BLUE  = new DeviceRgb(0,  51, 102);
    private static final DeviceRgb ORANGE     = new DeviceRgb(255, 152,  0);
    private static final DeviceRgb LIGHT_GRAY = new DeviceRgb(245, 245, 245);
    private static final DeviceRgb GRAY_TEXT  = new DeviceRgb(100, 100, 100);

    public byte[] generate(Rental rental, String companyName) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document doc = new Document(pdfDoc, PageSize.LETTER);
            doc.setMargins(40, 50, 40, 50);

            addHeader(doc, companyName, rental);
            addStatusBanner(doc);
            addCustomerSection(doc, rental);
            addVehicleSection(doc, rental);
            addFinancialSection(doc, rental);
            addNextStepsSection(doc);
            addFooter(doc, companyName);

            doc.close();
            return baos.toByteArray();

        } catch (Exception e) {
            log.error("Error generando PDF de confirmación de reserva {}: {}", rental.getContractNumber(), e.getMessage(), e);
            throw new RuntimeException("Error generando PDF de confirmación", e);
        }
    }

    // ── Header ──────────────────────────────────────────────────────────────

    private void addHeader(Document doc, String companyName, Rental rental) {
        Table header = new Table(UnitValue.createPercentArray(new float[]{2, 1}))
                .setWidth(UnitValue.createPercentValue(100))
                .setMarginBottom(20);

        // Nombre empresa
        Cell left = new Cell()
                .add(new Paragraph(companyName != null ? companyName : "Nova Rentacar")
                        .setFontSize(20).setBold().setFontColor(DARK_BLUE))
                .add(new Paragraph("Confirmación de Reserva")
                        .setFontSize(11).setFontColor(GRAY_TEXT))
                .setBorder(Border.NO_BORDER).setPadding(0);

        // Número de contrato
        Cell right = new Cell()
                .add(new Paragraph(rental.getContractNumber())
                        .setFontSize(13).setBold().setFontColor(DARK_BLUE).setTextAlignment(TextAlignment.RIGHT))
                .add(new Paragraph("Fecha: " + java.time.LocalDate.now().format(DATE_FMT))
                        .setFontSize(9).setFontColor(GRAY_TEXT).setTextAlignment(TextAlignment.RIGHT))
                .setBorder(Border.NO_BORDER).setPadding(0);

        header.addCell(left);
        header.addCell(right);
        doc.add(header);

        // Línea separadora
        doc.add(new LineSeparator(new com.itextpdf.kernel.pdf.canvas.draw.SolidLine(1.5f))
                .setStrokeColor(DARK_BLUE).setMarginBottom(15));
    }

    // ── Banner "RESERVA PENDIENTE" ───────────────────────────────────────────

    private void addStatusBanner(Document doc) {
        Table banner = new Table(1)
                .setWidth(UnitValue.createPercentValue(100))
                .setMarginBottom(20);

        Cell cell = new Cell()
                .add(new Paragraph("✔ RESERVA RECIBIDA — PENDIENTE DE ENTREGA")
                        .setBold().setFontSize(13).setFontColor(DARK_BLUE)
                        .setTextAlignment(TextAlignment.CENTER))
                .add(new Paragraph("Su reserva fue registrada exitosamente. El vehículo será entregado en la fecha acordada, " +
                        "momento en el que se firmará el contrato de arrendamiento.")
                        .setFontSize(10).setFontColor(GRAY_TEXT)
                        .setTextAlignment(TextAlignment.CENTER))
                .setBackgroundColor(LIGHT_GRAY)
                .setBorder(new SolidBorder(ORANGE, 2))
                .setPadding(14);

        banner.addCell(cell);
        doc.add(banner);
    }

    // ── Datos del cliente ────────────────────────────────────────────────────

    private void addCustomerSection(Document doc, Rental rental) {
        doc.add(sectionTitle("Datos del Cliente"));

        Table t = twoColTable();
        var c = rental.getCustomer();

        addRow(t, "Nombre completo",  c.getFullName());
        addRow(t, "Tipo de documento", c.getDocumentType() != null ? c.getDocumentType().getLabel() : "");
        addRow(t, "Número de documento", c.getDocumentNumber());
        addRow(t, "Email", c.getEmail());
        addRow(t, "Teléfono", c.getPhone());

        if (c.getDriverLicenseNumber() != null) {
            addRow(t, "Licencia de conducir", c.getDriverLicenseNumber()
                    + (c.getDriverLicenseCountry() != null ? " (" + c.getDriverLicenseCountry() + ")" : ""));
        }
        if (c.getDriverLicenseExpiry() != null) {
            addRow(t, "Vencimiento licencia", c.getDriverLicenseExpiry().format(DATE_FMT));
        }

        doc.add(t.setMarginBottom(16));
    }

    // ── Datos del vehículo ───────────────────────────────────────────────────

    private void addVehicleSection(Document doc, Rental rental) {
        doc.add(sectionTitle("Detalles de la Reserva"));

        Table t = twoColTable();
        var v = rental.getVehicle();

        addRow(t, "Vehículo", v.getFullDescription());
        if (v.getLicensePlate() != null) addRow(t, "Placa", v.getLicensePlate());
        addRow(t, "Fecha de entrega",    rental.getStartDate().format(DATE_FMT));
        addRow(t, "Fecha de devolución", rental.getEndDate().format(DATE_FMT));
        addRow(t, "Días de renta",       rental.getTotalDays() + " día(s)");

        if (rental.getFlightNumber() != null && !rental.getFlightNumber().isBlank())
            addRow(t, "Vuelo", rental.getFlightNumber());
        if (rental.getAccommodation() != null && !rental.getAccommodation().isBlank())
            addRow(t, "Hospedaje", rental.getAccommodation());
        if (rental.getContactPhone() != null && !rental.getContactPhone().isBlank())
            addRow(t, "Teléfono de contacto", rental.getContactPhone());

        doc.add(t.setMarginBottom(16));
    }

    // ── Resumen financiero ───────────────────────────────────────────────────

    private void addFinancialSection(Document doc, Rental rental) {
        doc.add(sectionTitle("Resumen de Costos"));

        Table t = twoColTable();
        addRow(t, "Tarifa diaria",  "$" + rental.getDailyRate());
        addRow(t, "Días",           rental.getTotalDays() + " día(s)");
        addRow(t, "Total estimado", "$" + rental.getTotalAmount());

        doc.add(t.setMarginBottom(8));

        doc.add(new Paragraph("* El monto final puede variar según accesorios, seguros o cargos adicionales acordados al momento de la entrega.")
                .setFontSize(8).setFontColor(GRAY_TEXT).setItalic().setMarginBottom(16));
    }

    // ── Próximos pasos ───────────────────────────────────────────────────────

    private void addNextStepsSection(Document doc) {
        doc.add(sectionTitle("Próximos Pasos"));

        String[] steps = {
            "Recibirá una llamada o mensaje de confirmación para coordinar el lugar y hora de entrega.",
            "En el momento de la entrega, presente su DUI o pasaporte y licencia de conducir originales.",
            "Se realizará una inspección del vehículo y se firmará el contrato de arrendamiento.",
            "En caso de necesitar cancelar o modificar su reserva, contáctenos con anticipación."
        };

        for (int i = 0; i < steps.length; i++) {
            doc.add(new Paragraph((i + 1) + ". " + steps[i])
                    .setFontSize(10).setMarginLeft(10).setMarginBottom(5));
        }
        doc.add(new Paragraph("").setMarginBottom(10));
    }

    // ── Footer ───────────────────────────────────────────────────────────────

    private void addFooter(Document doc, String companyName) {
        doc.add(new LineSeparator(new com.itextpdf.kernel.pdf.canvas.draw.SolidLine(0.8f))
                .setStrokeColor(GRAY_TEXT).setMarginTop(10).setMarginBottom(8));

        doc.add(new Paragraph("Este documento es una confirmación de reserva. No constituye un contrato de arrendamiento. " +
                "El contrato formal será firmado al momento de la entrega del vehículo.")
                .setFontSize(8).setFontColor(GRAY_TEXT).setItalic().setTextAlignment(TextAlignment.CENTER));

        doc.add(new Paragraph(companyName != null ? companyName : "Nova Rentacar")
                .setFontSize(8).setFontColor(GRAY_TEXT).setTextAlignment(TextAlignment.CENTER));
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private Paragraph sectionTitle(String text) {
        return new Paragraph(text)
                .setBold().setFontSize(11).setFontColor(DARK_BLUE)
                .setMarginBottom(6)
                .setBorderBottom(new SolidBorder(DARK_BLUE, 1));
    }

    private Table twoColTable() {
        return new Table(UnitValue.createPercentArray(new float[]{1.4f, 2f}))
                .setWidth(UnitValue.createPercentValue(100));
    }

    private void addRow(Table t, String label, String value) {
        t.addCell(new Cell().add(new Paragraph(label).setBold().setFontSize(10))
                .setBackgroundColor(LIGHT_GRAY).setBorder(Border.NO_BORDER).setPadding(5));
        t.addCell(new Cell().add(new Paragraph(value != null ? value : "—").setFontSize(10))
                .setBorder(Border.NO_BORDER).setPadding(5));
    }
}
