package com.rinha2025.__Imp_rinha2025.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
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
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Service
public class PaymentServiceImpl implements PaymentService {

    private final Logger logger = LoggerFactory.getLogger(PaymentServiceImpl.class);
    private final StringRedisTemplate redisTemplate;
    private final BlockingQueue<PaymentDTO> queue = new LinkedBlockingQueue<>();
    private final ObjectMapper objectMapper;
    private static final Pattern AMOUNT_PATTERN = Pattern.compile("\"amount\":(\\d+\\.?\\d*)");


    public PaymentServiceImpl(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void processPayment(PaymentRequestDTO requestDTO) {
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
            // O "valor" será o próprio pagamento serializado em JSON
            String value = objectMapper.writeValueAsString(payment);
            // Usamos dois Sorted Sets, um para cada processador
            String key = Boolean.TRUE.equals(payment.isDefault()) ? "payments:default" : "payments:fallback";
            redisTemplate.opsForZSet().add(key, value, score);
        } catch (JsonProcessingException e){
            logger.error("Falha ao serializar pagamento para o Redis: {}", payment.correlationId(), e);
        }
    }

    @Override
    public PaymentSummaryResponseDTO getPaymentSummary(Instant from, Instant to) {
        double fromTimestamp = from.toEpochMilli();
        double toTimestamp = to.toEpochMilli();

        // Busca os pagamentos no intervalo de tempo para o 'default'
        Set<String> defaultPaymentsJson = redisTemplate.opsForZSet().rangeByScore("payments:default", fromTimestamp, toTimestamp);
        // Busca os pagamentos no intervalo de tempo para o 'fallback'
        Set<String> fallbackPaymentsJson = redisTemplate.opsForZSet().rangeByScore("payments:fallback", fromTimestamp, toTimestamp);

        PaymentResultsDTO defaultResults = calculateSummary(defaultPaymentsJson);
        PaymentResultsDTO fallbackResults = calculateSummary(fallbackPaymentsJson);

        return new PaymentSummaryResponseDTO(defaultResults, fallbackResults);
    }

    private PaymentResultsDTO calculateSummary(Set<String> paymentsJson) {
        if (paymentsJson == null || paymentsJson.isEmpty()) {
            return new PaymentResultsDTO(0, 0.0);
        }

        long totalRequests = paymentsJson.size();
        double totalAmount = 0.0;

        for (String json : paymentsJson) {
            totalAmount += extractAmountFromJson(json);
        }

        return new PaymentResultsDTO(totalRequests, totalAmount);
    }

    private double extractAmountFromJson(String json) {
        if (json == null) return 0.0;
        Matcher matcher = AMOUNT_PATTERN.matcher(json);
        if (matcher.find()) {
            try {
                return Double.parseDouble(matcher.group(1));
            } catch (NumberFormatException e) {
                return 0.0;
            }
        }
        return 0.0;
    }
}
