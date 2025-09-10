package com.rinha2025.__Imp_rinha2025.config;

public final class RedisKeys {
    private RedisKeys() {} // Impede a instanciação

    public static final String HEALTH_CHECK_LOCK = "health_check:lock";
    public static final String PREFERRED_PROCESSOR = "health_check:preferred_processor";
    public static final String PAYMENTS_SORTED_SET = "payments";
}
