package com.rentacaresv.shared.storage;

/**
 * Enum para los tipos de carpetas en Digital Ocean Spaces
 */
public enum FolderType {
    PROFILE_PHOTOS("profile_photos"),
    CARS("cars"),
    CAR_DETAILS("car_details"),
    LOGOS("logos"),
    SETTINGS("settings"),
    CONTRACTS("contracts"),           // Carpeta principal de contratos
    CONTRACT_SIGNATURES("contracts/signatures"),  // Firmas digitales
    CONTRACT_DOCUMENTS("contracts/documents"),    // Fotos de documentos de identidad
    CONTRACT_DIAGRAMS("contracts/diagrams"),      // Diagramas con marcas de daños (legacy)
    CONTRACT_PDFS("contracts/pdfs"),              // PDFs generados
    CONTRACT_VIDEOS("contracts/videos");          // Videos de estado del vehículo

    private final String folderName;

    FolderType(String folderName) {
        this.folderName = folderName;
    }

    public String getFolderName() {
        return folderName;
    }
}
