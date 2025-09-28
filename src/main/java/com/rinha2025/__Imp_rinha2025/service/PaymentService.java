package com.rinha2025.__Imp_rinha2025.service;


import com.rinha2025.__Imp_rinha2025.model.dto.PaymentDTO;
import com.rinha2025.__Imp_rinha2025.model.dto.PaymentRequestDTO;
import com.rinha2025.__Imp_rinha2025.model.dto.PaymentSummaryResponseDTO;

import java.time.Instant;
import java.util.List;

public interface PaymentService {

    void processPayment(PaymentRequestDTO paymentRequestDTO);

    PaymentSummaryResponseDTO getPaymentSummary(Instant from, Instant to);

    void enqueuePayment(PaymentDTO paymentDTO);

    PaymentDTO dequeuePayment() throws InterruptedException;

    void save(PaymentDTO paymentDTO);

}
