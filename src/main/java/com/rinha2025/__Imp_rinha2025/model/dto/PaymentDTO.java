package com.rinha2025.__Imp_rinha2025.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

public record PaymentDTO(
        String correlationId,
        Double amount,
        Instant requestedAt,
        Boolean isDefault
) {}