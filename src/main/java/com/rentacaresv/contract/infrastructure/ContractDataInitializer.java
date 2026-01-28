package com.rentacaresv.contract.infrastructure;

import com.rentacaresv.contract.domain.AccessoryCatalog;
import com.rentacaresv.contract.domain.AccessoryCategory;
import com.rentacaresv.contract.domain.VehicleDiagram;
import com.rentacaresv.vehicle.domain.VehicleType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Inicializador de datos del módulo de contratos.
 * Crea los accesorios predeterminados basados en el contrato real de RentaCar.
 */
@Component
@Order(10) // Se ejecuta después de otros inicializadores
@RequiredArgsConstructor
@Slf4j
public class ContractDataInitializer implements CommandLineRunner {

    private final AccessoryCatalogRepository accessoryCatalogRepository;
    private final VehicleDiagramRepository vehicleDiagramRepository;

    @Override
    @Transactional
    public void run(String... args) {
        initializeAccessories();
        initializeVehicleDiagrams();
    }

    private void initializeAccessories() {
        if (accessoryCatalogRepository.count() > 0) {
            log.info("📋 Catálogo de accesorios ya inicializado ({} accesorios)", 
                    accessoryCatalogRepository.count());
            return;
        }

        log.info("📋 Inicializando catálogo de accesorios...");

        int order = 0;

        // ========================================
        // EXTERIOR
        // ========================================
        createAccessory("Antena", AccessoryCategory.EXTERIOR, ++order, true);
        createAccessory("Copas de ruedas", AccessoryCategory.EXTERIOR, ++order, true);
        createAccessory("Limpia parabrisas delantero", AccessoryCategory.EXTERIOR, ++order, true);
        createAccessory("Limpia parabrisas trasero", AccessoryCategory.EXTERIOR, ++order, false);
        createAccessory("Emblemas", AccessoryCategory.EXTERIOR, ++order, true);
        createAccessory("Espejos exteriores", AccessoryCategory.EXTERIOR, ++order, true);
        createAccessory("Manecillas exteriores", AccessoryCategory.EXTERIOR, ++order, true);
        createAccessory("Loderas delanteras", AccessoryCategory.EXTERIOR, ++order, false);
        createAccessory("Loderas traseras", AccessoryCategory.EXTERIOR, ++order, false);
        createAccessory("Tapón combustible", AccessoryCategory.EXTERIOR, ++order, true);
        createAccessory("Placas delantera y trasera", AccessoryCategory.EXTERIOR, ++order, true);

        // ========================================
        // INTERIOR
        // ========================================
        order = 0;
        createAccessory("Manecillas interiores", AccessoryCategory.INTERIOR, ++order, true);
        createAccessory("Espejos interiores", AccessoryCategory.INTERIOR, ++order, true);
        createAccessory("Tapicería en buen estado", AccessoryCategory.INTERIOR, ++order, true);
        createAccessory("Sobre alfombras", AccessoryCategory.INTERIOR, ++order, false);
        createAccessory("Perillas de tablero", AccessoryCategory.INTERIOR, ++order, true);
        createAccessory("Encendedor", AccessoryCategory.INTERIOR, ++order, false);
        createAccessory("CD Player / Radio", AccessoryCategory.INTERIOR, ++order, false);

        // ========================================
        // SEGURIDAD
        // ========================================
        order = 0;
        createAccessory("Extinguidor", AccessoryCategory.SEGURIDAD, ++order, true);
        createAccessory("Triángulo", AccessoryCategory.SEGURIDAD, ++order, true);
        createAccessory("Cinturones de seguridad", AccessoryCategory.SEGURIDAD, ++order, true);

        // ========================================
        // HERRAMIENTAS
        // ========================================
        order = 0;
        createAccessory("Llave con llavero", AccessoryCategory.HERRAMIENTAS, ++order, true);
        createAccessory("Llave de tuercas", AccessoryCategory.HERRAMIENTAS, ++order, true);
        createAccessory("Llanta de repuesto", AccessoryCategory.HERRAMIENTAS, ++order, true);
        createAccessory("Mica y palanca", AccessoryCategory.HERRAMIENTAS, ++order, true);

        // ========================================
        // DOCUMENTOS
        // ========================================
        order = 0;
        createAccessory("Tarjeta de circulación", AccessoryCategory.DOCUMENTOS, ++order, true);

        log.info("✅ Catálogo de accesorios inicializado con {} elementos", 
                accessoryCatalogRepository.count());
    }

