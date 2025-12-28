package com.rentacaresv.payment.infrastructure;

import com.rentacaresv.payment.application.PaymentDTO;
import com.rentacaresv.payment.domain.Payment;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper para convertir entre Payment y PaymentDTO
 */
@Component
public class PaymentMapper {

    /**
     * Convierte una entidad Payment a DTO
     */
    public PaymentDTO toDTO(Payment payment) {
        if (payment == null) {
            return null;
        }

        return PaymentDTO.builder()
                .id(payment.getId())
                .paymentNumber(payment.getPaymentNumber())
                .rentalId(payment.getRental().getId())
                .rentalContractNumber(payment.getRental().getContractNumber())
                .amount(payment.getAmount())
                .paymentMethod(payment.getPaymentMethod().name())
                .paymentMethodLabel(payment.getPaymentMethod().getLabel())
                .status(payment.getStatus().name())
                .statusLabel(payment.getStatus().getLabel())
                .paymentDate(payment.getPaymentDate())
                .referenceNumber(payment.getReferenceNumber())
                .cardLastDigits(payment.getCardLastDigits())
                .notes(payment.getNotes())
                .createdBy(payment.getCreatedBy())
                .createdAt(payment.getCreatedAt())
                .isSuccessful(payment.isSuccessful())
                .canBeRefunded(payment.canBeRefunded())
                .description(payment.getDescription())
                .build();
    }

    /**
     * Convierte una lista de Payment a lista de DTO
     */
    public List<PaymentDTO> toDTOList(List<Payment> payments) {
        if (payments == null) {
            return List.of();
        }
        return payments.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
}
