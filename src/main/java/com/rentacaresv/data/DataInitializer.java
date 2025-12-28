package com.rentacaresv.data;

import com.rentacaresv.customer.application.CreateCustomerCommand;
import com.rentacaresv.customer.application.CustomerService;
import com.rentacaresv.security.Role;
import com.rentacaresv.user.domain.User;
import com.rentacaresv.user.infrastructure.UserRepository;
import com.rentacaresv.vehicle.application.CreateVehicleCommand;
import com.rentacaresv.vehicle.application.VehicleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

/**
 * Inicializador de datos del sistema
 * Crea usuarios, vehículos y clientes por defecto si la base de datos está vacía
 */
@Configuration
@Slf4j
@RequiredArgsConstructor
public class DataInitializer {

    @Bean
    public CommandLineRunner initData(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            VehicleService vehicleService,
            CustomerService customerService) {
        
        return args -> {
            initializeUsers(userRepository, passwordEncoder);
            initializeVehicles(vehicleService);
            initializeCustomers(customerService);
        };
    }

    private void initializeUsers(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        if (userRepository.count() == 0) {
            log.info("📝 Creando usuarios por defecto...");

            User operator = User.builder()
                    .username("operator")
                    .name("Operador Demo")
                    .email("operator@rentacar.com")
                    .hashedPassword(passwordEncoder.encode("operator123"))
                    .roles(Set.of(Role.OPERATOR))
                    .active(true)
                    .build();
            userRepository.save(operator);

            User admin = User.builder()
                    .username("admin")
                    .name("Administrador")
                    .email("admin@rentacar.com")
                    .hashedPassword(passwordEncoder.encode("admin123"))
                    .roles(Set.of(Role.ADMIN))
                    .active(true)
                    .build();
            userRepository.save(admin);

            log.info("✅ Usuarios creados:");
            log.info("   👤 Operador: operator / operator123");
            log.info("   👑 Admin:    admin / admin123");
        }
    }

    private void initializeVehicles(VehicleService vehicleService) {
        if (vehicleService.findAll().isEmpty()) {
            log.info("📝 Creando vehículos de prueba...");

            createVehicle(vehicleService,
                    "P123456", "Toyota", "Corolla", 2023, "Blanco",
                    "AUTOMATIC", "GASOLINE", 5, 15000,
                    new BigDecimal("35.00"), new BigDecimal("30.00"),
                    new BigDecimal("28.00"), new BigDecimal("800.00"),
                    "Vehículo en excelente estado");

            createVehicle(vehicleService,
                    "P234567", "Honda", "CR-V", 2022, "Negro",
                    "AUTOMATIC", "GASOLINE", 5, 25000,
                    new BigDecimal("50.00"), new BigDecimal("45.00"),
                    new BigDecimal("40.00"), new BigDecimal("1200.00"),
                    "SUV espaciosa y confortable");

            createVehicle(vehicleService,
                    "P345678", "Hyundai", "Accent", 2024, "Rojo",
                    "MANUAL", "GASOLINE", 5, 5000,
                    new BigDecimal("30.00"), new BigDecimal("27.00"),
                    new BigDecimal("25.00"), new BigDecimal("700.00"),
                    "Auto nuevo, ideal para ciudad");

            createVehicle(vehicleService,
                    "P456789", "Ford", "Ranger", 2023, "Gris",
                    "AUTOMATIC", "DIESEL", 5, 18000,
                    new BigDecimal("60.00"), new BigDecimal("55.00"),
                    new BigDecimal("50.00"), new BigDecimal("1500.00"),
                    "Pickup doble cabina 4x4");

            createVehicle(vehicleService,
                    "P567890", "Tesla", "Model 3", 2024, "Azul",
                    "AUTOMATIC", "ELECTRIC", 5, 8000,
                    new BigDecimal("75.00"), new BigDecimal("70.00"),
                    new BigDecimal("65.00"), new BigDecimal("1800.00"),
                    "Vehículo eléctrico premium");

            log.info("✅ 5 vehículos de prueba creados");
        }
    }

