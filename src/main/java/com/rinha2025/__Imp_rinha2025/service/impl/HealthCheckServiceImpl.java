package com.rinha2025.__Imp_rinha2025.service.impl;

import com.rinha2025.__Imp_rinha2025.model.Component.ProcessorHealthState;
import com.rinha2025.__Imp_rinha2025.model.dto.ServiceHealthDTO;
import com.rinha2025.__Imp_rinha2025.service.HealthCheckService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;


@Service
public class HealthCheckServiceImpl implements HealthCheckService {

    private static final Logger logger = LoggerFactory.getLogger(HealthCheckServiceImpl.class);
    private final WebClient webClient;
    private final ProcessorHealthState healthState;
    private final JdbcTemplate jdbcTemplate;

    @Value("${payment.processor.default.healthcheck.url}")
    private String defaultHealthUrl ;
    @Value("${payment.processor.fallback.healthcheck.url}")
    private String fallbackHealthUrl;

    private static final double PERFORMANCE_MULTIPLIER = 4.5;

    public HealthCheckServiceImpl(@Qualifier("healthCheckWebClient") WebClient webClient, ProcessorHealthState healthState, JdbcTemplate jdbcTemplate) {
        this.webClient = webClient;
        this.healthState = healthState;
        this.jdbcTemplate = jdbcTemplate;
    }


    @Override
    @Async
    public void performHealthCheckAsync() {
        logger.info("Performing asynchronous health check...");
        Mono<ServiceHealthDTO> defaultHealthMono = getHealth(defaultHealthUrl);
        Mono<ServiceHealthDTO> fallbackHealthMono = getHealth(fallbackHealthUrl);

        Mono.zip(defaultHealthMono, fallbackHealthMono)
                .subscribe(tuple -> {
                    ProcessorHealthState.Processor newPreferred = decidePreferredProcessor(tuple.getT1(), tuple.getT2());

                    // ATUALIZA O ESTADO GLOBAL NO BANCO DE DADOS
                    String updateSql = "UPDATE processor_health_status SET preferred_processor = ? WHERE lock_id = 1";
                    jdbcTemplate.update(updateSql, newPreferred.name());

                    // Estado local (cache)
                    healthState.setPreferredProcessor(newPreferred);
                    logger.info("Health check complete. Preferred processor is now: {}", newPreferred);
                });
    }


    private ProcessorHealthState.Processor decidePreferredProcessor(ServiceHealthDTO defaultHealth, ServiceHealthDTO fallbackHealth) {
        // Ambos falhando: Default
        if (defaultHealth.failing() && fallbackHealth.failing()) {
            return ProcessorHealthState.Processor.DEFAULT;
        }
        // Default falhando: Fallback
        if (defaultHealth.failing()) {
            return ProcessorHealthState.Processor.FALLBACK;
        }
        // Fallback falhando: Default
        if (fallbackHealth.failing()) {
            return ProcessorHealthState.Processor.DEFAULT;
        }
        // Ambos funcionando: Compara a performance
        if (defaultHealth.minResponseTime() <= fallbackHealth.minResponseTime() * PERFORMANCE_MULTIPLIER) {
             return ProcessorHealthState.Processor.DEFAULT;
        } else {
            return ProcessorHealthState.Processor.FALLBACK;
        }
    }

    private Mono<ServiceHealthDTO> getHealth(String url) {
        return webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(ServiceHealthDTO.class)
                // Se a chamada falhar (timeout, erro 500), considera o serviço como "failing"
                .timeout(Duration.ofMillis(1000))
                .onErrorResume(ex -> {
                    logger.warn("Falha ao consultar health check de {}: {}", url, ex.getMessage());
                    // Retorna um DTO que representa a falha
                    return Mono.just(new ServiceHealthDTO(true, Double.MAX_VALUE));
                });
    }
}