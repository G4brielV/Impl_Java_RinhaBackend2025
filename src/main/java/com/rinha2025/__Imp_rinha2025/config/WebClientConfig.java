package com.rinha2025.__Imp_rinha2025.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;

@Configuration
public class WebClientConfig {

    // Valores inspirados nos repos de alta performance. Podem ser externalizados para o application.properties.
    int connectionTimeout = 2000;
    int readTimeout = 1500;
    int writeTimeout = 1500;
    int maxConnections = 1000;
    int maxIdleTime = 60000;
    int maxLifeTime = 60000;

    @Bean
    public WebClient webClient() {
        ConnectionProvider provider = ConnectionProvider.builder("rinha-connection-pool")
                .maxConnections(maxConnections)
                .pendingAcquireTimeout(Duration.ofMillis(0)) // Sem timeout para adquirir conexão
                .pendingAcquireMaxCount(-1) // Fila infinita para adquirir conexão
                .maxIdleTime(Duration.ofMillis(maxIdleTime))
                .maxLifeTime(Duration.ofMillis(maxLifeTime))
                .build();

        HttpClient httpClient = HttpClient.create(provider)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectionTimeout)
                .responseTimeout(Duration.ofMillis(readTimeout))
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(readTimeout / 1000))
                        .addHandlerLast(new WriteTimeoutHandler(writeTimeout / 1000)));

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
}