    private void createAccessory(String name, AccessoryCategory category, int order, boolean mandatory) {
        AccessoryCatalog accessory = AccessoryCatalog.builder()
                .name(name)
                .category(category)
                .displayOrder(order)
                .isMandatory(mandatory)
                .isActive(true)
                .build();
        accessoryCatalogRepository.save(accessory);
    }

    private void initializeVehicleDiagrams() {
        if (vehicleDiagramRepository.count() > 0) {
            log.info("🚗 Diagramas de vehículos ya inicializados ({} diagramas)", 
                    vehicleDiagramRepository.count());
            return;
        }

        log.info("🚗 Inicializando diagramas de vehículos...");

        // Crear diagrama placeholder para cada tipo de vehículo
        // Los SVGs reales se pueden cargar después desde la UI de administración
        
        createDiagram(VehicleType.SEDAN, "Sedán", getSedanSvg());
        createDiagram(VehicleType.SUV, "SUV", getSuvSvg());
        createDiagram(VehicleType.PICKUP, "Pick Up", getPickupSvg());
        createDiagram(VehicleType.HATCHBACK, "Hatchback", getHatchbackSvg());
        createDiagram(VehicleType.MINIVAN, "Minivan", getMinivanSvg());
        createDiagram(VehicleType.VAN, "Van", getVanSvg());

        log.info("✅ Diagramas de vehículos inicializados con {} elementos", 
                vehicleDiagramRepository.count());
    }

    private void createDiagram(VehicleType type, String name, String svgContent) {
        VehicleDiagram diagram = VehicleDiagram.builder()
                .vehicleType(type)
                .name(name)
                .svgContent(svgContent)
                .width(800)
                .height(400)
                .isActive(true)
                .build();
        vehicleDiagramRepository.save(diagram);
    }

    // ========================================
    // SVGs de los vehículos (simplificados)
    // Estos son diagramas básicos, se pueden reemplazar con mejores SVGs
    // ========================================

    private String getSedanSvg() {
        return """
            <svg viewBox="0 0 800 400" xmlns="http://www.w3.org/2000/svg">
                <!-- Vista lateral -->
                <g id="side-view" transform="translate(50, 50)">
                    <text x="300" y="-10" text-anchor="middle" font-size="14" font-weight="bold">VISTA LATERAL</text>
                    <!-- Carrocería -->
                    <path d="M50,120 L100,120 L120,80 L280,80 L320,120 L380,120 L380,150 L50,150 Z" 
                          fill="none" stroke="#333" stroke-width="2"/>
                    <!-- Ventanas -->
                    <path d="M125,85 L145,120 L255,120 L275,85 Z" fill="none" stroke="#333" stroke-width="1"/>
                    <!-- Ruedas -->
                    <circle cx="100" cy="150" r="25" fill="none" stroke="#333" stroke-width="2"/>
                    <circle cx="330" cy="150" r="25" fill="none" stroke="#333" stroke-width="2"/>
                </g>
                <!-- Vista superior -->
                <g id="top-view" transform="translate(450, 50)">
                    <text x="150" y="-10" text-anchor="middle" font-size="14" font-weight="bold">VISTA SUPERIOR</text>
                    <rect x="50" y="20" width="200" height="120" rx="20" fill="none" stroke="#333" stroke-width="2"/>
                    <!-- Ventanas -->
                    <rect x="70" y="40" width="160" height="30" rx="5" fill="none" stroke="#333" stroke-width="1"/>
                    <rect x="70" y="90" width="160" height="30" rx="5" fill="none" stroke="#333" stroke-width="1"/>
                </g>
                <!-- Vista frontal -->
                <g id="front-view" transform="translate(50, 220)">
                    <text x="100" y="-10" text-anchor="middle" font-size="14" font-weight="bold">VISTA FRONTAL</text>
                    <rect x="30" y="20" width="140" height="80" rx="10" fill="none" stroke="#333" stroke-width="2"/>
                    <!-- Parabrisas -->
                    <rect x="50" y="25" width="100" height="35" rx="5" fill="none" stroke="#333" stroke-width="1"/>
                    <!-- Faros -->
                    <ellipse cx="50" cy="80" rx="15" ry="10" fill="none" stroke="#333" stroke-width="1"/>
                    <ellipse cx="150" cy="80" rx="15" ry="10" fill="none" stroke="#333" stroke-width="1"/>
                </g>
                <!-- Vista trasera -->
                <g id="rear-view" transform="translate(300, 220)">
                    <text x="100" y="-10" text-anchor="middle" font-size="14" font-weight="bold">VISTA TRASERA</text>
                    <rect x="30" y="20" width="140" height="80" rx="10" fill="none" stroke="#333" stroke-width="2"/>
                    <!-- Ventana trasera -->
                    <rect x="50" y="25" width="100" height="30" rx="5" fill="none" stroke="#333" stroke-width="1"/>
                    <!-- Luces -->
                    <rect x="35" y="70" width="25" height="15" rx="3" fill="none" stroke="#333" stroke-width="1"/>
                    <rect x="140" y="70" width="25" height="15" rx="3" fill="none" stroke="#333" stroke-width="1"/>
                </g>
            </svg>
            """;
    }

