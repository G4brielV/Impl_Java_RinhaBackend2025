package com.rinha2025.__Imp_rinha2025.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Configuration
public class ExecutorConfig {

    @Bean
    public ExecutorService paymentExecutor() {
        // Cria um ExecutorService que usa uma nova thread virtual para cada tarefa
        return Executors.newVirtualThreadPerTaskExecutor();
    }

    @Bean
    public ScheduledExecutorService retryScheduler() {
        // Cria um agendador para as retentativas, usando threads virtuais
        return Executors.newSingleThreadScheduledExecutor(Thread.ofVirtual().factory());
    }
}
