package com.rentacaresv.payment.application;

import com.rentacaresv.payment.domain.Payment;
import com.rentacaresv.payment.domain.PaymentMethod;
import com.rentacaresv.payment.domain.PaymentStatus;
import com.rentacaresv.payment.infrastructure.PaymentMapper;
import com.rentacaresv.payment.infrastructure.PaymentRepository;
import com.rentacaresv.rental.domain.Rental;
import com.rentacaresv.rental.infrastructure.RentalRepository;
import com.rentacaresv.security.AuthenticatedUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Servicio de aplicación para Payment
 * Orquesta los casos de uso relacionados con pagos
 */
@Service
@Transactional
@Validated
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final RentalRepository rentalRepository;
    private final PaymentMapper paymentMapper;
    private final AuthenticatedUser authenticatedUser;

    /**
     * Registra un nuevo pago
     */
    public PaymentDTO registerPayment(@Valid RegisterPaymentCommand command) {
        log.info("Registrando pago de ${} para renta {}", 
            command.getAmount(), command.getRentalId());

        // 1. Obtener la renta
        Rental rental = rentalRepository.findById(command.getRentalId())
                .orElseThrow(() -> new IllegalArgumentException(
                    "Renta no encontrada con ID: " + command.getRentalId()
                ));

        // 2. Validar que la renta no esté cancelada
        if (rental.getStatus().name().equals("CANCELLED")) {
            throw new IllegalStateException(
                "No se pueden registrar pagos para rentas canceladas"
            );
        }

        // 3. Validar que el monto no exceda el saldo
        BigDecimal currentBalance = rental.getBalance();
        if (command.getAmount().compareTo(currentBalance) > 0) {
            throw new IllegalArgumentException(
                String.format("El monto ($%.2f) excede el saldo pendiente ($%.2f)", 
                    command.getAmount(), currentBalance)
            );
        }

        // 4. Generar número de pago
        String paymentNumber = generatePaymentNumber();

        // 5. Obtener usuario actual
        String currentUser = authenticatedUser.get()
                .map(user -> user.getUsername())
                .orElse("system");

        // 6. Crear entidad de dominio
        Payment payment = Payment.builder()
                .paymentNumber(paymentNumber)
                .rental(rental)
                .amount(command.getAmount())
                .paymentMethod(PaymentMethod.valueOf(command.getPaymentMethod().toUpperCase()))
                .status(PaymentStatus.COMPLETED)
                .paymentDate(LocalDateTime.now())
                .referenceNumber(command.getReferenceNumber())
                .cardLastDigits(command.getCardLastDigits())
                .notes(command.getNotes())
                .createdBy(currentUser)
                .build();

        // 7. Validar y persistir
        payment.validateAmount();
        payment = paymentRepository.save(payment);

        // 8. Actualizar saldo de la renta
        rental.registerPayment(command.getAmount());
        rentalRepository.save(rental);

        log.info("Pago registrado exitosamente: {} - ${}", paymentNumber, command.getAmount());
        log.info("Nuevo saldo de renta {}: ${}", rental.getContractNumber(), rental.getBalance());

        return paymentMapper.toDTO(payment);
    }

    /**
     * Obtiene un pago por ID
     */
    @Transactional(readOnly = true)
    public PaymentDTO findById(Long id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pago no encontrado con ID: " + id));
        return paymentMapper.toDTO(payment);
    }

    /**
     * Obtiene todos los pagos activos
     */
    @Transactional(readOnly = true)
    public List<PaymentDTO> findAll() {
        List<Payment> payments = paymentRepository.findAllActive();
        return paymentMapper.toDTOList(payments);
    }

    /**
     * Obtiene pagos por renta
     */
    @Transactional(readOnly = true)
    public List<PaymentDTO> findByRentalId(Long rentalId) {
        List<Payment> payments = paymentRepository.findByRentalId(rentalId);
        return paymentMapper.toDTOList(payments);
    }

    /**
     * Obtiene pagos por estado
     */
    @Transactional(readOnly = true)
    public List<PaymentDTO> findByStatus(PaymentStatus status) {
        List<Payment> payments = paymentRepository.findByStatus(status);
        return paymentMapper.toDTOList(payments);
    }

    /**
     * Obtiene pagos por método
     */
    @Transactional(readOnly = true)
    public List<PaymentDTO> findByPaymentMethod(PaymentMethod method) {
        List<Payment> payments = paymentRepository.findByPaymentMethod(method);
        return paymentMapper.toDTOList(payments);
    }

    /**
     * Obtiene pagos en un rango de fechas
     */
    @Transactional(readOnly = true)
    public List<PaymentDTO> findByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        List<Payment> payments = paymentRepository.findByDateRange(startDate, endDate);
        return paymentMapper.toDTOList(payments);
    }

    /**
     * Obtiene pagos pendientes
     */
    @Transactional(readOnly = true)
    public List<PaymentDTO> findPendingPayments() {
        List<Payment> payments = paymentRepository.findPendingPayments();
        return paymentMapper.toDTOList(payments);
    }

    /**
     * Calcula el total pagado para una renta
     */
    @Transactional(readOnly = true)
    public BigDecimal calculateTotalPaidForRental(Long rentalId) {
        return paymentRepository.calculateTotalPaidForRental(rentalId);
    }

    /**
     * Calcula total de ingresos en un período
     */
    @Transactional(readOnly = true)
    public BigDecimal calculateTotalIncome(LocalDateTime startDate, LocalDateTime endDate) {
        return paymentRepository.calculateTotalIncome(startDate, endDate);
    }

    /**
     * Reembolsa un pago
     */
    public void refundPayment(Long paymentId) {
        log.info("Reembolsando pago {}", paymentId);

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Pago no encontrado"));

        // Lógica de dominio
        payment.refund();
        paymentRepository.save(payment);

        // Actualizar saldo de la renta
        Rental rental = payment.getRental();
        rental.registerPayment(payment.getAmount().negate()); // Restar el monto
        rentalRepository.save(rental);

        log.info("Pago {} reembolsado exitosamente", payment.getPaymentNumber());
    }

    /**
     * Confirma un pago pendiente
     */
    public void confirmPayment(Long paymentId) {
        log.info("Confirmando pago {}", paymentId);

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Pago no encontrado"));

        payment.confirm(); // Lógica de dominio
        paymentRepository.save(payment);
    }

    /**
     * Rechaza un pago pendiente
     */
    public void rejectPayment(Long paymentId, String reason) {
        log.info("Rechazando pago {}: {}", paymentId, reason);

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Pago no encontrado"));

        payment.reject(reason); // Lógica de dominio
        paymentRepository.save(payment);
    }

    /**
     * Cuenta pagos por estado
     */
    @Transactional(readOnly = true)
    public long countByStatus(PaymentStatus status) {
        return paymentRepository.countByStatus(status);
    }

    /**
     * Elimina un pago (soft delete)
     */
    public void deletePayment(Long paymentId) {
        log.info("Eliminando pago {}", paymentId);

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Pago no encontrado"));

        payment.delete(); // Lógica de dominio
        paymentRepository.save(payment);
    }

    /**
     * Genera un número de pago único
     * Formato: PAY-YYYYMMDD-XXXXX
     */
    private String generatePaymentNumber() {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "PAY-" + date + "-";

        int sequence = 1;
        String paymentNumber;

        do {
            paymentNumber = prefix + String.format("%05d", sequence);
            sequence++;
        } while (paymentRepository.existsByPaymentNumber(paymentNumber));

        return paymentNumber;
    }
}
