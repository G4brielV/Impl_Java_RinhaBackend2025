package com.rinha2025.__Imp_rinha2025.service.impl;

import com.rinha2025.__Imp_rinha2025.entity.PaymentEntity;
import com.rinha2025.__Imp_rinha2025.service.PaymentProcessorJob;
import com.rinha2025.__Imp_rinha2025.service.PaymentSenderService;
import com.rinha2025.__Imp_rinha2025.service.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger logger = LoggerFactory.getLogger(PaymentProcessorJobImpl.class);

    private final PaymentService paymentService;
    private final PaymentSenderService paymentSenderService;
    private final ExecutorService paymentExecutor;


    public PaymentProcessorJobImpl(PaymentService paymentService,
                                   PaymentSenderService paymentSenderService,
                                   ExecutorService paymentExecutor) { // Modificar construtor
        this.paymentService = paymentService;
        this.paymentSenderService = paymentSenderService;
        this.paymentExecutor = paymentExecutor;
    }

    @Scheduled(fixedDelay = 15)
    @Override
    public void processPayment() {
        // Drena até 100 pagamentos da fila para uma lsita local
        List<String> paymentsToProcess = new ArrayList<>();
        paymentService.drainQueue(paymentsToProcess, 100);

        if (paymentsToProcess.isEmpty()) {
            return;
        }

        // Fila para coletar pagamentos processados com sucesso
        ConcurrentLinkedQueue<PaymentEntity> processedSuccessfully = new ConcurrentLinkedQueue<>();

        List<CompletableFuture<Void>> futures = paymentsToProcess.stream()
                .map(paymentJson -> CompletableFuture.runAsync(() -> {
                    PaymentEntity successfulEntity = processSinglePayment(paymentJson);
                    if (successfulEntity != null) {
                        processedSuccessfully.add(successfulEntity);
                    }
                }, paymentExecutor))
                .toList();

        // Espera que todas as tarefas do lote terminem
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        // Salva todos os pagamentos bem-sucedidos de uma vez
        if (!processedSuccessfully.isEmpty()) {
            paymentService.saveAll(new ArrayList<>(processedSuccessfully));
            logger.info("Lote de {} pagamentos salvo no banco de dados.", processedSuccessfully.size());
        }
    }

    private PaymentEntity  processSinglePayment(String paymentJson) {
        String correlationId = paymentJson.substring(18, 54);
        try {
            logger.info("Processando pagamento com correlationId: {}", correlationId);

            // Tenta o processador padrão
            boolean success = paymentSenderService.send(paymentJson, defaultPaymentProcessorUrl);
            if (success) {
                logger.info("Pagamento {} enviado para o processador DEFAULT com sucesso.", correlationId);
                PaymentEntity entity = PaymentEntity.fromJson(paymentJson);
                entity.setDefault(true);
                return entity;
            }

            // Se o padrão falhar, loga e tenta o fallback
            logger.warn("Falha ao enviar para o processador DEFAULT. Tentando FALLBACK para o pagamento {}.", correlationId);
            success = paymentSenderService.send(paymentJson, fallbackPaymentProcessorUrl);

            if (success) {
                logger.info("Pagamento {} enviado para o processador FALLBACK com sucesso.", correlationId);
                PaymentEntity entity = PaymentEntity.fromJson(paymentJson);
                entity.setDefault(false);
                return entity;
            }

            // Se ambos falharem, recoloca o item na fila
            logger.error("Ambos os processadores falharam para o pagamento {}. Devolvendo para a fila.", correlationId);
            paymentService.enqueuePayment(paymentJson);
            return null; //Falha

        } catch (Exception e) {
            logger.error("Erro inesperado ao processar pagamento {}. Devolvendo para a fila.", correlationId, e);
            paymentService.enqueuePayment(paymentJson);
            return null;
        }
    }
}
