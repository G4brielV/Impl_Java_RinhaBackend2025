package com.rinha2025.__Imp_rinha2025.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rinha2025.__Imp_rinha2025.model.Component.ProcessorHealthState;
import com.rinha2025.__Imp_rinha2025.model.dto.PaymentDTO;
import com.rinha2025.__Imp_rinha2025.service.PaymentProcessorJob;
import com.rinha2025.__Imp_rinha2025.service.PaymentSenderService;
import com.rinha2025.__Imp_rinha2025.service.PaymentService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;

@Service
public class PaymentProcessorJobImpl implements PaymentProcessorJob {

    @Value("${payment.processor.default.url}")
    private String defaultPaymentProcessorUrl;
    @Value("${payment.processor.fallback.url}")
    private String fallbackPaymentProcessorUrl;

    private final PaymentService paymentService;
    private final PaymentSenderService paymentSenderService;
    private final ExecutorService paymentExecutor;
    private final ProcessorHealthState healthState;
    private final ObjectMapper objectMapper;

    public PaymentProcessorJobImpl(PaymentService paymentService,
                                   PaymentSenderService paymentSenderService,
                                   ExecutorService paymentExecutor,
                                   ProcessorHealthState healthState,
                                   ObjectMapper objectMapper) {
        this.paymentService = paymentService;
        this.paymentSenderService = paymentSenderService;
        this.paymentExecutor = paymentExecutor;
        this.healthState = healthState;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelay = 5)
    @Override
    public void processPayment() {
        // Drena até 100 pagamentos da fila para uma lista local
        List<PaymentDTO> paymentsToProcess = new ArrayList<>();
        paymentService.drainQueue(paymentsToProcess, 35);

        if (paymentsToProcess.isEmpty()) {
            return;
        }
        // Fila para coletar pagamentos processados com sucesso de forma thread-safe
        ConcurrentLinkedQueue<PaymentDTO> processedSuccessfully = new ConcurrentLinkedQueue<>();

        List<CompletableFuture<Void>> futures = paymentsToProcess.stream()
                .map(paymentDTO -> CompletableFuture.runAsync(() -> {
                    PaymentDTO successfulPayment = processSinglePayment(paymentDTO);
                    if (successfulPayment != null) {
                        processedSuccessfully.add(successfulPayment);
                    }
                }, paymentExecutor))
                .toList();

        // Espera que todas as tarefas do lote terminem
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        // Salva todos os pagamentos bem-sucedidos de uma vez usando pipelining
        if (!processedSuccessfully.isEmpty()) {
            paymentService.saveAll(new ArrayList<>(processedSuccessfully));
        }
    }

    private PaymentDTO processSinglePayment(PaymentDTO payment) {
        ProcessorHealthState.Processor preferred = healthState.getPreferredProcessor();
        String targetUrl;
        boolean isDefaultProcessor;

        if (preferred == ProcessorHealthState.Processor.DEFAULT) {
            targetUrl = defaultPaymentProcessorUrl;
            isDefaultProcessor = true;
        } else {
            targetUrl = fallbackPaymentProcessorUrl;
            isDefaultProcessor = false;
        }

        String paymentJson;

        try {
            // Converte para Json
            paymentJson = objectMapper.writeValueAsString(payment);

        } catch (JsonProcessingException e) {
            // Se falhar, re-enfileiramos.
            paymentService.enqueuePayment(payment);
            return null;
        }

        boolean success = paymentSenderService.send(paymentJson, targetUrl);

        if (success) {
            // Se teve sucesso, retorna um NOVO DTO com o campo 'isDefault' preenchido.
            return new PaymentDTO(payment.correlationId(), payment.amount(), payment.requestedAt(), isDefaultProcessor);
        }

        // Se falhar, re-enfileira o DTO original.
        paymentService.enqueuePayment(payment);
        return null;
    }
}
