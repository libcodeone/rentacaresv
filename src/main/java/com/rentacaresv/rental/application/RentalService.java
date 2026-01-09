package com.rentacaresv.rental.application;

import com.rentacaresv.customer.domain.Customer;
import com.rentacaresv.customer.infrastructure.CustomerRepository;
import com.rentacaresv.rental.domain.Rental;
import com.rentacaresv.rental.domain.RentalPriceCalculator;
import com.rentacaresv.rental.domain.RentalStatus;
import com.rentacaresv.rental.infrastructure.RentalMapper;
import com.rentacaresv.rental.infrastructure.RentalRepository;
import com.rentacaresv.vehicle.domain.Vehicle;
import com.rentacaresv.vehicle.infrastructure.VehicleRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Servicio de aplicación para Rental
 * Orquesta los casos de uso relacionados con rentas
 */
@Service
@Transactional
@Validated
@RequiredArgsConstructor
@Slf4j
public class RentalService {

    private final RentalRepository rentalRepository;
    private final VehicleRepository vehicleRepository;
    private final CustomerRepository customerRepository;
    private final RentalMapper rentalMapper;

    // Domain Service (Java puro, sin @Service)
    private final RentalPriceCalculator priceCalculator = new RentalPriceCalculator();

    /**
     * Crea una nueva renta
     */
    public RentalDTO createRental(@Valid CreateRentalCommand command) {
        log.info("Creando renta para vehículo {} y cliente {}",
                command.getVehicleId(), command.getCustomerId());

        // Validación adicional
        command.validate();

        // 1. Obtener entidades
        Vehicle vehicle = vehicleRepository.findById(command.getVehicleId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Vehículo no encontrado con ID: " + command.getVehicleId()));

        Customer customer = customerRepository.findById(command.getCustomerId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Cliente no encontrado con ID: " + command.getCustomerId()));

        // 2. Validar disponibilidad del vehículo
        if (!vehicle.isAvailable()) {
            throw new IllegalStateException(
                    "El vehículo " + vehicle.getLicensePlate() + " no está disponible");
        }

        // 3. Validar que el cliente esté activo
        if (!customer.isActiveCustomer()) {
            throw new IllegalStateException(
                    "El cliente " + customer.getFullName() + " no está activo");
        }

        // 4. Calcular precio usando Domain Service
        int days = priceCalculator.calculateDays(command.getStartDate(), command.getEndDate());
        BigDecimal dailyRate = priceCalculator.selectDailyRate(vehicle, customer, days);
        BigDecimal totalAmount = priceCalculator.calculateTotalPrice(
                vehicle, customer, command.getStartDate(), command.getEndDate());

        // 5. Generar número de contrato
        String contractNumber = generateContractNumber();

        // 6. Crear entidad de dominio
        Rental rental = Rental.builder()
                .contractNumber(contractNumber)
                .vehicle(vehicle)
                .customer(customer)
                .startDate(command.getStartDate())
                .endDate(command.getEndDate())
                .dailyRate(dailyRate)
                .totalDays(days)
                .totalAmount(totalAmount)
                .amountPaid(BigDecimal.ZERO)
                .status(RentalStatus.PENDING)
                .notes(command.getNotes())
                .build();

        // 7. Persistir
        rental = rentalRepository.save(rental);

        log.info("Renta creada exitosamente: {} - {} días - ${}",
                contractNumber, days, totalAmount);

        return rentalMapper.toDTO(rental);
    }

    /**
     * Entrega el vehículo al cliente (inicia la renta) - CON NOTAS
     */
    public void deliverRental(Long rentalId, String notes) {
        log.info("Entregando vehículo de la renta {}", rentalId);

        Rental rental = rentalRepository.findById(rentalId)
                .orElseThrow(() -> new IllegalArgumentException("Renta no encontrada"));

        rental.deliverVehicle(); // Lógica de dominio

        // Agregar notas de entrega si existen
        if (notes != null && !notes.isBlank()) {
            String currentNotes = rental.getNotes() != null ? rental.getNotes() : "";
            rental.setNotes(currentNotes + "\n\n=== ENTREGA ===\n" + notes);
        }

        rentalRepository.save(rental);

        log.info("Vehículo {} entregado a {}",
                rental.getVehicle().getLicensePlate(),
                rental.getCustomer().getFullName());
    }

    /**
     * Entrega el vehículo al cliente (inicia la renta)
     */
    public void deliverVehicle(Long rentalId) {
        deliverRental(rentalId, null);
    }

    /**
     * Devuelve el vehículo (finaliza la renta) - CON NOTAS Y MANTENIMIENTO
     */
    public void returnRental(Long rentalId, String notes, boolean needsMaintenance) {
        log.info("Devolviendo vehículo de la renta {}", rentalId);

        Rental rental = rentalRepository.findById(rentalId)
                .orElseThrow(() -> new IllegalArgumentException("Renta no encontrada"));

        rental.returnVehicle(); // Lógica de dominio

        // Agregar notas de devolución si existen
        if (notes != null && !notes.isBlank()) {
            String currentNotes = rental.getNotes() != null ? rental.getNotes() : "";
            rental.setNotes(currentNotes + "\n\n=== DEVOLUCIÓN ===\n" + notes);
        }

        // Si requiere mantenimiento, cambiar estado del vehículo
        if (needsMaintenance) {
            Vehicle vehicle = rental.getVehicle();
            vehicle.sendToMaintenance();
            vehicleRepository.save(vehicle);
            log.warn("Vehículo {} enviado a mantenimiento", vehicle.getLicensePlate());
        }

        rentalRepository.save(rental);

        log.info("Vehículo {} devuelto. Renta completada.",
                rental.getVehicle().getLicensePlate());

        // Verificar si hay retraso
        if (rental.isDelayed()) {
            log.warn("Renta {} devuelta con {} días de retraso. Penalidad: ${}",
                    rental.getContractNumber(),
                    rental.getDelayDays(),
                    rental.calculateDelayPenalty());
        }
    }

    /**
     * Devuelve el vehículo (finaliza la renta)
     */
    public void returnVehicle(Long rentalId) {
        returnRental(rentalId, null, false);
    }

    /**
     * Cancela una renta
     */
    public void cancelRental(Long rentalId) {
        log.info("Cancelando renta {}", rentalId);

        Rental rental = rentalRepository.findById(rentalId)
                .orElseThrow(() -> new IllegalArgumentException("Renta no encontrada"));

        rental.cancel(); // Lógica de dominio
        rentalRepository.save(rental);
    }

    /**
     * Registra un pago
     */
    public void registerPayment(@Valid RegisterPaymentCommand command) {
        log.info("Registrando pago de ${} para renta {}",
                command.getAmount(), command.getRentalId());

        Rental rental = rentalRepository.findById(command.getRentalId())
                .orElseThrow(() -> new IllegalArgumentException("Renta no encontrada"));

        rental.registerPayment(command.getAmount()); // Lógica de dominio
        rentalRepository.save(rental);

        log.info("Pago registrado. Nuevo saldo: ${}", rental.getBalance());
    }

    /**
     * Obtiene una renta por ID
     */
    @Transactional(readOnly = true)
    public RentalDTO findById(Long id) {
        Rental rental = rentalRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Renta no encontrada con ID: " + id));
        return rentalMapper.toDTO(rental);
    }

    /**
     * Obtiene la entidad Rental por ID (para generación de PDF)
     * Carga todas las relaciones necesarias (Customer, Vehicle)
     */
    @Transactional(readOnly = true)
    public Rental findRentalEntityById(Long id) {
        Rental rental = rentalRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Renta no encontrada con ID: " + id));

        // Forzar carga de relaciones lazy (para uso fuera de transacción)
        rental.getCustomer().getFullName(); // Inicializa Customer
        rental.getVehicle().getBrand(); // Inicializa Vehicle

        return rental;
    }

    /**
     * Obtiene todas las rentas activas
     */
    @Transactional(readOnly = true)
    public List<RentalDTO> findAll() {
        List<Rental> rentals = rentalRepository.findAllActive();
        return rentalMapper.toDTOList(rentals);
    }

    /**
     * Obtiene rentas por estado
     */
    @Transactional(readOnly = true)
    public List<RentalDTO> findByStatus(RentalStatus status) {
        List<Rental> rentals = rentalRepository.findByStatus(status);
        return rentalMapper.toDTOList(rentals);
    }

    /**
     * Obtiene rentas activas
     */
    @Transactional(readOnly = true)
    public List<RentalDTO> findActiveRentals() {
        List<Rental> rentals = rentalRepository.findActiveRentals();
        return rentalMapper.toDTOList(rentals);
    }

    /**
     * Obtiene rentas pendientes
     */
    @Transactional(readOnly = true)
    public List<RentalDTO> findPendingRentals() {
        List<Rental> rentals = rentalRepository.findPendingRentals();
        return rentalMapper.toDTOList(rentals);
    }

    /**
     * Obtiene rentas por cliente
     */
    @Transactional(readOnly = true)
    public List<RentalDTO> findByCustomerId(Long customerId) {
        List<Rental> rentals = rentalRepository.findByCustomerId(customerId);
        return rentalMapper.toDTOList(rentals);
    }

    /**
     * Obtiene rentas por vehículo
     */
    @Transactional(readOnly = true)
    public List<RentalDTO> findByVehicleId(Long vehicleId) {
        List<Rental> rentals = rentalRepository.findByVehicleId(vehicleId);
        return rentalMapper.toDTOList(rentals);
    }

    /**
     * Obtiene rentas con saldo pendiente
     */
    @Transactional(readOnly = true)
    public List<RentalDTO> findWithPendingBalance() {
        List<Rental> rentals = rentalRepository.findWithPendingBalance();
        return rentalMapper.toDTOList(rentals);
    }

    /**
     * Obtiene rentas atrasadas
     */
    @Transactional(readOnly = true)
    public List<RentalDTO> findOverdueRentals() {
        List<Rental> rentals = rentalRepository.findOverdueRentals(LocalDate.now());
        return rentalMapper.toDTOList(rentals);
    }

    /**
     * Cuenta rentas por estado
     */
    @Transactional(readOnly = true)
    public long countByStatus(RentalStatus status) {
        return rentalRepository.countByStatus(status);
    }

    /**
     * Cuenta rentas activas
     */
    @Transactional(readOnly = true)
    public long countActiveRentals() {
        return rentalRepository.countByStatus(RentalStatus.ACTIVE);
    }

    /**
     * Elimina una renta (soft delete)
     */
    public void deleteRental(Long rentalId) {
        log.info("Eliminando renta {}", rentalId);

        Rental rental = rentalRepository.findById(rentalId)
                .orElseThrow(() -> new IllegalArgumentException("Renta no encontrada"));

        rental.delete(); // Lógica de dominio
        rentalRepository.save(rental);
    }

    /**
     * Genera un número de contrato único
     * Formato: RENT-YYYYMMDD-XXXXX
     */
    private String generateContractNumber() {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "RENT-" + date + "-";

        // Buscar el último número del día
        int sequence = 1;
        String contractNumber;

        do {
            contractNumber = prefix + String.format("%05d", sequence);
            sequence++;
        } while (rentalRepository.existsByContractNumber(contractNumber));

        return contractNumber;
    }

    // ========================================
    // Métodos con Paginación (para Lazy Loading en UI)
    // ========================================

    /**
     * Obtiene rentas paginadas (todas)
     */
    @Transactional(readOnly = true)
    public Page<RentalDTO> findAllPaged(Pageable pageable) {
        Page<Rental> page = rentalRepository.findAllActivePaged(pageable);
        return page.map(rentalMapper::toDTO);
    }

    /**
     * Obtiene rentas paginadas por estado
     */
    @Transactional(readOnly = true)
    public Page<RentalDTO> findByStatusPaged(RentalStatus status, Pageable pageable) {
        Page<Rental> page = rentalRepository.findByStatusPaged(status, pageable);
        return page.map(rentalMapper::toDTO);
    }

    /**
     * Busca rentas paginadas por término de búsqueda
     */
    @Transactional(readOnly = true)
    public Page<RentalDTO> searchRentals(String searchTerm, Pageable pageable) {
        if (searchTerm == null || searchTerm.isBlank()) {
            return findAllPaged(pageable);
        }
        Page<Rental> page = rentalRepository.searchRentals(searchTerm, pageable);
        return page.map(rentalMapper::toDTO);
    }

    /**
     * Busca rentas paginadas por estado y término de búsqueda
     */
    @Transactional(readOnly = true)
    public Page<RentalDTO> searchRentalsByStatus(RentalStatus status, String searchTerm, Pageable pageable) {
        if (searchTerm == null || searchTerm.isBlank()) {
            return findByStatusPaged(status, pageable);
        }
        Page<Rental> page = rentalRepository.searchRentalsByStatus(status, searchTerm, pageable);
        return page.map(rentalMapper::toDTO);
    }

    /**
     * Cuenta total de rentas activas
     */
    @Transactional(readOnly = true)
    public long countAllActive() {
        return rentalRepository.countAllActive();
    }

    /**
     * Obtiene todas las rentas como eventos de calendario
     * Mapea las rentas a eventos con colores según su estado
     */
    @Transactional(readOnly = true)
    public List<com.rentacaresv.calendar.application.CalendarEventDTO> findAllAsCalendarEvents() {
        List<Rental> rentals = rentalRepository.findAllActive();

        return rentals.stream()
                .map(this::mapToCalendarEvent)
                .toList();
    }

    /**
     * Mapea una renta a un evento de calendario
     */
    private com.rentacaresv.calendar.application.CalendarEventDTO mapToCalendarEvent(Rental rental) {
        // Determinar color según estado
        String color = switch (rental.getStatus()) {
            case PENDING -> "#FFC107"; // Amarillo
            case ACTIVE -> "#4CAF50"; // Verde
            case COMPLETED -> "#2196F3"; // Azul
            case CANCELLED -> "#F44336"; // Rojo
        };

        // Construir título del evento
        String title = String.format("%s - %s %s",
                rental.getCustomer().getFullName(),
                rental.getVehicle().getBrand(),
                rental.getVehicle().getModel());

        // Información del vehículo
        String vehicleInfo = String.format("%s %s (%s)",
                rental.getVehicle().getBrand(),
                rental.getVehicle().getModel(),
                rental.getVehicle().getLicensePlate());

        return new com.rentacaresv.calendar.application.CalendarEventDTO(
                rental.getId(),
                title,
                rental.getStartDate(),
                rental.getEndDate(),
                color,
                rental.getStatus().name(),
                rental.getCustomer().getFullName(),
                vehicleInfo,
                rental.getContractNumber());
    }
}
