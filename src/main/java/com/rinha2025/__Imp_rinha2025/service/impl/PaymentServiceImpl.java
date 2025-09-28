package com.rinha2025.__Imp_rinha2025.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rinha2025.__Imp_rinha2025.model.dto.PaymentDTO;
import com.rinha2025.__Imp_rinha2025.model.dto.PaymentRequestDTO;
import com.rinha2025.__Imp_rinha2025.model.dto.PaymentResultsDTO;
import com.rinha2025.__Imp_rinha2025.model.dto.PaymentSummaryResponseDTO;
import com.rinha2025.__Imp_rinha2025.service.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.regex.Pattern;


@Service
public class PaymentServiceImpl implements PaymentService {

    private final Logger logger = LoggerFactory.getLogger(PaymentServiceImpl.class);
    private final StringRedisTemplate redisTemplate;
    private final BlockingQueue<PaymentDTO> queue = new LinkedBlockingQueue<>();
    private double lastKnownAmount = 0.0;
    private static final Pattern AMOUNT_PATTERN = Pattern.compile("\"amount\":(\\d+\\.?\\d*)");


    public PaymentServiceImpl(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void processPayment(PaymentRequestDTO requestDTO) {
        this.lastKnownAmount = requestDTO.amount();
        PaymentDTO paymentDTO = new PaymentDTO(requestDTO.correlationId(), requestDTO.amount(), Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MILLIS), null, 0);
        enqueuePayment(paymentDTO);
    }

    @Override
    public void enqueuePayment(PaymentDTO paymentDto) {
        queue.offer(paymentDto);
    }

    @Override
    public PaymentDTO dequeuePayment() throws InterruptedException {
        // poll() retorna null imediatamente se a fila estiver vazia,
        // ao invés de esperar como o take().
        return queue.take();
    }

    @Override
    public void save(PaymentDTO payment) {
        // O "score" será o timestamp em milissegundos
        try {
            double score = payment.requestedAt().toEpochMilli();
            // O "valor" será apenas o ID
            String value = payment.correlationId();
            // Usamos dois Sorted Sets, um para cada processador
            String key = Boolean.TRUE.equals(payment.isDefault()) ? "payments:default" : "payments:fallback";
            redisTemplate.opsForZSet().add(key, value, score);
        } catch (Exception e){
            logger.error("Falha ao serializar pagamento para o Redis: {}", payment.correlationId(), e);
        }
    }

    @Override
    public PaymentSummaryResponseDTO getPaymentSummary(Instant from, Instant to) {
        double fromTimestamp = from.toEpochMilli();
        double toTimestamp = to.toEpochMilli();

        // Zcount para contar os registros no intervalo de tempo dos scores
        Long defaultCount = redisTemplate.opsForZSet().count("payments:default", fromTimestamp, toTimestamp);
        Long fallbackCount = redisTemplate.opsForZSet().count("payments:fallback", fromTimestamp, toTimestamp);

        PaymentResultsDTO defaultResults =new PaymentResultsDTO(defaultCount, defaultCount * this.lastKnownAmount);
        PaymentResultsDTO fallbackResults =new PaymentResultsDTO(fallbackCount, fallbackCount * this.lastKnownAmount);

        return new PaymentSummaryResponseDTO(defaultResults, fallbackResults);
    }
}
