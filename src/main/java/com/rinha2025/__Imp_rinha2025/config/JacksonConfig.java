package com.rinha2025.__Imp_rinha2025.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class JacksonConfig {

    @Bean
    @Primary // Define este como o ObjectMapper principal para toda a aplicação
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        // Adiciona suporte para tipos do Java 8, como Instant, LocalDate, etc.
        mapper.registerModule(new JavaTimeModule());
        // Datas NÃO sejam escritas como timestamps numéricos
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        // não inclui campos nulos
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        return mapper;
    }
}