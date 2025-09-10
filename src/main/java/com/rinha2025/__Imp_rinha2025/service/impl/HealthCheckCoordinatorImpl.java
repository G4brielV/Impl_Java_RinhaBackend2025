// Crie um novo arquivo: HealthCheckCoordinator.java
package com.rinha2025.__Imp_rinha2025.service.impl;

import com.rinha2025.__Imp_rinha2025.config.RedisKeys;
import com.rinha2025.__Imp_rinha2025.model.Component.ProcessorHealthState;
import com.rinha2025.__Imp_rinha2025.service.HealthCheckCoordinator;
import com.rinha2025.__Imp_rinha2025.service.HealthCheckService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

@Service
public class HealthCheckCoordinatorImpl implements HealthCheckCoordinator {

    private static final Logger logger = LoggerFactory.getLogger(HealthCheckCoordinatorImpl.class);

    private final StringRedisTemplate redisTemplate;
    private final HealthCheckService healthCheckService;
    private final ProcessorHealthState healthState;

    public HealthCheckCoordinatorImpl(StringRedisTemplate redisTemplate, HealthCheckService healthCheckService, ProcessorHealthState healthState) {
        this.redisTemplate = redisTemplate;
        this.healthCheckService = healthCheckService;
        this.healthState = healthState;
    }

    @Scheduled(fixedDelay = 5000)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void coordinateHealthCheck() {
        try {
            // Tenta adquirir o lock: SETNX retorna true se a chave não existia (sou o líder)
            Boolean acquiredLock = redisTemplate.opsForValue().setIfAbsent(RedisKeys.HEALTH_CHECK_LOCK, "locked", Duration.ofSeconds(3));

            if (Boolean.TRUE.equals(acquiredLock)) { // Lider
                logger.info("Acquired HealthCheck lock. Performing check.");
                // Executor -> chamadas externas e atualizar o Redis
                healthCheckService.performHealthCheckAsync();
            } else { // Não lider
                String currentPreferred = redisTemplate.opsForValue().get(RedisKeys.PREFERRED_PROCESSOR);
                if (currentPreferred != null) {
                    ProcessorHealthState.Processor processor = ProcessorHealthState.Processor.valueOf(currentPreferred);
                    healthState.setPreferredProcessor(processor);
                    logger.trace("Synchronized local state with leader's decision: {}", processor);
                }
            }
        } catch (Exception e) {
            logger.error("Error in HealthCheckCoordinator: {}", e.getMessage(), e);
        }
    }
}