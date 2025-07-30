package com.rinha2025.__Imp_rinha2025.service;


import com.rinha2025.__Imp_rinha2025.model.dto.PaymentRequestDTO;
import com.rinha2025.__Imp_rinha2025.model.dto.PaymentSummaryResponseDTO;

import java.time.LocalDateTime;

public interface PaymentService {

    void processPayment(PaymentRequestDTO paymentRequestDTO);

    PaymentSummaryResponseDTO getPaymentSummary(LocalDateTime from, LocalDateTime to);
}
