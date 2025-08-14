package com.rinha2025.__Imp_rinha2025.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Entity
@Table(name = "payments")
public class PaymentEntity {

    private static final Pattern JSON_PATTERN = Pattern.compile(
            "\\{\"correlationId\":\"(.*?)\",\"amount\":(.*?),\"requestedAt\":\"(.*?)\"(,\"isDefault\":(true|false))?\\}"
    );
    @Id
    private String correlationId;
    private Double amount;
    private Instant createdAt;
    private Boolean isDefault;

    public PaymentEntity() {
    }

    public PaymentEntity(String correlationId, Double amount, Instant createdAt, Boolean isDefault) {
        this.correlationId = correlationId;
        this.amount = amount;
        this.createdAt = createdAt;
        this.isDefault = isDefault;
    }

    public static PaymentEntity fromJson(String json) {
        Matcher matcher = JSON_PATTERN.matcher(json);
        if (matcher.find()) {
            String correlationId = matcher.group(1);
            double amount = Double.parseDouble(matcher.group(2));
            Instant createdAt = Instant.parse(matcher.group(3));
            return new PaymentEntity(correlationId, amount, createdAt, null);
        }
        throw new IllegalArgumentException("Invalid JSON format for PaymentEntity: " + json);
    }


    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Boolean getDefault() {
        return isDefault;
    }

    public void setDefault(Boolean aDefault) {
        isDefault = aDefault;
    }

}
