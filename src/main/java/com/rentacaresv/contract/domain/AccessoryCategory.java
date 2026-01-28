package com.rentacaresv.contract.domain;

/**
 * Categorías de accesorios para agrupar en el checklist
 */
public enum AccessoryCategory {
    
    EXTERIOR("Exterior", 1),
    INTERIOR("Interior", 2),
    SEGURIDAD("Seguridad", 3),
    DOCUMENTOS("Documentos", 4),
    HERRAMIENTAS("Herramientas", 5),
    ELECTRONICO("Electrónico", 6),
    OTROS("Otros", 99);

    private final String label;
    private final int order;

    AccessoryCategory(String label, int order) {
        this.label = label;
        this.order = order;
    }

    public String getLabel() {
        return label;
    }

    public int getOrder() {
        return order;
    }
}
