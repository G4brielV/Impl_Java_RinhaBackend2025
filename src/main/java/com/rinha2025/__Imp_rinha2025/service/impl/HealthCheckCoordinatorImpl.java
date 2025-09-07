// Crie um novo arquivo: HealthCheckCoordinator.java
package com.rinha2025.__Imp_rinha2025.service.impl;

import com.rinha2025.__Imp_rinha2025.model.Component.ProcessorHealthState;
import com.rinha2025.__Imp_rinha2025.service.HealthCheckCoordinator;
import com.rinha2025.__Imp_rinha2025.service.HealthCheckService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

@Service
public class HealthCheckCoordinatorImpl implements HealthCheckCoordinator {

    private static final Logger logger = LoggerFactory.getLogger(HealthCheckCoordinatorImpl.class);
    private final JdbcTemplate jdbcTemplate;
    private final HealthCheckService healthCheckService;
    private final ProcessorHealthState healthState;

    public HealthCheckCoordinatorImpl(JdbcTemplate jdbcTemplate, HealthCheckService healthCheckService, ProcessorHealthState healthState) {
        this.jdbcTemplate = jdbcTemplate;
        this.healthCheckService = healthCheckService;
        this.healthState = healthState;
    }

    @Scheduled(fixedDelay = 5000)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void coordinateHealthCheck() {
        try {
            String sql = "SELECT last_checked_at FROM processor_health_status WHERE lock_id = 1 FOR UPDATE SKIP LOCKED";
            OffsetDateTime lastChecked = jdbcTemplate.queryForObject(sql, OffsetDateTime.class);

            long secondsSinceLastCheck = ChronoUnit.SECONDS.between(lastChecked, OffsetDateTime.now(ZoneOffset.UTC));

            if (secondsSinceLastCheck >= 5) {
                // Lider
                String updateSql = "UPDATE processor_health_status SET last_checked_at = NOW() WHERE lock_id = 1";
                jdbcTemplate.update(updateSql);

                // Chama o trabalhador para fazer a tarefa pesada de forma assíncrona
                healthCheckService.performHealthCheckAsync();
            }
        } catch (Exception e) {
            // Nao sou o lider
            // AGORA, VOU LER O ESTADO ATUALIZADO PELO LÍDER (Cache).
            String currentPreferred = jdbcTemplate.queryForObject("SELECT preferred_processor FROM processor_health_status WHERE lock_id = 1", String.class);
            ProcessorHealthState.Processor processor = ProcessorHealthState.Processor.valueOf(currentPreferred);

            // SINCRONIZO MEU CACHE LOCAL
            healthState.setPreferredProcessor(processor);
            logger.trace("Could not acquire lock, another instance is likely leader.");
        }
    }
}