package com.rinha2025.__Imp_rinha2025.service.impl;

import com.rinha2025.__Imp_rinha2025.entity.PaymentEntity;
import com.rinha2025.__Imp_rinha2025.model.dto.PaymentProcessorRequestDTO;
import com.rinha2025.__Imp_rinha2025.service.PaymentProcessorJob;
import com.rinha2025.__Imp_rinha2025.service.PaymentSenderService;
import com.rinha2025.__Imp_rinha2025.service.PaymentService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class PaymentProcessorJobImpl implements PaymentProcessorJob {

    @Value("${payment.processor.default.url}")
    private String defaultPaymentProcessorUrl;

    @Value("${payment.processor.fallback.url}")
    private String fallbackPaymentProcessorUrl;

    private final PaymentService paymentService;
    private final PaymentSenderService paymentSenderService;
    private final PaymentProcessorJob self;

    public PaymentProcessorJobImpl(PaymentService paymentService, PaymentSenderService paymentSenderService, @Lazy PaymentProcessorJob self) {
        this.paymentService = paymentService;
        this.paymentSenderService = paymentSenderService;
        this.self = self;
    }

    @Async
    @Override
    public void processPayment() {
        //TODO: Dequeue and process the payments
        while (true) {
            PaymentEntity payment = paymentService.dequeuePayment();

            PaymentProcessorRequestDTO paymentProcessorRequestDTO = new PaymentProcessorRequestDTO(payment.getCorrelationId(), payment.getAmount(), payment.getCreatedAt().toString());
            if (paymentProcessorRequestDTO != null) {
                boolean sentDefault = false;
                boolean sentFallback = false;
                while (!sentDefault && !sentFallback) {
                    sentDefault = paymentSenderService.send(paymentProcessorRequestDTO, defaultPaymentProcessorUrl);
                    if (!sentDefault) {
                        sentFallback = paymentSenderService.send(paymentProcessorRequestDTO, fallbackPaymentProcessorUrl);
                        if (sentFallback) {
                            payment.setDefault(false);
                        }
                    }
                }
                paymentService.save(payment);
            } else {
                try {
                    Thread.sleep(100); // Evita busy-wait
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    @Override
    public void startJob() {
        processPayment();
    }
}
