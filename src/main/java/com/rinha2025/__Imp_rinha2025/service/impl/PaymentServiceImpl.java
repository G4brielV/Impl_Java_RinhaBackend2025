package com.rinha2025.__Imp_rinha2025.service.impl;

import com.rinha2025.__Imp_rinha2025.entity.PaymentEntity;
import com.rinha2025.__Imp_rinha2025.model.dto.PaymentRequestDTO;
import com.rinha2025.__Imp_rinha2025.model.dto.PaymentResultsDTO;
import com.rinha2025.__Imp_rinha2025.model.dto.PaymentSummaryResponseDTO;
import com.rinha2025.__Imp_rinha2025.model.projection.PaymentSummaryProjection;
import com.rinha2025.__Imp_rinha2025.repository.PaymentRepository;
import com.rinha2025.__Imp_rinha2025.service.PaymentService;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Service
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;

    private final BlockingQueue<String> queue = new LinkedBlockingQueue<>();

    public PaymentServiceImpl(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @Override
    public void processPayment(PaymentRequestDTO requestDTO) {
        // TODO: Convert the requestDTO to JSON and enqueue it for processing
        String amountStr = String.format(Locale.US, "%.2f", requestDTO.amount());

        String paymentJson = "{\"correlationId\":\"" + requestDTO.correlationId() +
                "\",\"amount\":" + amountStr +
                ",\"requestedAt\":\"" + Instant.now().toString() + "\"}";
        enqueuePayment(paymentJson);
    }

    @Override
    public void enqueuePayment(String paymentJson) {
        queue.offer(paymentJson);
    }

    @Override
    public String dequeuePayment() {
        // poll() retorna null imediatamente se a fila estiver vazia,
        // ao invés de esperar como o take().
        return queue.poll();
    }

    @Override
    public void save(PaymentEntity paymentEntity) {
        paymentRepository.save(paymentEntity);
    }

    @Override
    public void saveAll(List<PaymentEntity> payments) {
        if (payments != null && !payments.isEmpty()) {
            paymentRepository.saveAll(payments);
        }
    }

    @Override
    public void drainQueue(List<String> collection, int maxElements) {
        queue.drainTo(collection, maxElements);
    }


    @Override
    public PaymentSummaryResponseDTO getPaymentSummary(Instant from, Instant to) {
        List<PaymentSummaryProjection> summaryList = paymentRepository.findSummaryByDateRange(from, to);

        PaymentResultsDTO defaultResults = new PaymentResultsDTO(0, 0.0);
        PaymentResultsDTO fallbackResults = new PaymentResultsDTO(0, 0.0);

        for (PaymentSummaryProjection summary : summaryList) {
            if (Boolean.TRUE.equals(summary.getIsDefault())) {
                defaultResults = new PaymentResultsDTO(summary.getTotalRequests(), summary.getTotalAmount());
            } else {
                fallbackResults = new PaymentResultsDTO(summary.getTotalRequests(), summary.getTotalAmount());
            }
        }
        return new PaymentSummaryResponseDTO(defaultResults, fallbackResults);
    }
}
