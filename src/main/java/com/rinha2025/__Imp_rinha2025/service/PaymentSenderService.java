package com.rinha2025.__Imp_rinha2025.service;

import com.rinha2025.__Imp_rinha2025.model.dto.PaymentProcessorRequestDTO;

public interface PaymentSenderService {

    boolean send(PaymentProcessorRequestDTO paymentProcessorRequestDTO, String URL);
}
