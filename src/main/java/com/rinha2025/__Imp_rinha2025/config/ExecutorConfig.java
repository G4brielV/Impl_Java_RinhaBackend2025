package com.rinha2025.__Imp_rinha2025.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class ExecutorConfig {

    @Bean
    public ExecutorService paymentExecutor() {
        // Cria um ExecutorService que usa uma nova thread virtual para cada tarefa
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}
