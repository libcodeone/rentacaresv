package com.rentacaresv.shared.util;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Utilidades para formateo de datos
 * Centraliza el formateo de precios, fechas, etc. para mantener consistencia
 */
public class FormatUtils {

    private static final Locale EL_SALVADOR_LOCALE = new Locale("es", "SV");
    private static final DecimalFormatSymbols SYMBOLS;
    private static final DecimalFormat PRICE_FORMAT;
    
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter DATETIME_SECONDS_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    static {
        // Configurar símbolos para El Salvador (USD)
        SYMBOLS = new DecimalFormatSymbols(EL_SALVADOR_LOCALE);
        SYMBOLS.setCurrencySymbol("$");
        SYMBOLS.setGroupingSeparator(',');
        SYMBOLS.setDecimalSeparator('.');
        
        // Formato: $1,234.56
        PRICE_FORMAT = new DecimalFormat("$#,##0.00", SYMBOLS);
    }

    /**
     * Formatea un precio en dólares estadounidenses (USD)
     * Ejemplo: 1234.56 -> "$1,234.56"
     */
    public static String formatPrice(BigDecimal price) {
        if (price == null) {
            return "$0.00";
        }
        return PRICE_FORMAT.format(price);
    }

    /**
     * Formatea un precio en dólares desde un double
     */
    public static String formatPrice(double price) {
        return formatPrice(BigDecimal.valueOf(price));
    }

    /**
     * Formatea un precio en dólares desde un int
     */
    public static String formatPrice(int price) {
        return formatPrice(BigDecimal.valueOf(price));
    }

    /**
     * Formatea una fecha
     * Ejemplo: 2024-12-28 -> "28/12/2024"
     */
    public static String formatDate(LocalDate date) {
        if (date == null) {
            return "-";
        }
        return date.format(DATE_FORMAT);
    }

    /**
     * Formatea una fecha y hora sin segundos
     * Ejemplo: 2024-12-28 14:30:00 -> "28/12/2024 14:30"
     */
    public static String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "-";
        }
        return dateTime.format(DATETIME_FORMAT);
    }

    /**
     * Formatea una fecha y hora con segundos
     * Ejemplo: 2024-12-28 14:30:45 -> "28/12/2024 14:30:45"
     */
    public static String formatDateTimeWithSeconds(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "-";
        }
        return dateTime.format(DATETIME_SECONDS_FORMAT);
    }

    /**
     * Formatea un número sin decimales con separadores de miles
     * Ejemplo: 1234567 -> "1,234,567"
     */
    public static String formatNumber(long number) {
        DecimalFormat format = new DecimalFormat("#,###", SYMBOLS);
        return format.format(number);
    }

    /**
     * Formatea un porcentaje
     * Ejemplo: 0.15 -> "15.00%"
     */
    public static String formatPercentage(BigDecimal percentage) {
        if (percentage == null) {
            return "0.00%";
        }
        DecimalFormat format = new DecimalFormat("#,##0.00", SYMBOLS);
        return format.format(percentage.multiply(BigDecimal.valueOf(100))) + "%";
    }

    /**
     * Obtiene el símbolo de moneda para El Salvador (USD)
     */
    public static String getCurrencySymbol() {
        return "$";
    }

    /**
     * Obtiene el código de moneda para El Salvador
     */
    public static String getCurrencyCode() {
        return "USD";
    }

    /**
     * Obtiene el locale de El Salvador
     */
    public static Locale getLocale() {
        return EL_SALVADOR_LOCALE;
    }
}