    private void initializeCustomers(CustomerService customerService) {
        if (customerService.findAll().isEmpty()) {
            log.info("📝 Creando clientes de prueba...");

            // Cliente VIP
            createCustomer(customerService,
                    "María José González",
                    "DUI", "12345678-9",
                    "maria.gonzalez@email.com",
                    "7123-4567",
                    "Col. Escalón, San Salvador",
                    LocalDate.of(1985, 3, 15),
                    "VIP",
                    "Cliente frecuente, excelente historial");

            // Cliente Normal
            createCustomer(customerService,
                    "Carlos Antonio Martínez",
                    "DUI", "98765432-1",
                    "carlos.martinez@email.com",
                    "7234-5678",
                    "Col. San Benito, San Salvador",
                    LocalDate.of(1990, 7, 22),
                    "NORMAL",
                    null);

            // Cliente con pasaporte
            createCustomer(customerService,
                    "Ana Patricia Rivera",
                    "PASSPORT", "AB123456",
                    "ana.rivera@email.com",
                    "7345-6789",
                    "Santa Tecla, La Libertad",
                    LocalDate.of(1988, 11, 8),
                    "NORMAL",
                    "Cliente extranjera");

            // Cliente VIP 2
            createCustomer(customerService,
                    "Roberto Carlos Flores",
                    "DUI", "11223344-5",
                    "roberto.flores@empresa.com",
                    "7456-7890",
                    "Santa Elena, Antiguo Cuscatlán",
                    LocalDate.of(1980, 5, 30),
                    "VIP",
                    "Empresario, rentas mensuales");

            // Cliente joven
            createCustomer(customerService,
                    "Sofía Valentina López",
                    "DRIVERS_LICENSE", "DL-987654",
                    "sofia.lopez@email.com",
                    "7567-8901",
                    "Soyapango, San Salvador",
                    LocalDate.of(1998, 12, 25),
                    "NORMAL",
                    "Primera renta");

            log.info("✅ 5 clientes de prueba creados (2 VIP, 3 Normal)");
            log.info("===========================================");
        }
    }

    private void createVehicle(VehicleService service,
                                String plate, String brand, String model, Integer year, String color,
                                String transmission, String fuel, Integer capacity, Integer mileage,
                                BigDecimal priceNormal, BigDecimal priceVip,
                                BigDecimal price15Days, BigDecimal priceMonthly,
                                String notes) {
        try {
            CreateVehicleCommand command = CreateVehicleCommand.builder()
                    .licensePlate(plate)
                    .brand(brand)
                    .model(model)
                    .year(year)
                    .color(color)
                    .transmissionType(transmission)
                    .fuelType(fuel)
                    .passengerCapacity(capacity)
                    .mileage(mileage)
                    .priceNormal(priceNormal)
                    .priceVip(priceVip)
                    .priceMoreThan15Days(price15Days)
                    .priceMonthly(priceMonthly)
                    .notes(notes)
                    .build();

            service.createVehicle(command);
            log.info("   🚗 {} {} {} - {}", brand, model, year, plate);
        } catch (Exception e) {
            log.error("   ❌ Error creando vehículo {}: {}", plate, e.getMessage());
        }
    }

    private void createCustomer(CustomerService service,
                                 String fullName,
                                 String docType, String docNumber,
                                 String email, String phone, String address,
                                 LocalDate birthDate,
                                 String category,
                                 String notes) {
        try {
            CreateCustomerCommand command = CreateCustomerCommand.builder()
                    .fullName(fullName)
                    .documentType(docType)
                    .documentNumber(docNumber)
                    .email(email)
                    .phone(phone)
                    .address(address)
                    .birthDate(birthDate)
                    .category(category)
                    .notes(notes)
                    .build();

            service.createCustomer(command);
            String categoryLabel = category.equals("VIP") ? "⭐ VIP" : "👤";
            log.info("   {} {} - {}", categoryLabel, fullName, docNumber);
        } catch (Exception e) {
            log.error("   ❌ Error creando cliente {}: {}", fullName, e.getMessage());
        }
    }
}
