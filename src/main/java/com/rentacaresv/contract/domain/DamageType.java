package com.rentacaresv.contract.domain;

/**
 * Tipos de daño que se pueden marcar en el diagrama del vehículo
 */
public enum DamageType {
    
    SCRATCH("Rayón", "R", "#FFA500"),           // Naranja
    DENT("Abolladura", "A", "#FF0000"),          // Rojo
    CRACK("Grieta", "G", "#800080"),             // Púrpura
    BROKEN("Roto", "X", "#000000"),              // Negro
    MISSING("Faltante", "F", "#0000FF"),         // Azul
    PAINT_DAMAGE("Daño pintura", "P", "#FF69B4"), // Rosa
    RUST("Óxido", "O", "#8B4513"),               // Marrón
    STAIN("Mancha", "M", "#808080"),             // Gris
    OTHER("Otro", "?", "#696969");               // Gris oscuro

    private final String label;
    private final String symbol;
    private final String color;

    DamageType(String label, String symbol, String color) {
        this.label = label;
        this.symbol = symbol;
        this.color = color;
    }

    public String getLabel() {
        return label;
    }

    /**
     * Símbolo corto para mostrar en el diagrama
     */
    public String getSymbol() {
        return symbol;
    }

    /**
     * Color para representar en el diagrama
     */
    public String getColor() {
        return color;
    }
}
