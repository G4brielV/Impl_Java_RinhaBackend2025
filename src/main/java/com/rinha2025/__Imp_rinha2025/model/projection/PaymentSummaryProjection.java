package com.rinha2025.__Imp_rinha2025.model.projection;

public interface PaymentSummaryProjection {
    Boolean isDefault();
    long getTotalRequests();
    double getTotalAmount();
}
