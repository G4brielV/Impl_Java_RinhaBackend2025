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
    private final ExecutorService paymentExecutor; // Injetar nosso executor

    public PaymentProcessorJobImpl(PaymentService paymentService,
                                   PaymentSenderService paymentSenderService,
                                   ExecutorService paymentExecutor) { // Modificar construtor
        this.paymentService = paymentService;
        this.paymentSenderService = paymentSenderService;
        this.paymentExecutor = paymentExecutor;
    }

    // Substitui o método antigo por um método agendado
    @Scheduled(fixedDelay = 15) // Executa a cada 15 milissegundos
    @Override
    public void processPayment() {
        // Pega até 20 pagamentos da fila de uma vez para processar em paralelo
        List<PaymentEntity> paymentsToProcess = new ArrayList<>();
        int batchSize = 20;
        for (int i = 0; i < batchSize; i++) {
            PaymentEntity payment = paymentService.dequeuePayment(); // Usaremos um dequeue não bloqueante
            if (payment == null) {
                break; // Fila vazia
            }
            paymentsToProcess.add(payment);
        }

        if (paymentsToProcess.isEmpty()) {
            return;
        }

        // Submete cada pagamento para ser processado em uma thread virtual
        List<CompletableFuture<Void>> futures = paymentsToProcess.stream()
                .map(payment -> CompletableFuture.runAsync(() -> processSinglePayment(payment), paymentExecutor))
                .toList();

        // Opcional: esperar a conclusão do lote. Para um sistema "fire-and-forget", isso não é estritamente necessário.
        // CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    }

    private void processSinglePayment(PaymentEntity payment) {
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
                paymentService.save(payment);
                return; // Sucesso, encerra o processamento para este pagamento
            }

            // Se o padrão falhar, loga e tenta o fallback
            logger.warn("Falha ao enviar para o processador DEFAULT. Tentando FALLBACK para o pagamento {}.", payment.getCorrelationId());
            success = paymentSenderService.send(requestDTO, fallbackPaymentProcessorUrl);

            if (success) {
                logger.info("Pagamento {} enviado para o processador FALLBACK com sucesso.", payment.getCorrelationId());
                payment.setDefault(false);
                paymentService.save(payment);
                return; // Sucesso, encerra o processamento
            }

            // Se ambos falharem, recoloca o item na fila
            logger.error("Ambos os processadores falharam para o pagamento {}. Devolvendo para a fila.", payment.getCorrelationId());
            paymentService.enqueuePayment(payment);

        } catch (Exception e) {
            logger.error("Erro inesperado ao processar pagamento {}. Devolvendo para a fila.", payment.getCorrelationId(), e);
            paymentService.enqueuePayment(payment);
        }
    }

}