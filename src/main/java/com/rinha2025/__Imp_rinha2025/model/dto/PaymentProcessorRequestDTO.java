package com.rinha2025.__Imp_rinha2025.model.dto;

public record PaymentProcessorRequestDTO(
        String correlationId,
        double amount,
        String requestedAt
) {
}
