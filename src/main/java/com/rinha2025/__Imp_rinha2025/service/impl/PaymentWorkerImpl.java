package com.rinha2025.__Imp_rinha2025.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rinha2025.__Imp_rinha2025.model.Component.ProcessorHealthState;
import com.rinha2025.__Imp_rinha2025.model.dto.PaymentDTO;
import com.rinha2025.__Imp_rinha2025.service.PaymentWorker;
import com.rinha2025.__Imp_rinha2025.service.PaymentSenderService;
import com.rinha2025.__Imp_rinha2025.service.PaymentService;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
public class PaymentWorkerImpl implements PaymentWorker {

    private static final Logger logger = LoggerFactory.getLogger(PaymentWorkerImpl.class);


    @Value("${payment.processor.default.url}")
    private String defaultPaymentProcessorUrl;
    @Value("${payment.processor.fallback.url}")
    private String fallbackPaymentProcessorUrl;

    private final PaymentService paymentService;
    private final PaymentSenderService paymentSenderService;
    private final ExecutorService paymentExecutor;
    private final ProcessorHealthState healthState;
    private final ObjectMapper objectMapper;
    private final ScheduledExecutorService retryScheduler;

    private static final int MAX_PARALLELISM = 6;
    private static final int MAX_ATTEMPTS = 5;

    public PaymentWorkerImpl(PaymentService paymentService,
                             PaymentSenderService paymentSenderService,
                             ExecutorService paymentExecutor,
                             ProcessorHealthState healthState,
                             ObjectMapper objectMapper,
                             ScheduledExecutorService retryScheduler) {
        this.paymentService = paymentService;
        this.paymentSenderService = paymentSenderService;
        this.paymentExecutor = paymentExecutor;
        this.healthState = healthState;
        this.objectMapper = objectMapper;
        this.retryScheduler = retryScheduler;
    }

    @PostConstruct
    public void startWorkers() {
        // Inicia N workers contínuos em threads virtuais
        for (int i = 0; i < MAX_PARALLELISM; i++) {
            paymentExecutor.execute(this::runWorkerLoop);
        }
    }

    private void runWorkerLoop() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                PaymentDTO payment = paymentService.dequeuePayment();
                processSinglePayment(payment);
            } catch (InterruptedException e) {
                logger.warn("Payment worker interrupted.");
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                logger.error("Unexpected error in worker loop", e);
            }
        }
    }

    private void processSinglePayment(PaymentDTO payment) {
        ProcessorHealthState.Processor preferred = healthState.getPreferredProcessor();
        String targetUrl = (preferred == ProcessorHealthState.Processor.DEFAULT) ? defaultPaymentProcessorUrl : fallbackPaymentProcessorUrl;
        boolean isDefaultProcessor = (preferred == ProcessorHealthState.Processor.DEFAULT);

        String paymentJson;
        try {
            paymentJson = objectMapper.writeValueAsString(payment);
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize payment {}. Re-enqueuing.", payment.correlationId(), e);
            handleFailure(payment); // Trata a falha de serialização como qualquer outra falha
            return;
        }

        boolean success = paymentSenderService.send(paymentJson, targetUrl);

        if (success) {
            // SUCESSO: Salva individualmente e o ciclo termina aqui.
            PaymentDTO successfulPayment = new PaymentDTO(payment.correlationId(), payment.amount(), payment.requestedAt(), isDefaultProcessor, payment.attempts());
            paymentService.save(successfulPayment);
        } else {
            // FALHA: Inicia a lógica de retentativa com backoff.
            handleFailure(payment);
        }
    }


    private void handleFailure(PaymentDTO payment) {
        int nextAttempt = payment.attempts() + 1;

        if (nextAttempt >= MAX_ATTEMPTS) {
            logger.error("[PERMANENT FAILURE] Payment {} failed after {} attempts.", payment.correlationId(), nextAttempt);
            return;
        }

        // Calcula o delay com backoff exponencial + um pequeno jitter (variação aleatória)
        long delayMs = (long) (500 * Math.pow(2, nextAttempt)) + (long) (Math.random() * 250);

        PaymentDTO paymentForRetry = new PaymentDTO(payment.correlationId(), payment.amount(), payment.requestedAt(), null, nextAttempt);

        // Agenda o re-enfileiramento para o futuro
        retryScheduler.schedule(() -> {
            paymentService.enqueuePayment(paymentForRetry);
        }, delayMs, TimeUnit.MILLISECONDS);
    }

}
