package com.rinha2025.__Imp_rinha2025.service.impl;

import com.rinha2025.__Imp_rinha2025.entity.PaymentEntity;
import com.rinha2025.__Imp_rinha2025.model.dto.PaymentProcessorRequestDTO;
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
        List<PaymentEntity> paymentsToProcess = new ArrayList<>();
        paymentService.drainQueue(paymentsToProcess, 100);

        if (paymentsToProcess.isEmpty()) {
            return;
        }

        // Fila para coletar pagamentos processados com sucesso
        ConcurrentLinkedQueue<PaymentEntity> processedSuccessfully = new ConcurrentLinkedQueue<>();

        List<CompletableFuture<Void>> futures = paymentsToProcess.stream()
                .map(payment -> CompletableFuture.runAsync(() -> {
                    if (processSinglePayment(payment)) {
                        processedSuccessfully.add(payment);
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

    private boolean processSinglePayment(PaymentEntity payment) {
        try {
            logger.info("Processando pagamento com correlationId: {}", payment.getCorrelationId());

            PaymentProcessorRequestDTO requestDTO = new PaymentProcessorRequestDTO(
                    payment.getCorrelationId(),
                    payment.getAmount(),
                    payment.getCreatedAt().toString());

            // Tenta o processador padrão
            boolean success = paymentSenderService.send(requestDTO, defaultPaymentProcessorUrl);

            if (success) {
                logger.info("Pagamento {} enviado para o processador DEFAULT com sucesso.", payment.getCorrelationId());
                payment.setDefault(true);
                return true; // Sucesso, encerra o processamento para este pagamento
            }

            // Se o padrão falhar, loga e tenta o fallback
            logger.warn("Falha ao enviar para o processador DEFAULT. Tentando FALLBACK para o pagamento {}.", payment.getCorrelationId());
            success = paymentSenderService.send(requestDTO, fallbackPaymentProcessorUrl);

            if (success) {
                logger.info("Pagamento {} enviado para o processador FALLBACK com sucesso.", payment.getCorrelationId());
                payment.setDefault(false);
                return true; // Sucesso, encerra o processamento
            }

            // Se ambos falharem, recoloca o item na fila
            logger.error("Ambos os processadores falharam para o pagamento {}. Devolvendo para a fila.", payment.getCorrelationId());
            paymentService.enqueuePayment(payment);
            return false;

        } catch (Exception e) {
            logger.error("Erro inesperado ao processar pagamento {}. Devolvendo para a fila.", payment.getCorrelationId(), e);
            paymentService.enqueuePayment(payment);
            return false;
        }
    }
}
