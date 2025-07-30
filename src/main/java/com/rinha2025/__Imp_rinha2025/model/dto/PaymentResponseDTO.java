package com.rinha2025.__Imp_rinha2025.model.dto;

public record PaymentResponseDTO(
        boolean success,
        String correlationId,
        double amount
) {
}