    private String getSuvSvg() {
        return """
            <svg viewBox="0 0 800 400" xmlns="http://www.w3.org/2000/svg">
                <!-- Vista lateral SUV -->
                <g id="side-view" transform="translate(50, 50)">
                    <text x="300" y="-10" text-anchor="middle" font-size="14" font-weight="bold">VISTA LATERAL</text>
                    <!-- Carrocería más alta -->
                    <path d="M50,100 L80,100 L100,60 L300,60 L340,100 L400,100 L400,150 L50,150 Z" 
                          fill="none" stroke="#333" stroke-width="2"/>
                    <!-- Ventanas -->
                    <path d="M105,65 L120,100 L290,100 L305,65 Z" fill="none" stroke="#333" stroke-width="1"/>
                    <!-- Ruedas más grandes -->
                    <circle cx="110" cy="150" r="30" fill="none" stroke="#333" stroke-width="2"/>
                    <circle cx="340" cy="150" r="30" fill="none" stroke="#333" stroke-width="2"/>
                </g>
                <!-- Vista superior -->
                <g id="top-view" transform="translate(450, 50)">
                    <text x="150" y="-10" text-anchor="middle" font-size="14" font-weight="bold">VISTA SUPERIOR</text>
                    <rect x="40" y="10" width="220" height="140" rx="15" fill="none" stroke="#333" stroke-width="2"/>
                    <rect x="60" y="30" width="180" height="40" rx="5" fill="none" stroke="#333" stroke-width="1"/>
                    <rect x="60" y="90" width="180" height="40" rx="5" fill="none" stroke="#333" stroke-width="1"/>
                </g>
                <!-- Vista frontal -->
                <g id="front-view" transform="translate(50, 220)">
                    <text x="100" y="-10" text-anchor="middle" font-size="14" font-weight="bold">VISTA FRONTAL</text>
                    <rect x="20" y="10" width="160" height="100" rx="10" fill="none" stroke="#333" stroke-width="2"/>
                    <rect x="40" y="15" width="120" height="40" rx="5" fill="none" stroke="#333" stroke-width="1"/>
                    <ellipse cx="45" cy="85" rx="18" ry="12" fill="none" stroke="#333" stroke-width="1"/>
                    <ellipse cx="155" cy="85" rx="18" ry="12" fill="none" stroke="#333" stroke-width="1"/>
                </g>
                <!-- Vista trasera -->
                <g id="rear-view" transform="translate(300, 220)">
                    <text x="100" y="-10" text-anchor="middle" font-size="14" font-weight="bold">VISTA TRASERA</text>
                    <rect x="20" y="10" width="160" height="100" rx="10" fill="none" stroke="#333" stroke-width="2"/>
                    <rect x="40" y="15" width="120" height="35" rx="5" fill="none" stroke="#333" stroke-width="1"/>
                    <rect x="30" y="80" width="30" height="18" rx="3" fill="none" stroke="#333" stroke-width="1"/>
                    <rect x="140" y="80" width="30" height="18" rx="3" fill="none" stroke="#333" stroke-width="1"/>
                </g>
            </svg>
            """;
    }

