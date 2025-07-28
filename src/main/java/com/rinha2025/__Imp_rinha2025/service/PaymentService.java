package com.rinha2025.__Imp_rinha2025.service;


import com.rinha2025.__Imp_rinha2025.model.dto.PaymentRequestDTO;
import com.rinha2025.__Imp_rinha2025.model.dto.PaymentResponseDTO;
import com.rinha2025.__Imp_rinha2025.model.dto.PaymentSummaryDTO;

import java.time.LocalDateTime;

public interface PaymentService {

    PaymentResponseDTO processPayment(PaymentRequestDTO paymentRequestDTO);

    PaymentSummaryDTO getPaymentSummary(LocalDateTime from, LocalDateTime to);
}
