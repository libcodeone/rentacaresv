package com.rentacaresv.contract.infrastructure;

import com.rentacaresv.contract.domain.AccessoryCatalog;
import com.rentacaresv.contract.domain.AccessoryCategory;
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

    @Override
    @Transactional
    public void run(String... args) {
        initializeAccessories();
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

}