    private String getPickupSvg() {
        return """
            <svg viewBox="0 0 800 400" xmlns="http://www.w3.org/2000/svg">
                <!-- Vista lateral Pickup -->
                <g id="side-view" transform="translate(50, 50)">
                    <text x="300" y="-10" text-anchor="middle" font-size="14" font-weight="bold">VISTA LATERAL</text>
                    <!-- Cabina -->
                    <path d="M50,100 L80,100 L100,60 L200,60 L220,100" fill="none" stroke="#333" stroke-width="2"/>
                    <!-- Caja/Platón -->
                    <path d="M220,80 L400,80 L400,150 L50,150 L50,100" fill="none" stroke="#333" stroke-width="2"/>
                    <line x1="220" y1="80" x2="220" y2="150" stroke="#333" stroke-width="2"/>
                    <!-- Ventana cabina -->
                    <path d="M105,65 L120,100 L195,100 L205,65 Z" fill="none" stroke="#333" stroke-width="1"/>
                    <!-- Ruedas -->
                    <circle cx="110" cy="150" r="30" fill="none" stroke="#333" stroke-width="2"/>
                    <circle cx="350" cy="150" r="30" fill="none" stroke="#333" stroke-width="2"/>
                </g>
                <!-- Vista superior -->
                <g id="top-view" transform="translate(450, 50)">
                    <text x="150" y="-10" text-anchor="middle" font-size="14" font-weight="bold">VISTA SUPERIOR</text>
                    <!-- Cabina -->
                    <rect x="40" y="40" width="100" height="80" rx="10" fill="none" stroke="#333" stroke-width="2"/>
                    <!-- Platón -->
                    <rect x="140" y="30" width="120" height="100" fill="none" stroke="#333" stroke-width="2"/>
                </g>
                <!-- Vista frontal -->
                <g id="front-view" transform="translate(50, 220)">
                    <text x="100" y="-10" text-anchor="middle" font-size="14" font-weight="bold">VISTA FRONTAL</text>
                    <rect x="20" y="10" width="160" height="100" rx="10" fill="none" stroke="#333" stroke-width="2"/>
                    <rect x="40" y="15" width="120" height="40" rx="5" fill="none" stroke="#333" stroke-width="1"/>
                    <ellipse cx="45" cy="85" rx="18" ry="12" fill="none" stroke="#333" stroke-width="1"/>
                    <ellipse cx="155" cy="85" rx="18" ry="12" fill="none" stroke="#333" stroke-width="1"/>
                </g>
                <!-- Vista trasera -->
                <g id="rear-view" transform="translate(300, 220)">
                    <text x="100" y="-10" text-anchor="middle" font-size="14" font-weight="bold">VISTA TRASERA</text>
                    <rect x="20" y="10" width="160" height="100" fill="none" stroke="#333" stroke-width="2"/>
                    <rect x="30" y="80" width="25" height="15" rx="3" fill="none" stroke="#333" stroke-width="1"/>
                    <rect x="145" y="80" width="25" height="15" rx="3" fill="none" stroke="#333" stroke-width="1"/>
                </g>
            </svg>
            """;
    }

