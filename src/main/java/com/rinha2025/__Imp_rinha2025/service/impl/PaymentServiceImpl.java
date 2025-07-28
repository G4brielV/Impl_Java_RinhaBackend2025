package com.rinha2025.__Imp_rinha2025.service.impl;

import com.rinha2025.__Imp_rinha2025.model.dto.PaymentRequestDTO;
import com.rinha2025.__Imp_rinha2025.model.dto.PaymentResponseDTO;
import com.rinha2025.__Imp_rinha2025.model.dto.PaymentSummaryDTO;
import com.rinha2025.__Imp_rinha2025.service.PaymentService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;

    public PaymentServiceImpl(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @Override
    public PaymentResponseDTO processPayment(PaymentRequestDTO requestDTO) {

        return null;
    }

    @Override
    public PaymentSummaryDTO getPaymentSummary(LocalDateTime from, LocalDateTime to) {
        return null;
    }
}
