package com.rinha2025.__Imp_rinha2025.service.impl;

import com.rinha2025.__Imp_rinha2025.entity.PaymentEntity;
import com.rinha2025.__Imp_rinha2025.model.dto.PaymentProcessorRequestDTO;
import com.rinha2025.__Imp_rinha2025.service.PaymentProcessorJob;
import com.rinha2025.__Imp_rinha2025.service.PaymentSenderService;
import com.rinha2025.__Imp_rinha2025.service.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger logger = LoggerFactory.getLogger(PaymentProcessorJobImpl.class);

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
        while (true) {
            try {
                // Isso vai esperar até que um pagamento esteja disponível
                PaymentEntity payment = paymentService.dequeuePayment();
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
                    continue; // Pula para o próximo item da fila
                }

                // Se o padrão falhar, loga e tenta o fallback
                logger.warn("Falha ao enviar para o processador DEFAULT. Tentando FALLBACK para o pagamento {}.", payment.getCorrelationId());
                success = paymentSenderService.send(requestDTO, fallbackPaymentProcessorUrl);

                if (success) {
                    logger.info("Pagamento {} enviado para o processador FALLBACK com sucesso.", payment.getCorrelationId());
                    payment.setDefault(false);
                    paymentService.save(payment);
                    continue; // Pula para o próximo item da fila
                }

                // Se ambos falharem, loga e recoloca o item na fila para tentar mais tarde
                logger.error("Ambos os processadores falharam para o pagamento {}. Devolvendo para a fila.", payment.getCorrelationId());
                // Adiciona uma pequena pausa para não sobrecarregar em caso de falha contínua
                Thread.sleep(500);
                paymentService.enqueuePayment(payment);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.error("Job de processamento de pagamento foi interrompido.", e);
                break; // Sai do loop while(true)
            } catch (Exception e) {
                // Pega qualquer outra exceção para evitar que o job morra
                logger.error("Erro inesperado no job de processamento de pagamento.", e);
            }
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    @Override
    public void startJob() {
        processPayment();
    }
}