    private String getHatchbackSvg() {
        return """
            <svg viewBox="0 0 800 400" xmlns="http://www.w3.org/2000/svg">
                <!-- Vista lateral Hatchback -->
                <g id="side-view" transform="translate(50, 50)">
                    <text x="300" y="-10" text-anchor="middle" font-size="14" font-weight="bold">VISTA LATERAL</text>
                    <path d="M50,120 L100,120 L120,80 L260,80 L300,120 L350,120 L350,150 L50,150 Z" 
                          fill="none" stroke="#333" stroke-width="2"/>
                    <path d="M125,85 L145,120 L240,120 L265,85 Z" fill="none" stroke="#333" stroke-width="1"/>
                    <circle cx="100" cy="150" r="25" fill="none" stroke="#333" stroke-width="2"/>
                    <circle cx="300" cy="150" r="25" fill="none" stroke="#333" stroke-width="2"/>
                </g>
                <!-- Vista superior -->
                <g id="top-view" transform="translate(450, 50)">
                    <text x="150" y="-10" text-anchor="middle" font-size="14" font-weight="bold">VISTA SUPERIOR</text>
                    <rect x="60" y="25" width="180" height="110" rx="20" fill="none" stroke="#333" stroke-width="2"/>
                    <rect x="80" y="45" width="140" height="25" rx="5" fill="none" stroke="#333" stroke-width="1"/>
                    <rect x="80" y="90" width="140" height="25" rx="5" fill="none" stroke="#333" stroke-width="1"/>
                </g>
                <!-- Vista frontal -->
                <g id="front-view" transform="translate(50, 220)">
                    <text x="100" y="-10" text-anchor="middle" font-size="14" font-weight="bold">VISTA FRONTAL</text>
                    <rect x="30" y="20" width="140" height="80" rx="10" fill="none" stroke="#333" stroke-width="2"/>
                    <rect x="50" y="25" width="100" height="35" rx="5" fill="none" stroke="#333" stroke-width="1"/>
                    <ellipse cx="50" cy="80" rx="15" ry="10" fill="none" stroke="#333" stroke-width="1"/>
                    <ellipse cx="150" cy="80" rx="15" ry="10" fill="none" stroke="#333" stroke-width="1"/>
                </g>
                <!-- Vista trasera -->
                <g id="rear-view" transform="translate(300, 220)">
                    <text x="100" y="-10" text-anchor="middle" font-size="14" font-weight="bold">VISTA TRASERA</text>
                    <rect x="30" y="20" width="140" height="80" rx="10" fill="none" stroke="#333" stroke-width="2"/>
                    <rect x="50" y="25" width="100" height="40" rx="5" fill="none" stroke="#333" stroke-width="1"/>
                    <rect x="35" y="75" width="25" height="15" rx="3" fill="none" stroke="#333" stroke-width="1"/>
                    <rect x="140" y="75" width="25" height="15" rx="3" fill="none" stroke="#333" stroke-width="1"/>
                </g>
            </svg>
            """;
    }

    private String getMinivanSvg() {
        return """
            <svg viewBox="0 0 800 400" xmlns="http://www.w3.org/2000/svg">
                <!-- Vista lateral Minivan -->
                <g id="side-view" transform="translate(50, 50)">
                    <text x="300" y="-10" text-anchor="middle" font-size="14" font-weight="bold">VISTA LATERAL</text>
                    <path d="M50,90 L70,90 L90,50 L350,50 L380,90 L420,90 L420,150 L50,150 Z" 
                          fill="none" stroke="#333" stroke-width="2"/>
                    <path d="M95,55 L110,90 L200,90 L200,55 Z" fill="none" stroke="#333" stroke-width="1"/>
                    <path d="M210,55 L210,90 L340,90 L355,55 Z" fill="none" stroke="#333" stroke-width="1"/>
                    <circle cx="110" cy="150" r="28" fill="none" stroke="#333" stroke-width="2"/>
                    <circle cx="360" cy="150" r="28" fill="none" stroke="#333" stroke-width="2"/>
                </g>
                <!-- Vista superior -->
                <g id="top-view" transform="translate(450, 50)">
                    <text x="150" y="-10" text-anchor="middle" font-size="14" font-weight="bold">VISTA SUPERIOR</text>
                    <rect x="30" y="10" width="240" height="140" rx="15" fill="none" stroke="#333" stroke-width="2"/>
                    <rect x="50" y="25" width="200" height="35" rx="5" fill="none" stroke="#333" stroke-width="1"/>
                    <rect x="50" y="70" width="200" height="35" rx="5" fill="none" stroke="#333" stroke-width="1"/>
                    <rect x="50" y="115" width="200" height="25" rx="5" fill="none" stroke="#333" stroke-width="1"/>
                </g>
                <!-- Vista frontal -->
                <g id="front-view" transform="translate(50, 220)">
                    <text x="100" y="-10" text-anchor="middle" font-size="14" font-weight="bold">VISTA FRONTAL</text>
                    <rect x="15" y="5" width="170" height="110" rx="10" fill="none" stroke="#333" stroke-width="2"/>
                    <rect x="35" y="10" width="130" height="50" rx="5" fill="none" stroke="#333" stroke-width="1"/>
                    <ellipse cx="40" cy="90" rx="18" ry="12" fill="none" stroke="#333" stroke-width="1"/>
                    <ellipse cx="160" cy="90" rx="18" ry="12" fill="none" stroke="#333" stroke-width="1"/>
                </g>
                <!-- Vista trasera -->
                <g id="rear-view" transform="translate(300, 220)">
                    <text x="100" y="-10" text-anchor="middle" font-size="14" font-weight="bold">VISTA TRASERA</text>
                    <rect x="15" y="5" width="170" height="110" rx="10" fill="none" stroke="#333" stroke-width="2"/>
                    <rect x="35" y="10" width="130" height="45" rx="5" fill="none" stroke="#333" stroke-width="1"/>
                    <rect x="25" y="85" width="30" height="20" rx="3" fill="none" stroke="#333" stroke-width="1"/>
                    <rect x="145" y="85" width="30" height="20" rx="3" fill="none" stroke="#333" stroke-width="1"/>
                </g>
            </svg>
            """;
    }

