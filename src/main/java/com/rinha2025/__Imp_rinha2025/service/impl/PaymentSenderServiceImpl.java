package com.rinha2025.__Imp_rinha2025.service.impl;


import com.rinha2025.__Imp_rinha2025.service.PaymentSenderService;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class PaymentSenderServiceImpl implements PaymentSenderService {

    private final WebClient webClient;

    public PaymentSenderServiceImpl(WebClient webClient) {
        this.webClient = webClient;
    }

    @Override
    public boolean send(String requestBody, String url) {
        try {
            Integer statusCode = webClient.post()
                    .uri(url)
                    .bodyValue(requestBody)
                    .retrieve()
                    .toBodilessEntity()
                    .flatMap(response -> Mono.just(response.getStatusCode().value()))
                    .block();
            return statusCode != null && statusCode == 200;
        } catch (Exception e) {
            return false;
        }
    }
}
