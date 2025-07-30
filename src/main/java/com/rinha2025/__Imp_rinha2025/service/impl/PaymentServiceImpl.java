package com.rinha2025.__Imp_rinha2025.service.impl;

import com.rinha2025.__Imp_rinha2025.entity.PaymentEntity;
import com.rinha2025.__Imp_rinha2025.model.dto.PaymentRequestDTO;
import com.rinha2025.__Imp_rinha2025.model.dto.PaymentResultsDTO;
import com.rinha2025.__Imp_rinha2025.model.dto.PaymentSummaryResponseDTO;
import com.rinha2025.__Imp_rinha2025.model.projection.PaymentSummaryProjection;
import com.rinha2025.__Imp_rinha2025.repository.PaymentRepository;
import com.rinha2025.__Imp_rinha2025.service.PaymentService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Service
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;

    private final BlockingQueue<PaymentEntity> queue = new LinkedBlockingQueue<>();

    public PaymentServiceImpl(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @Override
    public void processPayment(PaymentRequestDTO requestDTO) {
        // TODO: Convert the requestDTO to PaymentEntity and enqueue it for processing
        PaymentEntity paymentEntity = new PaymentEntity(
                requestDTO.correlationId(),
                requestDTO.amount(),
                LocalDateTime.now(),
                true);
        enqueuePayment(paymentEntity);
    }

    private void enqueuePayment(PaymentEntity requestEntity) {
        queue.offer(requestEntity);
    }

    @Override
    public PaymentSummaryResponseDTO getPaymentSummary(LocalDateTime from, LocalDateTime to) {
        List<PaymentSummaryProjection> summaryList = paymentRepository.findSummaryByDateRange(from, to);

        PaymentResultsDTO defaultResults = new PaymentResultsDTO(0, 0.0);
        PaymentResultsDTO fallbackResults = new PaymentResultsDTO(0, 0.0);

        for (PaymentSummaryProjection summary : summaryList) {
            if (Boolean.TRUE.equals(summary.isDefault())) {
                defaultResults = new PaymentResultsDTO(summary.getTotalRequests(), summary.getTotalAmount());
            } else {
                fallbackResults = new PaymentResultsDTO(summary.getTotalRequests(), summary.getTotalAmount());
            }
        }
        return new PaymentSummaryResponseDTO(defaultResults, fallbackResults);
    }
}
