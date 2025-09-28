package com.rinha2025.__Imp_rinha2025.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PaymentDTO(
        String correlationId,
        Double amount,
        @JsonProperty("requestedAt")
        Instant requestedAt,
        Boolean isDefault,
        Integer attempts
) {}