    private String getVanSvg() {
        return """
            <svg viewBox="0 0 800 400" xmlns="http://www.w3.org/2000/svg">
                <!-- Vista lateral Van -->
                <g id="side-view" transform="translate(50, 50)">
                    <text x="300" y="-10" text-anchor="middle" font-size="14" font-weight="bold">VISTA LATERAL</text>
                    <path d="M50,50 L400,50 L400,150 L50,150 Z" fill="none" stroke="#333" stroke-width="2"/>
                    <rect x="60" y="60" width="80" height="50" rx="5" fill="none" stroke="#333" stroke-width="1"/>
                    <rect x="160" y="60" width="60" height="50" rx="5" fill="none" stroke="#333" stroke-width="1"/>
                    <rect x="240" y="60" width="60" height="50" rx="5" fill="none" stroke="#333" stroke-width="1"/>
                    <circle cx="110" cy="150" r="30" fill="none" stroke="#333" stroke-width="2"/>
                    <circle cx="350" cy="150" r="30" fill="none" stroke="#333" stroke-width="2"/>
                </g>
                <!-- Vista superior -->
                <g id="top-view" transform="translate(450, 50)">
                    <text x="150" y="-10" text-anchor="middle" font-size="14" font-weight="bold">VISTA SUPERIOR</text>
                    <rect x="20" y="10" width="260" height="140" rx="10" fill="none" stroke="#333" stroke-width="2"/>
                </g>
                <!-- Vista frontal -->
                <g id="front-view" transform="translate(50, 220)">
                    <text x="100" y="-10" text-anchor="middle" font-size="14" font-weight="bold">VISTA FRONTAL</text>
                    <rect x="10" y="5" width="180" height="115" rx="8" fill="none" stroke="#333" stroke-width="2"/>
                    <rect x="30" y="10" width="140" height="55" rx="5" fill="none" stroke="#333" stroke-width="1"/>
                    <ellipse cx="35" cy="95" rx="18" ry="12" fill="none" stroke="#333" stroke-width="1"/>
                    <ellipse cx="165" cy="95" rx="18" ry="12" fill="none" stroke="#333" stroke-width="1"/>
                </g>
                <!-- Vista trasera -->
                <g id="rear-view" transform="translate(300, 220)">
                    <text x="100" y="-10" text-anchor="middle" font-size="14" font-weight="bold">VISTA TRASERA</text>
                    <rect x="10" y="5" width="180" height="115" rx="8" fill="none" stroke="#333" stroke-width="2"/>
                    <rect x="30" y="10" width="140" height="50" rx="5" fill="none" stroke="#333" stroke-width="1"/>
                    <rect x="20" y="90" width="35" height="20" rx="3" fill="none" stroke="#333" stroke-width="1"/>
                    <rect x="145" y="90" width="35" height="20" rx="3" fill="none" stroke="#333" stroke-width="1"/>
                </g>
            </svg>
            """;
    }
}
