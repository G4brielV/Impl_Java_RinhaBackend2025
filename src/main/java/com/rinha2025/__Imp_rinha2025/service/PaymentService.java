package com.rinha2025.__Imp_rinha2025.service;


import com.rinha2025.__Imp_rinha2025.entity.PaymentEntity;
import com.rinha2025.__Imp_rinha2025.model.dto.PaymentRequestDTO;
import com.rinha2025.__Imp_rinha2025.model.dto.PaymentSummaryResponseDTO;

import java.time.LocalDateTime;
import java.util.List;

public interface PaymentService {

    void processPayment(PaymentRequestDTO paymentRequestDTO);

    PaymentSummaryResponseDTO getPaymentSummary(LocalDateTime from, LocalDateTime to);

    void enqueuePayment(PaymentEntity paymentEntity);

    PaymentEntity dequeuePayment();

    void save(PaymentEntity paymentEntity);

    void saveAll(List<PaymentEntity> payments);

    void drainQueue(List<PaymentEntity> collection, int maxElements);
}
