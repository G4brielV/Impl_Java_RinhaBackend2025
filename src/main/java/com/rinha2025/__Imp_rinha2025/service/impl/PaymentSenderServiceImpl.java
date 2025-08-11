package com.rinha2025.__Imp_rinha2025.service.impl;


import com.rinha2025.__Imp_rinha2025.model.dto.PaymentProcessorRequestDTO;
import com.rinha2025.__Imp_rinha2025.service.PaymentSenderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;


@Service
public class PaymentSenderServiceImpl implements PaymentSenderService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentSenderServiceImpl.class);
    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public boolean send(PaymentProcessorRequestDTO paymentProcessorRequestDTO, String URL) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<PaymentProcessorRequestDTO> request = new HttpEntity<>(paymentProcessorRequestDTO, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(URL, request, String.class);

            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            logger.error("Erro ao enviar pagamento para a URL: {}", URL, e);
            return false;
        }
    }
}
