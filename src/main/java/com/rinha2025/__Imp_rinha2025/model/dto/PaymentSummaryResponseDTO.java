package com.rinha2025.__Imp_rinha2025.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PaymentSummaryResponseDTO(
        @JsonProperty("default")
        PaymentResultsDTO defaultResults,
        @JsonProperty("fallback")
        PaymentResultsDTO fallbackResults
) { }
