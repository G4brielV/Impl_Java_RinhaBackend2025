package com.rinha2025.__Imp_rinha2025.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rinha2025.__Imp_rinha2025.model.dto.*;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.context.annotation.Configuration;

// Esta classe informa ao GraalVM como lidar com a projeção dinâmica.
@Configuration
public class NativeHints implements RuntimeHintsRegistrar {

    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
        // Dica para a Projeção JPA (problema do health check)
        //hints.proxies().registerJdkProxy(PaymentSummaryProjection.class);
        //hints.reflection().registerType(PaymentSummaryProjection.class, MemberCategory.INVOKE_PUBLIC_METHODS);

        // Dicas para os DTOs (problema do JSON "No HttpMessageConverter")
        // Registra todos os DTOs para reflection, permitindo que o Jackson os serialize.
        var memberCategories = new MemberCategory[]{ MemberCategory.INVOKE_DECLARED_CONSTRUCTORS, MemberCategory.INVOKE_PUBLIC_METHODS};
        hints.reflection().registerType(PaymentRequestDTO.class, memberCategories);
        hints.reflection().registerType(PaymentResultsDTO.class, memberCategories);
        hints.reflection().registerType(PaymentSummaryResponseDTO.class, memberCategories);
        hints.reflection().registerType(ServiceHealthDTO.class, memberCategories);
        hints.reflection().registerType(PaymentDTO.class, memberCategories);
        hints.reflection().registerType(ObjectMapper.class, memberCategories);
        hints.reflection().registerType(com.fasterxml.jackson.datatype.jsr310.JavaTimeModule.class, memberCategories);
    }
}