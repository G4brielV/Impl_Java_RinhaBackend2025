package com.rinha2025.__Imp_rinha2025.service.impl;


import com.rinha2025.__Imp_rinha2025.service.PaymentSenderService;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;


@Service
public class PaymentSenderServiceImpl implements PaymentSenderService {

    private final HttpClient httpClient;

    public PaymentSenderServiceImpl(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @Override
    public boolean send(String requestBody, String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofMillis(1500))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();


            HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());

            return response.statusCode() == 200 || response.statusCode() == 422;
        } catch (Exception e) {
            return false;
        }
    }
}
