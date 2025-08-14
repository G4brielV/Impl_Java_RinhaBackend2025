package com.rinha2025.__Imp_rinha2025.service;


import com.rinha2025.__Imp_rinha2025.entity.PaymentEntity;
import com.rinha2025.__Imp_rinha2025.model.dto.PaymentRequestDTO;
import com.rinha2025.__Imp_rinha2025.model.dto.PaymentSummaryResponseDTO;

import java.time.Instant;
import java.util.List;

public interface PaymentService {

    void processPayment(PaymentRequestDTO paymentRequestDTO);

    PaymentSummaryResponseDTO getPaymentSummary(Instant from, Instant to);

    void enqueuePayment(String paymentJson);

    String dequeuePayment();

    void save(PaymentEntity paymentEntity);

    void saveAll(List<PaymentEntity> payments);

    void drainQueue(List<String> collection, int maxElements);
}
