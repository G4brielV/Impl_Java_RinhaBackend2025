package com.rinha2025.__Imp_rinha2025.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
public class HttpClientConfig {

    @Bean
    public HttpClient httpClient() {
        return HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1) // Usa HTTP/1.1 para compatibilidade
                .connectTimeout(Duration.ofSeconds(2)) // Timeout para estabelecer a conexão
                .followRedirects(HttpClient.Redirect.NEVER) // Não seguir redirecionamentos
                .build();
    }
}
